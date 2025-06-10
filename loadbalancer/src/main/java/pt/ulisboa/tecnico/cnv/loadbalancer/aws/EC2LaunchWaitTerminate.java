package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;

public class EC2LaunchWaitTerminate {

    // TODO: import from config.sh?
    private static final String REGION = "us-east-1";
    private static final String AMI_ID = "ami-045df467fe4d245ab";
    private static final String KEY_NAME = "mykeypair";
    private static final String SEC_GROUP_ID = "sg-bacf59c2";

    // Time to wait before terminating (10 minutes)
    private static final long WAIT_TIME = 1000 * 60 * 10;

    public static void main(String[] args) throws InterruptedException {
        Region region = Region.of(REGION);

        try (Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

            // List Availability Zones
            DescribeAvailabilityZonesResponse zonesResponse = ec2.describeAvailabilityZones();
            System.out.println("You have access to " + zonesResponse.availabilityZones().size() + " Availability Zones.");

            // List running instances
            DescribeInstancesResponse beforeResponse = ec2.describeInstances();
            long totalInstances = beforeResponse.reservations().stream()
                    .mapToLong(r -> r.instances().size()).sum();
            System.out.println("You have " + totalInstances + " EC2 instance(s) running.");

            // Start a new instance
            RunInstancesRequest runRequest = RunInstancesRequest.builder()
                    .imageId(AMI_ID)
                    .instanceType(InstanceType.T2_MICRO)
                    .maxCount(1)
                    .minCount(1)
                    .keyName(KEY_NAME)
                    .securityGroupIds(SEC_GROUP_ID)
                    .build();

            RunInstancesResponse runResponse = ec2.runInstances(runRequest);
            String instanceId = runResponse.instances().get(0).instanceId();
            System.out.println("Launched EC2 instance with ID: " + instanceId);

            // Wait
            System.out.println("Waiting 10 minutes before terminating the instance...");
            Thread.sleep(WAIT_TIME);

            // Terminate instance
            TerminateInstancesRequest termRequest = TerminateInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();
            TerminateInstancesResponse termResponse = ec2.terminateInstances(termRequest);
            List<InstanceStateChange> termResults = termResponse.terminatingInstances();

            System.out.println("Instance terminated: " + termResults.get(0).instanceId());
        } catch (Ec2Exception e) {
            System.err.println("EC2 error: " + e.awsErrorDetails().errorMessage());
        }
    }
}
