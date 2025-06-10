package pt.ulisboa.tecnico.cnv.aws;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.*;

public class EC2MeasureCPU {

    // TODO: import from config.sh?
    private static final Region AWS_REGION = Region.US_EAST_1;
    private static final long OBS_TIME_MILLIS = 1000L * 60L * 20L; // 20 minutes

    public static void main(String[] args) {

        System.out.println("===========================================");
        System.out.println("AWS SDK v2 EC2 CPU Monitoring");
        System.out.println("===========================================");

        try (Ec2Client ec2 = Ec2Client.builder()
                .region(AWS_REGION)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

             CloudWatchClient cloudWatch = CloudWatchClient.builder()
                     .region(AWS_REGION)
                     .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                     .build()) {

            Set<Instance> instances = getInstances(ec2);
            System.out.println("Total instances = " + instances.size());

            for (Instance instance : instances) {
                String iid = instance.instanceId();
                String state = instance.state().nameAsString();

                if ("running".equalsIgnoreCase(state)) {
                    System.out.println("Running instance ID: " + iid);

                    Dimension dimension = Dimension.builder()
                            .name("InstanceId")
                            .value(iid)
                            .build();

                    Instant endTime = Instant.now();
                    Instant startTime = endTime.minusMillis(OBS_TIME_MILLIS);

                    GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                            .namespace("AWS/EC2")
                            .metricName("CPUUtilization")
                            .startTime(startTime)
                            .endTime(endTime)
                            .period(60)
                            .statistics(Statistic.AVERAGE)
                            .dimensions(dimension)
                            .build();

                    GetMetricStatisticsResponse response = cloudWatch.getMetricStatistics(request);
                    List<Datapoint> dataPoints = response.datapoints();

                    for (Datapoint dp : dataPoints) {
                        System.out.printf("CPU Utilization for instance %s = %.2f%%%n", iid, dp.average());
                    }

                } else {
                    System.out.printf("Instance %s is in state: %s%n", iid, state);
                }
            }

        } catch (Ec2Exception | CloudWatchException e) {
            System.err.println("AWS SDK Error: " + e.awsErrorDetails().errorMessage());
        }
    }

    private static Set<Instance> getInstances(Ec2Client ec2) {
        Set<Instance> instances = new HashSet<>();

        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2.describeInstances(request);

        for (Reservation reservation : response.reservations()) {
            instances.addAll(reservation.instances());
        }
        return instances;
    }
}
