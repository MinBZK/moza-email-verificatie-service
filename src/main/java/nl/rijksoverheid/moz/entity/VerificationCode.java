package nl.rijksoverheid.moz.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.smallrye.common.constraint.Nullable;
import jakarta.persistence.*;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.annotations.UpdateTimestamp;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
public class VerificationCode extends PanacheEntity {

    private String referenceId;
    private String email;
    private String code;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "verify_email_sent_at")
    private LocalDateTime verifyEmailSentAt;

    @Nullable
    private LocalDateTime verifiedAt;

    @Nullable
    private LocalDateTime validUntil;

    private static final SecureRandom RANDOM = new SecureRandom();

    public VerificationCode() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (validUntil == null) {
            int validityMinutes = ConfigProvider.getConfig()
                    .getOptionalValue("verification.code.validity-minutes", Integer.class)
                    .orElse(10);
            validUntil = createdAt.plusMinutes(validityMinutes);
        }
        if (referenceId == null) {
            referenceId = UUID.randomUUID().toString();
        }

        if (code == null) {
            code = String.valueOf(100000 + RANDOM.nextInt(900000));
        }
    }

    public VerificationCode(String email) {
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getVerifyEmailSentAt() {
        return verifyEmailSentAt;
    }

    public void setVerifyEmailSentAt(LocalDateTime verifyEmailSentAt) {
        this.verifyEmailSentAt = verifyEmailSentAt;
    }

    public boolean isUsed() {
        return verifiedAt != null;
    }


    public static Optional<VerificationCode> findByReferenceIdAndEmail(String referenceId, String email) {
        return find("referenceId = ?1 and email = ?2", referenceId, email).singleResultOptional();
    }

    public static List<VerificationCode> findSuccessfulVerifications() {
        return find("verifiedAt is not null").list();
    }

    public static List<VerificationCode> findExpiredCodes(LocalDateTime now) {
        return find("validUntil < ?1", now).list();
    }


}
