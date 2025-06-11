package pt.ulisboa.tecnico.cnv.loadbalancer.aws;


import pt.ulisboa.tecnico.cnv.loadbalancer.util.ConcurrentQueue;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.*;

public class DynamoUploader {

    //TODO: Find region from environment?
    private static final DynamoDbClient client = DynamoDbClient.create();
    private static final ConcurrentQueue<Map<String, AttributeValue>> itemsBuffer = new ConcurrentQueue<>();
    private static final int BUFFER_LIMIT = 5;
    private static int itemsToSend = 0;

    // Called at the end of an instance execution, with the thread-specific MetricsCollector simpleton mapping its
    // metrics to this global environment class
    // TODO: Should this be synchronized?
    public synchronized static void accumulateItem() {
        if (itemsToSend >= BUFFER_LIMIT) {
            System.out.println("Buffer limit reached. Uploading Metrics and accumulating new item.");
            uploadMetrics();
        }
        itemsBuffer.push(mapMetricsToItem());
        itemsToSend++;
    }

    // TODO: Should this be synchronized?
    public static Map<String, AttributeValue> mapMetricsToItem(Map<String, Object> metrics) {

        Map<String, AttributeValue> item = new HashMap<>();
        Set<Map<String, Object>> visited = new HashSet<>();

        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            String key = entry.getKey();
            Object metric = entry.getValue();
            AttributeValue attributeValue = null;

            // Check if the metric is a nested Map
            if (metric instanceof Map) {
                if (visited.contains(metric)) {
                    throw new IllegalStateException("Circular reference detected for key: " + key);
                }
                visited.add((Map<String, Object>) metric);
                attributeValue = AttributeValue.builder()
                        .m(mapMetricsToItem((Map<String, Object>) metric))
                        .build();
            } else if (metric instanceof String) {
                attributeValue = AttributeValue.builder().s(metric.toString()).build();
            } else if (metric instanceof Long) {
                attributeValue = AttributeValue.builder().n(Long.toString((Long) metric)).build();
            } else {
                // Handle other types, if any (you may want to throw an exception or log)
                throw new IllegalArgumentException("Invalid metric type: " + metric.getClass().getName());
            }
            if (attributeValue != null) {
                item.put(key, attributeValue);
            }
        };
        System.out.printf("[Uploader] Sent to dynamo buffer: " + MetricsCollector.getInstance());
        MetricsCollector.getInstance().clearMetrics();
        return item;
    }

    public static Map<String, AttributeValue> mapMetricsToItem() {
        return mapMetricsToItem(MetricsCollector.getInstance().getAllMetrics());
    }

    public static void uploadMetrics() {
        List<WriteRequest> writeRequests = new ArrayList<>();

        while (!itemsBuffer.isEmpty() && writeRequests.size() < BUFFER_LIMIT) {
            Map<String, AttributeValue> item = itemsBuffer.pop();
            writeRequests.add(WriteRequest.builder()
                    .putRequest(pr -> pr.item(item))
                    .build());
            System.out.printf("[Uploader] Added to BatchWriteRequest: " + item.toString());
        }

        if (!writeRequests.isEmpty()) {
            BatchWriteItemRequest batchWriteRequest = BatchWriteItemRequest.builder()
                    .requestItems(Map.of("MetricStorageSystem", writeRequests))
                    .build();

            client.batchWriteItem(batchWriteRequest);
            System.out.println("[Uploader] Stored metrics batch in DynamoDB table.");

            itemsToSend = 0;
        }
    }
}
