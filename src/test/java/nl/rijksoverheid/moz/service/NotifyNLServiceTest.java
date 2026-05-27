package nl.rijksoverheid.moz.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;
import nl.rijksoverheid.moz.entity.VerificationCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class NotifyNLServiceTest {

    private NotifyNLService notifyNLService;

    @BeforeEach
    void setup() throws Exception {
        notifyNLService = new NotifyNLService();
        circuitBreakerMaintenance.resetAll();
    }

    @Inject
    NotifyNLService injectedNotifyNLService;

    @Inject
    CircuitBreakerMaintenance circuitBreakerMaintenance;

    @InjectMock
    HttpClient mockHttpClient;

    @Test
    void testExtractServiceIdAndApiKey() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        // Valid API key format: some-prefix-SERVICE_ID-SECRET
        // From code: 
        // serviceId = fromApiKey.substring(keyLength - 73, keyLength - 73 + 36);
        // secret = fromApiKey.substring(keyLength - 36);
        // Total length >= 74
        
        String serviceId = "00000000-0000-0000-0000-000000000000";
        String secret = "00000000-0000-0000-0000-000000000000";
        String apiKey = "prefix-" + serviceId + "-" + secret;
        // Adjust length to meet requirement >= 74
        while (apiKey.length() < 74) {
            apiKey = "a" + apiKey;
        }

        Object result = method.invoke(null, apiKey);
        
        Method getServiceId = result.getClass().getDeclaredMethod("serviceId");
        Method getSecret = result.getClass().getDeclaredMethod("secret");
        
        Assertions.assertEquals(serviceId, getServiceId.invoke(result));
        Assertions.assertEquals(secret, getSecret.invoke(result));
    }

    @Test
    void testExtractServiceIdAndApiKeyInvalid() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        // Too short
        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, "too-short");
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });

        // Null
        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, (String) null);
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    void testCreateToken() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("createToken", String.class, String.class);
        method.setAccessible(true);

        String secret = "00000000-0000-0000-0000-000000000000";
        String serviceId = "00000000-0000-0000-0000-000000000000";

        String token = (String) method.invoke(null, secret, serviceId);
        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isEmpty());
    }

    @Test
    void testBuildJsonBody() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("buildJsonBody", String.class, String.class, String.class);
        method.setAccessible(true);

        String json = (String) method.invoke(injectedNotifyNLService, "123456", "test@example.com", "my-template-id");
        Assertions.assertTrue(json.contains("\"code\":\"123456\""));
        Assertions.assertTrue(json.contains("\"email_address\":\"test@example.com\""));
        Assertions.assertTrue(json.contains("\"template_id\":\"my-template-id\""));
    }

    @Test
    void testExtractServiceIdAndApiKeySpaces() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, "a".repeat(74) + " ");
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    void testExtractServiceIdAndApiKeyBlank() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, "   ");
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });
    }

    @Test
    void testExtractServiceIdAndApiKeyEmpty() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("extractServiceIdAndApiKey", String.class);
        method.setAccessible(true);

        Assertions.assertThrows(Exception.class, () -> {
            try {
                method.invoke(null, "");
            } catch (Exception e) {
                throw (Exception) e.getCause();
            }
        });
    }

    private static final String VALID_SERVICE_ID = "00000000-0000-0000-0000-000000000000";
    private static final String VALID_SECRET     = "00000000-0000-0000-0000-000000000000";
    private static final String VALID_API_KEY    = "prefix-" + VALID_SERVICE_ID + "-" + VALID_SECRET;

    @Test
    void testSendVerificationEmailSuccess() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock();
        Mockito.when(mockResponse.statusCode()).thenReturn(200);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        Assertions.assertTrue(injectedNotifyNLService.sendVerificationEmail(code, "test@example.com"));
    }

    @Test
    void testSendVerificationEmailFailure() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock();
        Mockito.when(mockResponse.statusCode()).thenReturn(400);
        Mockito.when(mockResponse.body()).thenReturn("Bad request");
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        Assertions.assertFalse(injectedNotifyNLService.sendVerificationEmail(code, "test@example.com"));
    }

    @Test
    void testSendVerificationEmailException() throws Exception {
        Mockito.doThrow(new IOException("Connection refused")).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        Assertions.assertFalse(injectedNotifyNLService.sendVerificationEmail(code, "test@example.com"));
    }

    @Test
    void testSendVerificationEmailWithCustomApiKeyAndTemplateId() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock();
        Mockito.when(mockResponse.statusCode()).thenReturn(201);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        Assertions.assertTrue(injectedNotifyNLService.sendVerificationEmail(
                code, "test@example.com", VALID_API_KEY, "custom-template-id"));
    }

    private static final int RATE_LIMIT_MAX = 5;

    @Test
    void testSendVerificationEmailRateLimited() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock();
        Mockito.when(mockResponse.statusCode()).thenReturn(200);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        VerificationCode code = new VerificationCode();
        String email = "ratelimit@example.com";

        for (int i = 0; i < RATE_LIMIT_MAX; i++) {
            Assertions.assertTrue(injectedNotifyNLService.sendVerificationEmail(code, email));
        }

        Assertions.assertFalse(injectedNotifyNLService.sendVerificationEmail(code, email));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCleanupRateLimits() throws Exception {
        Field rateLimitsField = NotifyNLService.class.getDeclaredField("rateLimits");
        rateLimitsField.setAccessible(true);
        Map<String, List<Instant>> rateLimits = (Map<String, List<Instant>>) rateLimitsField.get(notifyNLService);

        // Future timestamp: not before Instant.now() so survives cleanup regardless of window
        List<Instant> freshEntry = new ArrayList<>();
        freshEntry.add(Instant.now().plus(Duration.ofMinutes(30)));
        rateLimits.put("fresh@example.com", freshEntry);

        // 2 hours in the past: expired (rateLimitWindowHours is 0 on non-CDI instance, cutoff = now)
        List<Instant> expiredEntry = new ArrayList<>();
        expiredEntry.add(Instant.now().minus(Duration.ofHours(2)));
        rateLimits.put("expired@example.com", expiredEntry);

        notifyNLService.cleanupRateLimits();

        Assertions.assertFalse(rateLimits.containsKey("expired@example.com"));
        Assertions.assertTrue(rateLimits.containsKey("fresh@example.com"));
    }

    @Test
    void testCircuitBreakerStaysClosedAfterThreeFailures() throws Exception {
        Mockito.doThrow(new IOException("Connection refused")).when(mockHttpClient).send(any(), any());

        for (int i = 1; i <= 3; i++) {
            Assertions.assertFalse(injectedNotifyNLService.sendVerificationEmail(
                    new VerificationCode(), "cb-closed-" + i + "@example.com"));
        }

        // Circuit still closed: 4th request reaches httpClient and succeeds
        HttpResponse<String> mockResponse = Mockito.mock();
        Mockito.when(mockResponse.statusCode()).thenReturn(200);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        Assertions.assertTrue(injectedNotifyNLService.sendVerificationEmail(
                new VerificationCode(), "cb-closed-4@example.com"));
    }

    @Test
    void testCircuitBreakerOpensAfterFiveConsecutiveFailures() throws Exception {
        Mockito.doThrow(new IOException("Connection refused")).when(mockHttpClient).send(any(), any());

        // Requests 1-5: all fail — window full, 5/5 = 100% failure rate → circuit opens
        for (int i = 1; i <= 5; i++) {
            injectedNotifyNLService.sendVerificationEmail(
                    new VerificationCode(), "cb-open-" + i + "@example.com");
        }

        // Request 6: circuit is open — httpClient.send() must not be called
        injectedNotifyNLService.sendVerificationEmail(
                new VerificationCode(), "cb-open-6@example.com");

        // Total send() calls must still be 5 — the 6th request never reached httpClient
        Mockito.verify(mockHttpClient, Mockito.times(5)).send(any(), any());
    }

    @Test
    void testIsSuccessResponse() throws Exception {
        Method method = NotifyNLService.class.getDeclaredMethod("isSuccessResponse", java.net.http.HttpResponse.class);
        method.setAccessible(true);

        HttpResponse<String> response200 = Mockito.mock();
        Mockito.when(response200.statusCode()).thenReturn(200);
        Assertions.assertTrue((Boolean) method.invoke(notifyNLService, response200));

        HttpResponse<String> response201 = Mockito.mock();
        Mockito.when(response201.statusCode()).thenReturn(201);
        Assertions.assertTrue((Boolean) method.invoke(notifyNLService, response201));

        HttpResponse<String> response400 = Mockito.mock();
        Mockito.when(response400.statusCode()).thenReturn(400);
        Assertions.assertFalse((Boolean) method.invoke(notifyNLService, response400));
    }
}
