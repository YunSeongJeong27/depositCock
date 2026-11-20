package com.depositcock.account;

import java.time.Clock;
import java.time.LocalDateTime;

import com.depositcock.account.AccountDtos.CodeSentResponse;
import com.depositcock.account.AccountDtos.FindLoginIdRequest;
import com.depositcock.account.AccountDtos.PasswordAccountRequest;
import com.depositcock.account.AccountDtos.PasswordChangeRequest;
import com.depositcock.account.AccountDtos.PasswordVerifyRequest;
import com.depositcock.account.AccountDtos.PhoneChangeRequest;
import com.depositcock.account.AccountDtos.SignupCodeRequest;
import com.depositcock.account.AccountDtos.SignupVerifyRequest;
import com.depositcock.security.CurrentSeller;
import com.depositcock.security.TokenService;
import com.depositcock.seller.FeatureAccess;
import com.depositcock.seller.Seller;
import com.depositcock.seller.SellerDtos.MeResponse;
import com.depositcock.seller.SellerDtos.SignupRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가입(이메일 인증번호), 아이디 찾기, 비밀번호 찾기·변경 (2026-11-20). 로그인 없이 쓴다 (/api/auth/**).
 * 내 정보의 휴대폰 변경(/api/seller/phone)만 로그인이 필요하다.
 * 인증번호 받기·확인·찾기는 요청 제한(RateLimitFilter)에서 IP 마다 10분 횟수도 센다.
 */
@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TokenService tokenService;
    private final FeatureAccess featureAccess;
    private final Clock clock;

    @PostMapping("/api/auth/signup/email-code")
    public CodeSentResponse sendSignupCode(@Valid @RequestBody SignupCodeRequest request) {
        accountService.sendSignupCode(request.email());
        return new CodeSentResponse(EmailVerificationService.CODE_VALID_MINUTES);
    }

    @PostMapping("/api/auth/signup/email-verify")
    public void verifySignupCode(@Valid @RequestBody SignupVerifyRequest request) {
        accountService.verifySignupCode(request.email(), request.code());
    }

    @PostMapping("/api/auth/signup")
    public MeResponse signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        Seller seller = accountService.signup(request);
        tokenService.writeCookie(response, tokenService.issue(seller));
        return toMe(seller);
    }

    @PostMapping("/api/auth/find-login-id")
    public void findLoginId(@Valid @RequestBody FindLoginIdRequest request) {
        accountService.sendLoginId(request.email(), request.phone());
    }

    @PostMapping("/api/auth/password/email-code")
    public CodeSentResponse sendPasswordCode(@Valid @RequestBody PasswordAccountRequest request) {
        accountService.sendPasswordCode(request);
        return new CodeSentResponse(EmailVerificationService.CODE_VALID_MINUTES);
    }

    @PostMapping("/api/auth/password/email-verify")
    public void verifyPasswordCode(@Valid @RequestBody PasswordVerifyRequest request) {
        accountService.verifyPasswordCode(request.loginId(), request.email(), request.phone(), request.code());
    }

    /** 비밀번호 찾기: 임시 비밀번호를 메일로 */
    @PostMapping("/api/auth/password/reset")
    public void resetPassword(@Valid @RequestBody PasswordAccountRequest request) {
        accountService.resetPassword(request);
    }

    @PostMapping("/api/auth/password/change")
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        accountService.changePassword(request.loginId(), request.email(), request.phone(), request.newPassword());
    }

    /** 내 정보: 휴대폰 변경 인증번호를 가입 이메일로 (로그인 필요) */
    @PostMapping("/api/seller/phone/email-code")
    public CodeSentResponse sendPhoneCode() {
        accountService.sendPhoneCode(CurrentSeller.id());
        return new CodeSentResponse(EmailVerificationService.CODE_VALID_MINUTES);
    }

    /** 내 정보: 인증번호를 넣고 휴대폰 번호 변경 (로그인 필요) */
    @PutMapping("/api/seller/phone")
    public MeResponse changePhone(@Valid @RequestBody PhoneChangeRequest request) {
        Seller seller = accountService.changePhone(CurrentSeller.id(), request.phone(), request.code());
        return toMe(seller);
    }

    private MeResponse toMe(Seller seller) {
        return MeResponse.of(seller, LocalDateTime.now(clock), featureAccess.commentsAllowed(seller),
                featureAccess.depositCaptureOpen());
    }
}
