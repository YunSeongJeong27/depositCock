package com.depositcock.account;

import java.security.SecureRandom;
import java.util.Locale;

import com.depositcock.account.AccountDtos.PasswordAccountRequest;
import com.depositcock.common.ApiException;
import com.depositcock.common.ErrorCode;
import com.depositcock.common.PhoneNumbers;
import com.depositcock.notification.AccountMailer;
import com.depositcock.seller.Seller;
import com.depositcock.seller.SellerDtos.SignupRequest;
import com.depositcock.seller.SellerRepository;
import com.depositcock.seller.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가입 이메일 인증, 아이디 찾기, 비밀번호 찾기·변경 (2026-11-20 사용자 결정).
 * <ul>
 *   <li>가입: 이메일로 받은 인증번호를 확인해야 가입된다</li>
 *   <li>아이디 찾기: 가입 이메일 + 휴대폰 번호가 맞으면 아이디를 그 이메일로 바로 보낸다</li>
 *   <li>비밀번호 찾기: 아이디 + 가입 이메일 + 휴대폰 번호가 맞으면 인증번호를 보내고, 확인되면 임시 비밀번호를 메일로</li>
 *   <li>비밀번호 변경: 같은 확인 + 인증번호 확인 뒤 새 비밀번호로</li>
 * </ul>
 * 비밀번호 찾기에 인증번호를 거치는 이유: 거치지 않으면 남이 아이디·이메일·휴대폰만 알아도 그 사람 비밀번호를
 * 임시 비밀번호로 바꿔 버릴 수 있다. 휴대폰 인증은 하지 않는다 (사용자 결정).
 * 메일을 못 보내면 요청 전체가 실패한다 (임시 비밀번호가 바뀌지 않음).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    /** 임시 비밀번호 글자: 헷갈리는 0·o·1·l·i 를 뺀 영문 소문자·숫자 */
    private static final String TEMP_PASSWORD_CHARS = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SellerService sellerService;
    private final SellerRepository sellerRepository;
    private final EmailVerificationService verifications;
    private final AccountMailer mailer;
    private final PasswordEncoder passwordEncoder;

    public void sendSignupCode(String rawEmail) {
        String email = SellerService.normalizeEmail(rawEmail);
        if (sellerRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        String code = verifications.issue(VerificationPurpose.SIGNUP, email, null);
        mailer.signupCode(email, code, EmailVerificationService.CODE_VALID_MINUTES);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void verifySignupCode(String rawEmail, String code) {
        verifications.verify(VerificationPurpose.SIGNUP, SellerService.normalizeEmail(rawEmail), null, code);
    }

    /** 아이디·이메일·휴대폰 검사(SellerService)를 먼저 하고 인증을 쓴다. 인증이 없으면 판매자 저장도 되돌아간다 */
    public Seller signup(SignupRequest request) {
        Seller seller = sellerService.signup(request);
        verifications.consume(VerificationPurpose.SIGNUP, seller.getEmail(), null);
        return seller;
    }

    @Transactional(readOnly = true)
    public void sendLoginId(String rawEmail, String rawPhone) {
        Seller seller = byEmailAndPhone(rawEmail, rawPhone);
        mailer.loginId(seller.getEmail(), seller.getLoginId());
    }

    public void sendPasswordCode(PasswordAccountRequest request) {
        Seller seller = byAccount(request.loginId(), request.email(), request.phone());
        String code = verifications.issue(VerificationPurpose.PASSWORD, seller.getEmail(), seller.getId());
        mailer.passwordCode(seller.getEmail(), code, EmailVerificationService.CODE_VALID_MINUTES);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void verifyPasswordCode(String loginId, String email, String phone, String code) {
        Seller seller = byAccount(loginId, email, phone);
        verifications.verify(VerificationPurpose.PASSWORD, seller.getEmail(), seller.getId(), code);
    }

    /** 비밀번호 찾기: 임시 비밀번호로 바꾸고 메일로 보낸다. 메일을 못 보내면 바꾸지 않는다 */
    public void resetPassword(PasswordAccountRequest request) {
        Seller seller = byAccount(request.loginId(), request.email(), request.phone());
        verifications.consume(VerificationPurpose.PASSWORD, seller.getEmail(), seller.getId());
        String temp = tempPassword();
        seller.setPasswordHash(passwordEncoder.encode(temp));
        mailer.tempPassword(seller.getEmail(), temp);
    }

    public void changePassword(String loginId, String email, String phone, String newPassword) {
        Seller seller = byAccount(loginId, email, phone);
        verifications.consume(VerificationPurpose.PASSWORD, seller.getEmail(), seller.getId());
        seller.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    /** 가입 이메일 + 휴대폰 번호가 모두 맞는 판매자. 하나라도 다르면 어느 것이 다른지 알리지 않고 ACCOUNT_NOT_FOUND */
    private Seller byEmailAndPhone(String rawEmail, String rawPhone) {
        String phone = PhoneNumbers.normalize(rawPhone);
        if (phone == null) {
            throw new ApiException(ErrorCode.PHONE_INVALID);
        }
        return sellerRepository.findByEmail(SellerService.normalizeEmail(rawEmail))
                .filter(s -> phone.equals(s.getPhone()))
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private Seller byAccount(String rawLoginId, String rawEmail, String rawPhone) {
        String loginId = rawLoginId.trim().toLowerCase(Locale.ROOT);
        Seller seller = byEmailAndPhone(rawEmail, rawPhone);
        if (!loginId.equals(seller.getLoginId())) {
            throw new ApiException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return seller;
    }

    static String tempPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
