package pt.ulisboa.tecnico.cnv.loadbalancer.aws;


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

public class DynamoUploader {

    //TODO: Find region from environment?
    private static final DynamoDbClient client = DynamoDbClient.create();

    public static void uploadMetrics() {

        Map<String, AttributeValue> item = new HashMap<>();
        // Iterate over metrics and add them to the item
        MetricsCollector.getInstance().getAllMetrics().forEach((key, metric) -> {
            if (metric instanceof String) {
                item.put(key, AttributeValue.fromS((String) metric));
            } else if (metric instanceof Number) {
                item.put(key, AttributeValue.fromN(metric.toString()));
            } else if (metric instanceof Map) {
                item.put(key, AttributeValue.fromM((Map<String, AttributeValue>) metric));
            } else {
                item.put(key, AttributeValue.fromS("Unknown Type"));
            }
        });

        client.putItem(PutItemRequest.builder()
                .tableName("MetricStorageSystem")
                .item(item)
                .build());

        System.out.printf("[Uploader] stored to table: " + MetricsCollector.getInstance());
        MetricsCollector.getInstance().clearMetrics();
    }
}
