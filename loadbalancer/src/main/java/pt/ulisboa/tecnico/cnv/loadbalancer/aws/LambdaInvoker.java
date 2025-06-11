package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import java.nio.charset.StandardCharsets;

public class LambdaInvoker {

    private static final String REGION = "eu-west-3";
    private static final String FUNCTION_NAME = "GamesCloudHandler"; // TODO deploy lambda with some script

    public static String invokeLambda(String payloadJson) {
        Region region = Region.of(REGION);

        try (LambdaClient lambdaClient = LambdaClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(FUNCTION_NAME)
                    .payload(SdkBytes.fromString(payloadJson, StandardCharsets.UTF_8))
                    .build();

            InvokeResponse response = lambdaClient.invoke(request);

            String result = response.payload().asUtf8String();
            System.out.println("Lambda returned: " + result);

            return result;
        } catch (LambdaException e) {
            System.err.println("Lambda error: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }
}
