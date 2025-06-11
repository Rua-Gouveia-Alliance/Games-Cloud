package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.List;

public class EC2Metrics {

    private static final Region AWS_REGION = Region.EU_WEST_3;
    private static final int PERIOD_SECONDS = 60; // granularity (1 min)
    private static final long WINDOW_MILLIS = 1000L * 60L * 5L; // 5 minutes

    public static double getAverageCpuUtilization(String instanceId) {
        try (CloudWatchClient cloudWatch = CloudWatchClient.builder()
                .region(AWS_REGION)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

            Dimension dimension = Dimension.builder()
                    .name("InstanceId")
                    .value(instanceId)
                    .build();

            Instant endTime = Instant.now();
            Instant startTime = endTime.minusMillis(WINDOW_MILLIS);

            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/EC2")
                    .metricName("CPUUtilization")
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(PERIOD_SECONDS)
                    .statistics(Statistic.AVERAGE)
                    .dimensions(dimension)
                    .build();

            GetMetricStatisticsResponse response = cloudWatch.getMetricStatistics(request);
            List<Datapoint> dataPoints = response.datapoints();

            if (dataPoints.isEmpty()) {
                System.out.printf("No CPU data for instance %s%n", instanceId);
                return -1.0;
            }

            // You can average multiple datapoints, but we'll take the latest one
            Datapoint latest = dataPoints.stream()
                    .max((a, b) -> a.timestamp().compareTo(b.timestamp()))
                    .orElse(dataPoints.get(0));

            return latest.average();

        } catch (CloudWatchException e) {
            System.err.println("CloudWatch error: " + e.awsErrorDetails().errorMessage());
            return -1.0;
        }
    }
}
