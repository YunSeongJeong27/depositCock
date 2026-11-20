package com.depositcock.account;

import java.time.LocalDateTime;

import com.depositcock.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메일로 보낸 인증번호 1건 (2026-11-20). 인증번호는 해시만 저장한다.
 * 확인되면 verifiedAt 이 생기고, 가입·비밀번호 바꾸기에 쓰면 줄을 지운다.
 */
@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationPurpose purpose;

    @Column(nullable = false)
    private String email;

    /** 비밀번호 찾기·변경이면 그 판매자. 가입이면 null */
    private Long sellerId;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int failedAttempts;

    private LocalDateTime verifiedAt;

    public EmailVerification(VerificationPurpose purpose, String email, Long sellerId, String codeHash,
                             LocalDateTime sentAt, LocalDateTime expiresAt) {
        this.purpose = purpose;
        this.email = email;
        this.sellerId = sellerId;
        this.codeHash = codeHash;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
    }

    void fail() {
        this.failedAttempts++;
    }

    void markVerified(LocalDateTime now) {
        this.verifiedAt = now;
    }
}
