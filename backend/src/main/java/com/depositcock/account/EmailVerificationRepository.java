package com.depositcock.account;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findFirstByPurposeAndEmailOrderByIdDesc(VerificationPurpose purpose, String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EmailVerification v where v.purpose = :purpose and v.email = :email")
    int deleteByPurposeAndEmail(@Param("purpose") VerificationPurpose purpose, @Param("email") String email);

    @Modifying
    @Query("delete from EmailVerification v where v.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") LocalDateTime before);
}
