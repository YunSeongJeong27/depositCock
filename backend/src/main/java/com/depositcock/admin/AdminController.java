package com.depositcock.admin;

import java.util.List;

import java.time.Clock;
import java.time.LocalDateTime;

import com.depositcock.admin.AdminDtos.AdminSellerRow;
import com.depositcock.admin.AdminDtos.AdminSettings;
import com.depositcock.admin.AdminDtos.AdminUpdateRequest;
import com.depositcock.admin.AdminDtos.SellerInfoRequest;
import com.depositcock.seller.FeatureAccess;
import com.depositcock.seller.SellerDtos.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 슈퍼 관리자(윤성) 전용: 판매자 목록, 플랜·유료 만료일·사용 제한·댓글 기능 허용 변경, 탈퇴 처리, 서비스 전체 설정.
 */
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final FeatureAccess featureAccess;
    private final Clock clock;

    @GetMapping("/api/admin/settings")
    public AdminSettings settings() {
        return new AdminSettings(featureAccess.commentFeatureOpen(), featureAccess.signupOpen(), featureAccess.depositCaptureOpen());
    }

    /** 댓글 주문 기능 전체 공개, 새 회원가입 받기, 은행앱 화면 캡처로 올리기 켜기/끄기 */
    @PutMapping("/api/admin/settings")
    public AdminSettings updateSettings(@RequestBody AdminSettings request) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (request.commentFeatureOpen() != null) {
            featureAccess.setCommentFeatureOpen(request.commentFeatureOpen(), now);
        }
        if (request.signupOpen() != null) {
            featureAccess.setSignupOpen(request.signupOpen(), now);
        }
        if (request.depositCaptureOpen() != null) {
            featureAccess.setDepositCaptureOpen(request.depositCaptureOpen(), now);
        }
        return settings();
    }

    @GetMapping("/api/admin/sellers")
    public List<AdminSellerRow> sellers() {
        return adminService.listSellers();
    }

    /** 판매자 계정 직접 만들기 (2026-11-09) */
    @PostMapping("/api/admin/sellers")
    public void create(@Valid @RequestBody SignupRequest request) {
        adminService.createSeller(request);
    }

    @PutMapping("/api/admin/sellers/{sellerId}")
    public void update(@PathVariable Long sellerId, @Valid @RequestBody AdminUpdateRequest request) {
        adminService.updateSeller(sellerId, request);
    }

    /** 판매자 정보 고치기: 이메일·판매자 이름·휴대폰·새 비밀번호 (2026-11-09) */
    @PutMapping("/api/admin/sellers/{sellerId}/info")
    public void updateInfo(@PathVariable Long sellerId, @Valid @RequestBody SellerInfoRequest request) {
        adminService.updateSellerInfo(sellerId, request);
    }

    @DeleteMapping("/api/admin/sellers/{sellerId}")
    public void delete(@PathVariable Long sellerId) {
        adminService.deleteSeller(sellerId);
    }
}
