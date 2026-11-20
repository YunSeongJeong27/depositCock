package com.depositcock.admin;

import java.util.List;

import java.time.Clock;
import java.time.LocalDateTime;

import com.depositcock.admin.AdminDtos.AdminInfoRequest;
import com.depositcock.admin.AdminDtos.AdminLoginIdRequest;
import com.depositcock.admin.AdminDtos.AdminPasswordRequest;
import com.depositcock.admin.AdminDtos.AdminSellerRow;
import com.depositcock.admin.AdminDtos.AdminSettings;
import com.depositcock.admin.AdminDtos.AdminUpdateRequest;
import com.depositcock.seller.FeatureAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 슈퍼 관리자(윤성) 전용: 판매자 목록, 플랜·유료 만료일·사용 제한·댓글 기능 허용 변경, 이름·이메일·휴대폰·아이디·비밀번호 변경,
 * 탈퇴 처리,
 * 서비스 전체 설정.
 */
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final FeatureAccess featureAccess;
    private final Clock clock;

    @GetMapping("/api/admin/settings")
    public AdminSettings settings() {
        return new AdminSettings(featureAccess.commentFeatureOpen());
    }

    /** 댓글 주문 기능 전체 공개 켜기/끄기 */
    @PutMapping("/api/admin/settings")
    public AdminSettings updateSettings(@RequestBody AdminSettings request) {
        featureAccess.setCommentFeatureOpen(request.commentFeatureOpen(), LocalDateTime.now(clock));
        return settings();
    }

    @GetMapping("/api/admin/sellers")
    public List<AdminSellerRow> sellers() {
        return adminService.listSellers();
    }

    @PutMapping("/api/admin/sellers/{sellerId}")
    public void update(@PathVariable Long sellerId, @Valid @RequestBody AdminUpdateRequest request) {
        adminService.updateSeller(sellerId, request);
    }

    /** 판매자 정보(이름·이메일·휴대폰) 바꾸기 (2026-11-20). 이메일 인증 없이 바로 바뀐다 */
    @PutMapping("/api/admin/sellers/{sellerId}/info")
    public void changeInfo(@PathVariable Long sellerId, @Valid @RequestBody AdminInfoRequest request) {
        adminService.changeInfo(sellerId, request);
    }

    /** 아이디 바꾸기 (2026-11-20) */
    @PutMapping("/api/admin/sellers/{sellerId}/login-id")
    public void changeLoginId(@PathVariable Long sellerId, @Valid @RequestBody AdminLoginIdRequest request) {
        adminService.changeLoginId(sellerId, request.loginId());
    }

    /** 비밀번호 새로 정하기 (2026-11-20). 지금 비밀번호는 해시로만 저장돼 보여 줄 수 없다 */
    @PutMapping("/api/admin/sellers/{sellerId}/password")
    public void changePassword(@PathVariable Long sellerId, @Valid @RequestBody AdminPasswordRequest request) {
        adminService.changePassword(sellerId, request.password());
    }

    @DeleteMapping("/api/admin/sellers/{sellerId}")
    public void delete(@PathVariable Long sellerId) {
        adminService.deleteSeller(sellerId);
    }
}
