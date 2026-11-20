package com.depositcock.admin;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.depositcock.admin.AdminDtos.AdminSellerRow;
import com.depositcock.admin.AdminDtos.AdminUpdateRequest;
import com.depositcock.admin.AdminDtos.SellerInfoRequest;
import com.depositcock.common.ApiException;
import com.depositcock.common.ErrorCode;
import com.depositcock.common.PhoneNumbers;
import com.depositcock.common.Texts;
import com.depositcock.groupbuy.GroupBuyRepository;
import com.depositcock.groupbuy.GroupBuyRepository.SellerCount;
import com.depositcock.seller.LoginIds;
import com.depositcock.seller.Role;
import com.depositcock.seller.Seller;
import com.depositcock.seller.SellerDtos.SignupRequest;
import com.depositcock.seller.SellerRepository;
import com.depositcock.seller.SellerService;
import com.depositcock.vision.CaptureUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 슈퍼 관리자(윤성)의 판매자 관리: 목록, 플랜·사용 제한 등 변경, 탈퇴 처리, 관리자 계정 만들기.
 * 판매자 본인 기능은 SellerService (2026-10-28 나눔).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final SellerRepository sellerRepository;
    private final SellerService sellerService;
    private final GroupBuyRepository groupBuyRepository;
    private final PasswordEncoder passwordEncoder;
    private final CaptureUsageService captureUsageService;
    private final Clock clock;

    public List<AdminSellerRow> listSellers() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Seller> sellers = sellerRepository.findAllByOrderByIdDesc();
        // 공구 수·캡처 사용량은 판매자마다 따로 읽지 않고 한 번에 읽는다 (전에는 판매자 1명당 3번 조회, 2026-10-21)
        Map<Long, Long> groupBuyCounts = groupBuyRepository.countPerSeller().stream()
                .collect(Collectors.toMap(SellerCount::getSellerId, SellerCount::getCount));
        Map<Long, CaptureUsageService.Usage> usages = captureUsageService.usages(sellers);
        return sellers.stream()
                .map(s -> {
                    CaptureUsageService.Usage usage = usages.get(s.getId());
                    return AdminSellerRow.builder()
                            .id(s.getId())
                            .loginId(s.getLoginId())
                            .email(s.getEmail())
                            .phone(PhoneNumbers.format(s.getPhone()))
                            .displayName(s.getDisplayName())
                            .role(s.getRole())
                            .plan(s.getPlan())
                            .trialEndsAt(s.getTrialEndsAt())
                            .paidUntil(s.getPaidUntil())
                            .expired(s.isExpired(now))
                            .suspended(s.isSuspended())
                            .adminMemo(s.getAdminMemo())
                            .createdAt(s.getCreatedAt())
                            .groupBuyCount(groupBuyCounts.getOrDefault(s.getId(), 0L))
                            .captureToday(usage.usedToday())
                            .captureThisMonth(usage.usedThisMonth())
                            .dailyCaptureLimit(usage.dailyLimit())
                            .customCaptureLimit(usage.customLimit())
                            .commentFeatureEnabled(s.isCommentFeatureEnabled())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void updateSeller(Long sellerId, AdminUpdateRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ApiException(ErrorCode.SELLER_NOT_FOUND));
        if (seller.getRole() == Role.ADMIN && request.suspended()) {
            throw new ApiException(ErrorCode.ADMIN_CANNOT_SUSPEND);
        }
        seller.setPlan(request.plan());
        seller.setPaidUntil(request.paidUntil());
        if (request.trialEndsAt() != null) {
            seller.setTrialEndsAt(request.trialEndsAt());
        }
        seller.setSuspended(request.suspended());
        seller.setAdminMemo(Texts.blankToNull(request.adminMemo()));
        seller.setDailyCaptureLimit(request.dailyCaptureLimit());
        if (request.commentFeatureEnabled() != null) {
            seller.setCommentFeatureEnabled(request.commentFeatureEnabled());
        }
    }

    /**
     * 판매자 정보 고치기 (2026-11-09: 관리자가 잘못 넣은 정보를 고칠 수 있게. 2026-11-20 아이디 추가, 관리자 자신도).
     * 이메일 인증 없이 바로 바뀐다. 아이디·이메일은 다른 계정과 겹치면 안 되고, 휴대폰은 비우면 지움, 새 비밀번호는 적었을 때만.
     * 로그인 토큰에는 판매자 id 만 들어 있어 이메일·아이디를 바꿔도 로그인은 그대로이고, 메일은 보낼 때 이메일을 읽어 새 주소로 간다.
     * 관리자 계정 이메일도 바꿀 수 있다 (전에는 막았음): 앱이 켜질 때 관리자가 하나도 없을 때만 만들어서(ensureAdmin) 하나 더 생기지 않음.
     * 비밀번호는 일방향 해시(BCrypt)로만 저장해서 지금 비밀번호를 보여 줄 수는 없다.
     */
    @Transactional
    public void updateSellerInfo(Long sellerId, SellerInfoRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ApiException(ErrorCode.SELLER_NOT_FOUND));
        String loginId = SellerService.requireLoginId(request.loginId());
        if (!loginId.equals(seller.getLoginId())) {
            if (sellerRepository.existsByLoginId(loginId)) {
                throw new ApiException(ErrorCode.LOGIN_ID_TAKEN);
            }
            seller.setLoginId(loginId);
        }
        String email = SellerService.normalizeEmail(request.email());
        if (!email.equals(seller.getEmail())) {
            if (sellerRepository.existsByEmail(email)) {
                throw new ApiException(ErrorCode.EMAIL_TAKEN);
            }
            seller.setEmail(email);
        }
        seller.setDisplayName(request.displayName().trim());
        seller.setPhone(SellerService.phoneOrNull(request.phone()));
        if (Texts.blankToNull(request.newPassword()) != null) {
            seller.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }
    }

    /** 관리자가 판매자 계정을 직접 만든다 (2026-11-09: 메일로 요청받은 경우 등). 회원가입을 꺼 둬도 된다 */
    @Transactional
    public void createSeller(SignupRequest request) {
        sellerService.create(request);
    }

    /**
     * 판매자 탈퇴(관리자 처리): 판매자의 공구·주문·입금 정보를 함께 파기한다 (개발 전 인터뷰 결과 8번).
     * FK 의 ON DELETE CASCADE 로 하위 데이터가 같이 지워진다.
     */
    @Transactional
    public void deleteSeller(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ApiException(ErrorCode.SELLER_NOT_FOUND));
        if (seller.getRole() == Role.ADMIN) {
            throw new ApiException(ErrorCode.ADMIN_CANNOT_DELETE);
        }
        sellerRepository.delete(seller);
    }

    /**
     * 관리자 계정이 하나도 없으면 만든다. 이미 있으면 아무것도 바꾸지 않는다 (비밀번호·이메일·아이디는 관리자 화면에서 바꿈).
     * 관리자가 화면에서 자기 이메일·아이디를 바꾼 뒤 앱을 다시 켜도 환경변수 값으로 관리자가 하나 더 생기지 않게 (2026-11-20).
     * 같은 이메일·아이디를 일반 판매자가 먼저 쓰고 있으면 관리자로 바꾸지 않고 false 를 돌려준다.
     * 아이디가 형식에 맞지 않아도 false.
     */
    @Transactional
    public boolean ensureAdmin(String rawLoginId, String email, String rawPassword) {
        if (sellerRepository.existsByRole(Role.ADMIN)) {
            return true;
        }
        String normalized = SellerService.normalizeEmail(email);
        if (sellerRepository.existsByEmail(normalized)) {
            return false;
        }
        String loginId = LoginIds.normalize(rawLoginId);
        if (loginId == null || sellerRepository.existsByLoginId(loginId)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        sellerRepository.save(new Seller(loginId, normalized, null, passwordEncoder.encode(rawPassword), "관리자",
                Role.ADMIN, now, now.plusYears(100)));
        return true;
    }
}
