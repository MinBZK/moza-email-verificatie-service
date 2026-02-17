package nl.rijksoverheid.moz;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import nl.rijksoverheid.moz.dto.request.VerificationApplicationRequest;
import nl.rijksoverheid.moz.dto.request.VerificationRequest;
import nl.rijksoverheid.moz.entity.VerificationCode;
import nl.rijksoverheid.moz.entity.VerificationStatistics;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import nl.rijksoverheid.moz.controller.VerificationController;
import nl.rijksoverheid.moz.job.VerificationCleanupJob;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class VerificationControllerTest {


    private static final int VERIFICATION_CODE_VALIDITY_MINUTES = 10;

    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void testAddServiceProviderEndpoint() {
        VerificationApplicationRequest request = new VerificationApplicationRequest();
        request.setEmail("test@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        assertNotNull(referenceId);
        assertFalse(referenceId.isEmpty());

        VerificationCode code = VerificationCode.find("email", "test@example.com").firstResult();
        assertNotNull(code);
        assertEquals(referenceId, code.getReferenceId());
    }

    @Test
    void testVerifySuccess() {
        // Create request via the API
        VerificationApplicationRequest appRequest = new VerificationApplicationRequest();
        appRequest.setEmail("success@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(appRequest)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        VerificationCode code = VerificationCode.find("referenceId", referenceId).firstResult();
        assertNotNull(code);

        VerificationRequest request = new VerificationRequest();
        request.setEmail("success@example.com");
        request.setReferenceId(referenceId);
        request.setCode(code.getCode());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(200);
    }

    @Test
    void testVerifyNotFound() {
        VerificationRequest request = new VerificationRequest();
        request.setEmail("nonexistent@example.com");
        request.setReferenceId("invalid-ref-id");
        request.setCode("123456");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(404);
    }

    @Test
    void testVerifyExpired() {
        // Create request via the API
        VerificationApplicationRequest appRequest = new VerificationApplicationRequest();
        appRequest.setEmail("expired@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(appRequest)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        expireCode(referenceId);

        VerificationCode code = VerificationCode.find("referenceId", referenceId).firstResult();
        VerificationRequest request = new VerificationRequest();
        request.setEmail("expired@example.com");
        request.setReferenceId(referenceId);
        request.setCode(code.getCode());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(410);
    }

    @Transactional
    void expireCode(String referenceId) {
        VerificationCode.update("validUntil = ?1 where referenceId = ?2",
            LocalDateTime.now().minusMinutes(VERIFICATION_CODE_VALIDITY_MINUTES), referenceId);
    }
    @Test
    void testVerifyAlreadyUsed() {
        // Create request via the API
        VerificationApplicationRequest appRequest = new VerificationApplicationRequest();
        appRequest.setEmail("used@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(appRequest)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        VerificationCode code = VerificationCode.find("referenceId", referenceId).firstResult();

        // First verification - should succeed
        VerificationRequest request = new VerificationRequest();
        request.setEmail("used@example.com");
        request.setReferenceId(referenceId);
        request.setCode(code.getCode());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(200);

        // Second verification - should fail with 409
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(409);
    }

    @Test
    void testVerifyWrongCode() {
        // Create request via the API
        VerificationApplicationRequest appRequest = new VerificationApplicationRequest();
        appRequest.setEmail("wrongcode@example.com");

        String referenceId = given()
                .contentType(ContentType.JSON)
                .body(appRequest)
                .when().post("/request")
                .then()
                .statusCode(200)
                .extract().asString();

        VerificationRequest request = new VerificationRequest();
        request.setEmail("wrongcode@example.com");
        request.setReferenceId(referenceId);
        request.setCode("000000"); // Wrong code

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/verify")
                .then()
                .statusCode(401);
    }
    @Inject
    VerificationCleanupJob cleanupJob;

    @Test
    @TestTransaction
    void testCleanUpSuccessfulVerifications() {
        VerificationCode code = new VerificationCode("cleanup-success@example.com");
        code.setVerifiedAt(LocalDateTime.now().minusMinutes(1));
        code.persist();

        cleanupJob.cleanUpSuccessfulVerifications();

        // Check that code is deleted
        assertNull(VerificationCode.findById(code.id));

        // Check that statistics are created
        List<VerificationStatistics> stats = VerificationStatistics.listAll();
        assertTrue(stats.stream().anyMatch(s -> s.getVerifiedAt() != null));
    }

    @Test
    @TestTransaction
    void testCleanUpExpiredCodes() {
        VerificationCode code = new VerificationCode("cleanup-expired@example.com");
        code.setValidUntil(LocalDateTime.now().minusMinutes(1));
        code.persist();

        cleanupJob.cleanUpExpiredCodes();

        // Check that code is deleted
        assertNull(VerificationCode.findById(code.id));

        // Check that statistics are created (verifiedAt should be null)
        List<VerificationStatistics> stats = VerificationStatistics.listAll();
        assertTrue(stats.stream().anyMatch(s -> s.getVerifiedAt() == null));
    }
}