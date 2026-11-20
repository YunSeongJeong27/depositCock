package com.depositcock.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

import com.depositcock.common.ApiException;
import com.depositcock.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증번호 (2026-11-20): 만들기 → 확인 → 쓰기(가입·비밀번호 바꾸기).
 * <ul>
 *   <li>6자리 숫자, 10분 동안 유효. 같은 용도·이메일로는 1분에 한 번만 보낸다 (메일 폭탄 방지)</li>
 *   <li>5번 틀리면 그 인증번호는 못 쓰고 다시 받아야 한다 (6자리 대입 방지)</li>
 *   <li>확인한 뒤 30분 안에 가입·비밀번호 바꾸기를 해야 한다</li>
 * </ul>
 * 인증번호 원문은 저장하지 않고 SHA-256 해시만 둔다. 메일 보내기는 부르는 쪽(AccountService)이 한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    public static final int CODE_VALID_MINUTES = 10;
    static final int VERIFIED_VALID_MINUTES = 30;
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final int RESEND_SECONDS = 60;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository repository;
    private final Clock clock;

    /** 새 인증번호를 만들어 저장하고 원문을 돌려준다. 전에 보낸 인증번호는 지운다 */
    @Transactional
    public String issue(VerificationPurpose purpose, String email, Long sellerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.findFirstByPurposeAndEmailOrderByIdDesc(purpose, email).ifPresent(last -> {
            if (last.getSentAt().plusSeconds(RESEND_SECONDS).isAfter(now)) {
                throw new ApiException(ErrorCode.EMAIL_CODE_TOO_SOON);
            }
        });
        repository.deleteByPurposeAndEmail(purpose, email);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        repository.save(new EmailVerification(purpose, email, sellerId, hash(code), now,
                now.plusMinutes(CODE_VALID_MINUTES)));
        return code;
    }

    /**
     * 인증번호 확인. 틀린 횟수는 오류가 나도 남아야 하므로 ApiException 으로 되돌리지 않는다
     * (부르는 쪽 트랜잭션도 noRollbackFor = ApiException 이어야 함).
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void verify(VerificationPurpose purpose, String email, Long sellerId, String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        EmailVerification v = repository.findFirstByPurposeAndEmailOrderByIdDesc(purpose, email)
                .filter(found -> Objects.equals(found.getSellerId(), sellerId))
                .orElseThrow(() -> new ApiException(ErrorCode.EMAIL_CODE_NOT_SENT));
        if (v.getVerifiedAt() != null) {
            return;
        }
        if (v.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new ApiException(ErrorCode.EMAIL_CODE_LOCKED, MAX_FAILED_ATTEMPTS);
        }
        if (!v.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.EMAIL_CODE_EXPIRED);
        }
        String typed = code == null ? "" : code.replaceAll("\\s", "");
        if (!MessageDigest.isEqual(hash(typed).getBytes(StandardCharsets.UTF_8),
                v.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            v.fail();
            if (v.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                throw new ApiException(ErrorCode.EMAIL_CODE_LOCKED, MAX_FAILED_ATTEMPTS);
            }
            throw new ApiException(ErrorCode.EMAIL_CODE_WRONG);
        }
        v.markVerified(now);
    }

    /** 확인된 인증을 한 번 쓴다 (가입·비밀번호 바꾸기). 확인 안 됐거나 30분이 지났으면 EMAIL_NOT_VERIFIED */
    @Transactional
    public void consume(VerificationPurpose purpose, String email, Long sellerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        EmailVerification v = repository.findFirstByPurposeAndEmailOrderByIdDesc(purpose, email)
                .filter(found -> Objects.equals(found.getSellerId(), sellerId))
                .filter(found -> found.getVerifiedAt() != null
                        && found.getVerifiedAt().plusMinutes(VERIFIED_VALID_MINUTES).isAfter(now))
                .orElseThrow(() -> new ApiException(ErrorCode.EMAIL_NOT_VERIFIED));
        repository.delete(v);
    }

    /** 유효 시간이 지나고 하루가 넘은 인증번호(가입을 마치지 않은 사람의 이메일 포함)를 지운다. 1시간마다 */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void deleteOld() {
        int deleted = repository.deleteExpiredBefore(LocalDateTime.now(clock).minusDays(1));
        if (deleted > 0) {
            log.info("지난 이메일 인증번호 {}건을 지웠습니다.", deleted);
        }
    }

    static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
