package pt.ulisboa.tecnico.cnv.loadbalancer.util;
/*
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.regions.Region;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamoDbUtils {

    private static final String TABLE_NAME = "RequestMetrics";

    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .region(Region.EU_WEST_1) // Set your AWS region
            .build();

    public static void logRequestMetric(String workerIp, long durationMs) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("RequestId", AttributeValue.fromS(UUID.randomUUID().toString()));
        item.put("WorkerIP", AttributeValue.fromS(workerIp));
        item.put("DurationMs", AttributeValue.fromN(Long.toString(durationMs)));
        item.put("Timestamp", AttributeValue.fromN(Long.toString(System.currentTimeMillis())));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDb.putItem(request);
    }

    // Example: fetch recent metrics (optional helper)
    public static void printRecentMetrics() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .limit(10)
                .build();

        ScanResponse response = dynamoDb.scan(scanRequest);
        for (Map<String, AttributeValue> item : response.items()) {
            System.out.println(item);
        }
    }
}

*/
