package nl.rijksoverheid.moz.consumer;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import nl.rijksoverheid.moz.service.VerificationService;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class VerificationRequestHandler {

    private static final Logger LOG = Logger.getLogger(VerificationRequestHandler.class);
    
    @Inject
    VerificationService verificationService;

    @Incoming("verification-requests-in")
    @Asynchronous
    @Retry(maxRetries = 5, delay = 2, delayUnit = ChronoUnit.SECONDS, maxDuration = 15, durationUnit = ChronoUnit.MINUTES)
    @ExponentialBackoff(factor = 2, maxDelay = 300, maxDelayUnit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "onMaxRetriesReached")
    public CompletionStage<Void> consume(String payload) {
        LOG.info("Received verification request message with code ID: " + payload);
        verificationService.process(payload);
        return CompletableFuture.completedFuture(null);
    }

    public CompletionStage<Void> onMaxRetriesReached(String payload) {
        LOG.warn("reached max tries, gracefully deleting this message from the queue for code ID: " + payload);
        return CompletableFuture.completedFuture(null);
    }
}