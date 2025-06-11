package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import java.util.HashMap;
import java.util.Map;

public class TestDynamo {

    public static void main(String[] args) {

        System.out.println("Dynamo Uploader Tester: ");
        System.out.println(MetricsCollector.getInstance());

        String gameName = "TestGame";
        long timestamp = System.currentTimeMillis();  // Using current time as a timestamp
        long executionDuration = 1500; // Dummy execution duration (in milliseconds)
        Map<String, String> inputParams = new HashMap<>();
        inputParams.put("x", "10");
        inputParams.put("y", "43");

        MetricsCollector.getInstance().addMetric("GameName", gameName);
        MetricsCollector.getInstance().addMetric("Timestamp", timestamp);
        MetricsCollector.getInstance().addMetric("ExecutionDuration", executionDuration);
        MetricsCollector.getInstance().addMetric("InputParams", inputParams);

        System.out.println("DEBUG1");
        System.out.println(MetricsCollector.getInstance());

        try {
            DynamoUploader.accumulateItem();

            System.out.println("Dummy values uploaded successfully!");

        } catch (Exception e) {
            System.err.println("Failed to upload dummy values to DynamoDB.");
            e.printStackTrace();
        }
    }
}
