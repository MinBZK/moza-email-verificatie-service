package nl.rijksoverheid.moz;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import nl.rijksoverheid.moz.entity.VerificationStatistics;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class AdminStatisticsControllerTest {

    @Test
    void testGetAdminStatistics() {
        createStats();
        
        given()
                .contentType(ContentType.JSON)
                .when().get("/admin/statistics")
                .then()
                .statusCode(200)
                .body("averageVerificationTimeSeconds", is(90.0f))
                .body("unverifiedPercentage", is(33.333332f));
    }

    @jakarta.transaction.Transactional
    void createStats() {
        VerificationStatistics.deleteAll();

        // Create a verified statistic
        VerificationStatistics stat1 = new VerificationStatistics();
        stat1.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        stat1.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(4));
        stat1.setVerifiedAt(LocalDateTime.now().minusMinutes(2)); // 2 minutes (120 seconds) verification time
        stat1.persist();

        // Create another verified statistic
        VerificationStatistics stat2 = new VerificationStatistics();
        stat2.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        stat2.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(9));
        stat2.setVerifiedAt(LocalDateTime.now().minusMinutes(8)); // 1 minute (60 seconds) verification time
        stat2.persist();

        // Average should be (120 + 60) / 2 = 90 seconds

        // Create an unverified statistic
        VerificationStatistics stat3 = new VerificationStatistics();
        stat3.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        stat3.setVerifyEmailSentAt(LocalDateTime.now().minusMinutes(19));
        stat3.setVerifiedAt(null);
        stat3.persist();
    }

    @Test
    void testGetAdminStatisticsEmpty() {
        clearStats();

        given()
                .contentType(ContentType.JSON)
                .when().get("/admin/statistics")
                .then()
                .statusCode(200)
                .body("averageVerificationTimeSeconds", is(0.0f))
                .body("unverifiedPercentage", is(0.0f));
    }

    @jakarta.transaction.Transactional
    void clearStats() {
        VerificationStatistics.deleteAll();
    }
}
