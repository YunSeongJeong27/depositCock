package com.depositcock.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 가입 이메일 인증, 아이디 찾기, 비밀번호 찾기·변경 (2026-11-20) */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccountDtos {

    /** 가입 인증번호 받기 */
    public record SignupCodeRequest(
            @NotBlank(message = "EMAIL_REQUIRED") @Email(message = "EMAIL_INVALID")
            @Size(max = 255, message = "EMAIL_TOO_LONG") String email) {
    }

    /** 가입 인증번호 확인 */
    public record SignupVerifyRequest(
            @NotBlank(message = "EMAIL_REQUIRED") @Size(max = 255, message = "EMAIL_TOO_LONG") String email,
            @NotBlank(message = "EMAIL_CODE_REQUIRED") @Size(max = 20, message = "EMAIL_CODE_TOO_LONG") String code) {
    }

    /** 아이디 찾기: 가입 이메일 + 휴대폰 번호가 맞으면 아이디를 메일로 */
    public record FindLoginIdRequest(
            @NotBlank(message = "EMAIL_REQUIRED") @Size(max = 255, message = "EMAIL_TOO_LONG") String email,
            @NotBlank(message = "PHONE_REQUIRED") @Size(max = 30, message = "PHONE_TOO_LONG") String phone) {
    }

    /** 비밀번호 찾기·변경 인증번호 받기, 임시 비밀번호 받기: 아이디 + 가입 이메일 + 휴대폰 번호 */
    public record PasswordAccountRequest(
            @NotBlank(message = "LOGIN_ID_REQUIRED") @Size(max = 30, message = "LOGIN_ID_TOO_LONG") String loginId,
            @NotBlank(message = "EMAIL_REQUIRED") @Size(max = 255, message = "EMAIL_TOO_LONG") String email,
            @NotBlank(message = "PHONE_REQUIRED") @Size(max = 30, message = "PHONE_TOO_LONG") String phone) {
    }

    /** 비밀번호 찾기·변경 인증번호 확인 */
    public record PasswordVerifyRequest(
            @NotBlank(message = "LOGIN_ID_REQUIRED") @Size(max = 30, message = "LOGIN_ID_TOO_LONG") String loginId,
            @NotBlank(message = "EMAIL_REQUIRED") @Size(max = 255, message = "EMAIL_TOO_LONG") String email,
            @NotBlank(message = "PHONE_REQUIRED") @Size(max = 30, message = "PHONE_TOO_LONG") String phone,
            @NotBlank(message = "EMAIL_CODE_REQUIRED") @Size(max = 20, message = "EMAIL_CODE_TOO_LONG") String code) {
    }

    /** 비밀번호 변경 (인증번호 확인 뒤) */
    public record PasswordChangeRequest(
            @NotBlank(message = "LOGIN_ID_REQUIRED") @Size(max = 30, message = "LOGIN_ID_TOO_LONG") String loginId,
            @NotBlank(message = "EMAIL_REQUIRED") @Size(max = 255, message = "EMAIL_TOO_LONG") String email,
            @NotBlank(message = "PHONE_REQUIRED") @Size(max = 30, message = "PHONE_TOO_LONG") String phone,
            @NotBlank(message = "PASSWORD_REQUIRED") @Size(min = 8, max = 100, message = "PASSWORD_LENGTH") String newPassword) {
    }

    /** 인증번호를 보냈다는 응답: 몇 분 안에 넣어야 하는지 */
    public record CodeSentResponse(int validMinutes) {
    }
}
