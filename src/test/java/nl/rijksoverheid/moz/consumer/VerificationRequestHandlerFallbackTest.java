package nl.rijksoverheid.moz.consumer;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import nl.rijksoverheid.moz.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
public class VerificationRequestHandlerFallbackTest {

    @Inject
    VerificationRequestHandler handler;

    @InjectMock
    VerificationService verificationService;

    @Test
    public void testFallbackIsCalledAfterMaxRetries() throws Exception {
        // Mock the service to throw exception every time
        doThrow(new RuntimeException("Persistent failure")).when(verificationService).process(anyString());

        // This call will be asynchronous and intercepted by Fault Tolerance
        CompletionStage<Void> result = handler.consume("123");
        
        // Wait for completion (with timeout)
        result.toCompletableFuture().get(10, TimeUnit.SECONDS);

        // Verify it was called 6 times (1 initial + 5 retries)
        verify(verificationService, times(6)).process("123");
        
        // If we reach here, it means the RuntimeException was handled by @Fallback
    }
}
