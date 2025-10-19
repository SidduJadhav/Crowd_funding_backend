package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByProfileId(Long profileId);

    List<BankAccount> findByProfileIdAndIsActiveTrue(Long profileId);

    Optional<BankAccount> findByProfileIdAndIsPrimaryTrueAndIsActiveTrue(Long profileId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.isVerified = false AND ba.verificationDocumentUrl IS NOT NULL")
    List<BankAccount> findByIsVerifiedFalseAndVerificationDocumentUrlIsNotNull();

    @Query("SELECT COUNT(ba) FROM BankAccount ba WHERE ba.profile.id = :profileId AND ba.isActive = true")
    long countActiveAccountsByProfileId(@Param("profileId") Long profileId);

    boolean existsByProfileIdAndAccountNumberAndIsActiveTrue(Long profileId, String accountNumber);
}
