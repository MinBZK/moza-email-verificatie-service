package nl.rijksoverheid.moz.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class VerificationStatistics extends PanacheEntity {

    // Maybe good to keep track of statistics to see how fast people verify,
    // if it is always 9 min maybe change the timer to 15 instead of 10
    // and see what % of the codes are never verified

    private LocalDateTime createdAt;
    private LocalDateTime verifyEmailSentAt;
    private LocalDateTime verifiedAt; //if this is null, it means the code was never verified

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
}
