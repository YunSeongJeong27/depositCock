package com.depositcock.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.depositcock.seller.Plan;
import com.depositcock.seller.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AdminDtos {

    @Builder
    public record AdminSellerRow(
            Long id,
            String loginId,
            String email,
            /** 010-1234-5678 모양 (없으면 null) */
            String phone,
            String displayName,
            Role role,
            Plan plan,
            LocalDateTime trialEndsAt,
            LocalDate paidUntil,
            boolean expired,
            boolean suspended,
            String adminMemo,
            LocalDateTime createdAt,
            long groupBuyCount,
            int captureToday,
            int captureThisMonth,
            int dailyCaptureLimit,
            boolean customCaptureLimit,
            /** 판매자별 댓글 주문 기능 허용 (전체 공개와 별개) */
            boolean commentFeatureEnabled) {
    }

    public record AdminUpdateRequest(
            @NotNull(message = "PLAN_REQUIRED") Plan plan,
            LocalDate paidUntil,
            LocalDateTime trialEndsAt,
            boolean suspended,
            @Size(max = 1000, message = "ADMIN_MEMO_TOO_LONG") String adminMemo,
            /** 하루 캡처 제한 장수. null 이면 기본값(설정) 사용 */
            @Min(value = 0, message = "CAPTURE_LIMIT_RANGE")
            @Max(value = 10000, message = "CAPTURE_LIMIT_RANGE") Integer dailyCaptureLimit,
            /** 판매자별 댓글 주문 기능 허용. null 이면 그대로 둔다 */
            Boolean commentFeatureEnabled) {
    }

    /** 관리자가 아이디 바꾸기 (2026-11-20) */
    public record AdminLoginIdRequest(
            @NotBlank(message = "LOGIN_ID_REQUIRED") @Size(max = 30, message = "LOGIN_ID_TOO_LONG") String loginId) {
    }

    /** 관리자가 판매자 정보(이름·이메일·휴대폰) 바꾸기 (2026-11-20). 휴대폰은 비우면 지움 */
    public record AdminInfoRequest(
            @NotBlank(message = "SELLER_NAME_REQUIRED") @Size(max = 100, message = "SELLER_NAME_TOO_LONG") String displayName,
            @NotBlank(message = "EMAIL_REQUIRED") @Email(message = "EMAIL_INVALID")
            @Size(max = 255, message = "EMAIL_TOO_LONG") String email,
            @Size(max = 30, message = "PHONE_TOO_LONG") String phone) {
    }

    /** 관리자가 비밀번호 새로 정하기 (2026-11-20) */
    public record AdminPasswordRequest(
            @NotBlank(message = "PASSWORD_REQUIRED") @Size(min = 8, max = 100, message = "PASSWORD_LENGTH") String password) {
    }

    /** 서비스 전체 설정 (관리자) */
    public record AdminSettings(boolean commentFeatureOpen) {
    }
}
