package pt.ulisboa.tecnico.cnv.loadbalancer.aws;


import pt.ulisboa.tecnico.cnv.loadbalancer.util.ConcurrentQueue;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.*;

public class DynamoUploader {

    // TODO: Find region from environment?
    private static final DynamoDbClient client = DynamoDbClient.create();
    private static final ConcurrentQueue<AttributeValue> itemsBuffer = new ConcurrentQueue<>();
    private static final int BUFFER_LIMIT = 5;
    private static int itemsToSend = 0;

    // Called at the end of an instance execution, with the thread-specific MetricsCollector
    // simpleton mapping its
    // metrics to this global environment class
    // TODO: Should this be synchronized?
    public synchronized static void accumulateItems() {
        if (itemsToSend >= BUFFER_LIMIT) {
            System.out
                    .println("Buffer limit reached. Uploading Metrics and accumulating new item.");
            uploadMetrics();
        }

        List<Metric> metrics = MetricsCollector.getInstance().getAllMetrics();
        MetricsCollector.getInstance().clearMetrics();
        for (Metric m : metrics) {
            itemsBuffer.push(metricToValue(m));
            itemsToSend++;
        }
    }

    private static AttributeValue metricToValue(Metric m) {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("gameName", AttributeValue.builder().s(m.gameName()).build());
        itemMap.put("timestamp", AttributeValue.builder().s(m.timestamp()).build());
        itemMap.put("params", AttributeValue.builder().s(m.params()).build());
        itemMap.put("methods", AttributeValue.builder().n(Long.toString(m.methods())).build());
        itemMap.put("basicBlocks",
                AttributeValue.builder().n(Long.toString(m.basicBlocks())).build());
        itemMap.put("instructions",
                AttributeValue.builder().n(Long.toString(m.instructions())).build());
        return AttributeValue.builder().m(itemMap).build();
    }

    public static void uploadMetrics() {
        List<WriteRequest> writeRequests = new ArrayList<>();

        while (!itemsBuffer.isEmpty() && writeRequests.size() < BUFFER_LIMIT) {
            AttributeValue item = itemsBuffer.pop();
            // TODO
            // writeRequests.add(WriteRequest.builder().putRequest(pr -> pr.item(item)).build());
            System.out.printf("[Uploader] Added to BatchWriteRequest: " + item.toString());
        }

        if (!writeRequests.isEmpty()) {
            BatchWriteItemRequest batchWriteRequest = BatchWriteItemRequest.builder()
                    .requestItems(Map.of("MetricStorageSystem", writeRequests)).build();
            client.batchWriteItem(batchWriteRequest);
            System.out.println("[Uploader] Stored metrics batch in DynamoDB table.");
            itemsToSend = 0;
        }
    }
}
