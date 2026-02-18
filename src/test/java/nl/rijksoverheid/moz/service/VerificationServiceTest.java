package nl.rijksoverheid.moz.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.rijksoverheid.moz.entity.VerificationCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@QuarkusTest
public class VerificationServiceTest {

    @Inject
    VerificationService verificationService;

    @InjectMock
    NotifyNLService notifyNLService;

    @Test
    @Transactional
    public void testProcessSuccess() {
        VerificationCode code = new VerificationCode("test@example.com");
        code.persist();
        
        when(notifyNLService.sendVerificationEmail(ArgumentMatchers.any(VerificationCode.class))).thenReturn(true);
        
        verificationService.process(String.valueOf(code.id));
        
        VerificationCode updatedCode = VerificationCode.findById(code.id);
        Assertions.assertNotNull(updatedCode.getVerifyEmailSentAt());
        verify(notifyNLService, times(1)).sendVerificationEmail(ArgumentMatchers.any(VerificationCode.class));
    }

    @Test
    @Transactional
    public void testProcessExpired() {
        VerificationCode code = new VerificationCode("test-expired@example.com");
        code.setValidUntil(LocalDateTime.now().minusMinutes(1));
        code.persist();
        
        verificationService.process(String.valueOf(code.id));
        
        VerificationCode updatedCode = VerificationCode.findById(code.id);
        Assertions.assertNull(updatedCode.getVerifyEmailSentAt());
        verify(notifyNLService, never()).sendVerificationEmail(ArgumentMatchers.any(VerificationCode.class));
    }

    @Test
    @Transactional
    public void testProcessFailure() {
        VerificationCode code = new VerificationCode("test-failure@example.com");
        code.persist();
        
        when(notifyNLService.sendVerificationEmail(ArgumentMatchers.any(VerificationCode.class))).thenReturn(false);
        
        Assertions.assertThrows(RuntimeException.class, () -> {
            verificationService.process(String.valueOf(code.id));
        });
        
        // Use findById and clear to ensure we get fresh state if needed
        VerificationCode updatedCode = VerificationCode.findById(code.id);
        Assertions.assertNull(updatedCode.getVerifyEmailSentAt());
    }
    
    @Test
    @Transactional
    public void testProcessNonExistent() {
        // Should not throw exception, just log warning
        verificationService.process("999999");
        verify(notifyNLService, never()).sendVerificationEmail(ArgumentMatchers.any());
    }

    @Test
    @Transactional
    public void testProcessInvalidPayload() {
        // Should not throw exception, just log error
        verificationService.process("invalid-id");
        verify(notifyNLService, never()).sendVerificationEmail(ArgumentMatchers.any());
    }
}
