package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamoDbUtils {

    private static final String TABLE_NAME = "MetricStorageSystem";

    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .region(Region.EU_WEST_3)
            .build();

    public static void logExecutionMetrics(String workerIp,
                                           long durationMs,
                                           long instructionsExecuted,
                                           long methodsExecuted) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("RequestId", AttributeValue.fromS(UUID.randomUUID().toString()));
        item.put("WorkerIP", AttributeValue.fromS(workerIp));
        item.put("ExecutionDurationMs", AttributeValue.fromN(Long.toString(durationMs)));
        item.put("InstructionsExecuted", AttributeValue.fromN(Long.toString(instructionsExecuted)));
        item.put("MethodsExecuted", AttributeValue.fromN(Long.toString(methodsExecuted)));
        item.put("Timestamp", AttributeValue.fromN(Long.toString(System.currentTimeMillis())));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDb.putItem(request);
    }


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

