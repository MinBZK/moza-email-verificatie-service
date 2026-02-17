package nl.rijksoverheid.moz.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import nl.rijksoverheid.moz.entity.VerificationCode;
import nl.rijksoverheid.moz.entity.VerificationStatistics;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class VerificationCleanupJob {

    private static final Logger LOG = Logger.getLogger(VerificationCleanupJob.class);

    @Scheduled(every = "{verification.cleanup.schedule}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void cleanUpSuccessfulVerifications() {
        // we can be sure that this method is never executed concurrently
        List<VerificationCode> codes = VerificationCode.findSuccessfulVerifications();
        if (!codes.isEmpty()) {
            LOG.info("Cleaning up " + codes.size() + " successful verifications");
        }

        for (VerificationCode code : codes) {
            VerificationStatistics statistics = new VerificationStatistics();
            statistics.setCreatedAt(code.getCreatedAt());
            statistics.setVerifyEmailSentAt(code.getVerifyEmailSentAt());
            statistics.setVerifiedAt(code.getVerifiedAt());
            statistics.persist();
        }

        codes.forEach(VerificationCode::delete);
    }

    @Scheduled(every = "{verification.cleanup.schedule}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void cleanUpExpiredCodes() {
        // we can be sure that this method is never executed concurrently
        List<VerificationCode> codes = VerificationCode.findExpiredCodes(java.time.LocalDateTime.now());
        if (!codes.isEmpty()) {
            LOG.info("Cleaning up " + codes.size() + " expired verification codes");
        }

        for (VerificationCode code : codes) {
            VerificationStatistics statistics = new VerificationStatistics();
            statistics.setCreatedAt(code.getCreatedAt());
            statistics.setVerifyEmailSentAt(code.getVerifyEmailSentAt());
            statistics.persist();
        }

        codes.forEach(VerificationCode::delete);
    }
}
