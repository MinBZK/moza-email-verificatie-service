package nl.rijksoverheid.moz.consumer;

import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import nl.rijksoverheid.moz.entity.VerificationCode;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.concurrent.CompletionStage;


@ApplicationScoped
public class VerificationRequestHandler {

    private static final Logger LOG = Logger.getLogger(VerificationRequestHandler.class);

    @Incoming("verification-requests-in")
    @Blocking
    @Transactional
    public CompletionStage<Void> consume(Message<String> requestMessage){
        String payload = requestMessage.getPayload();
        LOG.info("Received verification request message with code ID: " + payload);

        try {
            Long codeId = Long.parseLong(payload);
            VerificationCode code = VerificationCode.findById(codeId);
            if (code != null) {
                // todo instead of logging, send an email to the user using NotifyNL for now
                LOG.info("Verification code found for reference ID: " + code.getReferenceId());

                code.setVerifyEmailSentAt(LocalDateTime.now());
                code.persist();
            } else {
                LOG.warn("No verification code found for ID: " + codeId);
            };

        } catch (NumberFormatException e) {
            LOG.error("Failed to parse verification request ID from message: " + payload, e);
        }

        return requestMessage.ack();
    }
}