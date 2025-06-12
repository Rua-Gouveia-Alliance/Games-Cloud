package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDynamo {

    public static void main(String[] args) {

        System.out.println("Dynamo Uploader Tester: ");
        System.out.println(MetricsCollector.getInstance());

        String gameName = "TestGame";
        long timestamp = System.currentTimeMillis();  // Using current time as a timestamp
        long executionDuration = 1500; // Dummy execution duration (in milliseconds)
        Map<String, Integer> inputParams = new HashMap<>();
        inputParams.put("arg0", 10);
        inputParams.put("arg1", 43);

        MetricsCollector.getInstance().addMetric(new Metric("FifteenPuzzleHandler", timestamp,
                inputParams, 20000, 40000, 100000));

        System.out.println("DEBUG1");
        System.out.println(MetricsCollector.getInstance());

        try {
            DynamoUploader.accumulateItem();

            System.out.println("Dummy values uploaded successfully!");

        } catch (Exception e) {
            System.err.println("Failed to upload dummy values to DynamoDB.");
            e.printStackTrace();
        }

        try {
            Map<String, Integer> params = new HashMap<>();
            params.put("arg0", 10);
            params.put("arg1", 16);
            List<Object> retrieved = DynamoRetriever.retrieveRecentClosestMetric("FifteenPuzzleHandler", params, 5);
            List<Map<String, Object>> retrievedAll = DynamoRetriever.retrieveMetrics("FifteenPuzzleHandler");

            System.out.println("Metrics retrieved successfully!");
            System.out.println("Get(0): " + retrieved.get(0));
            System.out.println("Get(1): " + retrieved.get(1));

            System.out.println("All Metrics " + retrievedAll);
        } catch (Exception e) {
            System.err.println("Failed to retrieve values from DynamoDB.");
            e.printStackTrace();
        }
    }
}
