package nl.rijksoverheid.moz.dto.response;

public class AdminStatisticsResponse {
    private double averageVerificationTimeSeconds;
    private double unverifiedPercentage;

    public AdminStatisticsResponse() {}

    public AdminStatisticsResponse(double averageVerificationTimeSeconds, double unverifiedPercentage) {
        this.averageVerificationTimeSeconds = averageVerificationTimeSeconds;
        this.unverifiedPercentage = unverifiedPercentage;
    }

    public double getAverageVerificationTimeSeconds() {
        return averageVerificationTimeSeconds;
    }

    public void setAverageVerificationTimeSeconds(double averageVerificationTimeSeconds) {
        this.averageVerificationTimeSeconds = averageVerificationTimeSeconds;
    }

    public double getUnverifiedPercentage() {
        return unverifiedPercentage;
    }

    public void setUnverifiedPercentage(double unverifiedPercentage) {
        this.unverifiedPercentage = unverifiedPercentage;
    }
}
