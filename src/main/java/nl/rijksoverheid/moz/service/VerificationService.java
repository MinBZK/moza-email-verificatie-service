package nl.rijksoverheid.moz.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.rijksoverheid.moz.entity.VerificationCode;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

@ApplicationScoped
public class VerificationService {

    private static final Logger LOG = Logger.getLogger(VerificationService.class);

    @Inject
    NotifyNLService notifyNLService;

    @Transactional
    public void process(String payload) {
        try {
            Long codeId = Long.parseLong(payload);
            VerificationCode code = VerificationCode.findById(codeId);
            if (code != null) {
                if (code.getValidUntil().isBefore(LocalDateTime.now())) {
                    LOG.warn("Verification code expired for reference ID: " + code.getReferenceId() + ". Skipping.");
                    return;
                }

                if (notifyNLService.sendVerificationEmail(code)) {
                    LOG.info("Verification code sent for reference ID: " + code.getReferenceId());
                    code.setVerifyEmailSentAt(LocalDateTime.now());
                } else {
                    LOG.error("Failed to send verification code for reference ID: " + code.getReferenceId() + ". Throwing exception to trigger retry.");
                    throw new RuntimeException("External service call failed");
                }
            } else {
                LOG.warn("No verification code found for ID: " + codeId);
            }

        } catch (NumberFormatException e) {
            LOG.error("Failed to parse verification request ID from message: " + payload, e);
        }
    }
}
