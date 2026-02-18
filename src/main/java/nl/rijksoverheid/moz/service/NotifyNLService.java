package nl.rijksoverheid.moz.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import nl.rijksoverheid.moz.entity.VerificationCode;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@ApplicationScoped
public class NotifyNLService {

    private static final Logger LOG = Logger.getLogger(NotifyNLService.class);
    private final HttpClient httpClient;

    @ConfigProperty(name = "notifynl.emailverificatie.url")
    String url;

    @ConfigProperty(name = "notifynl.emailverificatie.template-id")
    String templateId;

    @ConfigProperty(name = "notifynl.emailverificatie.api-key")
    String apiKey;

    public NotifyNLService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Calls NotifyNL service via HTTP POST.
     * Returns true if successful (200 OK or 201 Created), false otherwise.
     */
    public boolean sendVerificationEmail(VerificationCode code) {

        LOG.info("Calling NotifyNL service for email: " + code.getEmail());

        try {
            String jsonBody = String.format(
                    "{\"personalisation\":{\"code\":\"%s\"},\"template_id\":\"%s\",\"email_address\":\"%s\"}"
                    , code.getCode(), templateId, code.getEmail()
            );

            ApiKeyDetails keys = extractServiceIdAndApiKey(apiKey);
            String token = createToken(keys.secret(), keys.serviceId());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                LOG.info("NotifyNL service call successful for email: " + code.getEmail());
                return true;
            } else {
                LOG.error("NotifyNL service returned status code: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error calling NotifyNL service", e);
            return false;
        }
    }


    private record ApiKeyDetails(String serviceId, String secret) {}

    private static ApiKeyDetails extractServiceIdAndApiKey(String fromApiKey) {
        if (fromApiKey == null || fromApiKey.isBlank() || fromApiKey.contains(" ") || fromApiKey.length() < 74) {
            throw new IllegalArgumentException(
                    "The API Key provided is invalid. Please ensure you are using a v2 API Key that is not empty or null");
        }

        String serviceId = fromApiKey.substring(fromApiKey.length() - 73, fromApiKey.length() - 73 + 36);
        String secret = fromApiKey.substring(fromApiKey.length() - 36);

        return new ApiKeyDetails(serviceId, secret);
    }

    private static String createToken(String secret, String serviceId) {
        return Jwt.issuer(serviceId)
                .issuedAt(Instant.now())
                .signWithSecret(secret);
    }
}
