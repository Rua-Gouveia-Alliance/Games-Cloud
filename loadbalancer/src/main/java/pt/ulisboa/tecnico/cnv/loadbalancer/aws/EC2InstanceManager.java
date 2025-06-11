package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;

public class EC2InstanceManager {

    // TODO: import from config.sh?
    private static final String REGION = "us-east-1";
    private static final String AMI_ID = "ami-045df467fe4d245ab";
    private static final String KEY_NAME = "mykeypair";
    private static final String SEC_GROUP_ID = "sg-bacf59c2";

    public static String createInstance() {
        // TODO configure instance

        Region region = Region.of(REGION);

        try (Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

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

            // Wait for instance to reach 'running' state
            boolean ready = false;
            while (!ready) {
                DescribeInstancesRequest descReq = DescribeInstancesRequest.builder()
                        .instanceIds(instanceId)
                        .build();

                DescribeInstancesResponse descRes = ec2.describeInstances(descReq);
                InstanceStateName state = descRes.reservations().get(0).instances().get(0).state().name();

                if (state == InstanceStateName.RUNNING) {
                    ready = true;
                } else {
                    Thread.sleep(5000); // wait 5 seconds before checking again
                }
            }

            return instanceId;
        } catch (Ec2Exception e) {
            System.err.println("EC2 error: " + e.awsErrorDetails().errorMessage());
            return "";
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void terminateInstance(String instanceId) {
        Region region = Region.of(REGION);

        try (Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

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

    public static String getInstanceIP(String instanceId) {
        Region region = Region.of(REGION);

        try (Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            DescribeInstancesResponse response = ec2.describeInstances(request);

            List<Reservation> reservations = response.reservations();
            if (reservations.isEmpty()) {
                System.err.println("No reservations found for instance ID: " + instanceId);
                return null;
            }

            List<Instance> instances = reservations.get(0).instances();
            if (instances.isEmpty()) {
                System.err.println("No instance details found for instance ID: " + instanceId);
                return null;
            }

            Instance instance = instances.get(0);
            String ip = instance.publicIpAddress(); // use privateIpAddress() if needed
            System.out.println("Instance " + instanceId + " IP: " + ip);
            return ip;

        } catch (Ec2Exception e) {
            System.err.println("EC2 error: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }
}
