package nl.rijksoverheid.moz.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import nl.rijksoverheid.moz.dto.response.AdminStatisticsResponse;
import nl.rijksoverheid.moz.entity.VerificationStatistics;

import java.time.Duration;
import java.util.List;

@Path("/admin/statistics")
public class AdminStatisticsController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AdminStatisticsResponse getAdminStatistics() {
        List<VerificationStatistics> allStats = VerificationStatistics.listAll();
        long totalCount = allStats.size();
        double unverifiedPercentage = totalCount > 0
                ? (allStats.stream()
                .filter(s -> s.getVerifiedAt() == null)
                .count() * 100.0) / totalCount
                : 0.0;

        double averageTimeSeconds = allStats.stream()
                .filter(s -> s.getVerifiedAt() != null && s.getVerifyEmailSentAt() != null)
                .mapToLong(s -> Duration.between(s.getVerifyEmailSentAt(), s.getVerifiedAt()).toSeconds())
                .average()
                .orElse(0.0);

        return new AdminStatisticsResponse(averageTimeSeconds, unverifiedPercentage);
    }
}
