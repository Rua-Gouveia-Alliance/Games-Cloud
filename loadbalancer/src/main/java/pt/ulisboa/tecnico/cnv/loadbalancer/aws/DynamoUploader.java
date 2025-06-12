package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import pt.ulisboa.tecnico.cnv.loadbalancer.util.ConcurrentQueue;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.*;
import java.util.Map.Entry;

// TODO: Buffer timeout
public class DynamoUploader {

    private static final DynamoDbClient client = DynamoDbClient.create();
    private static final ConcurrentQueue<Map<String, AttributeValue>> itemsBuffer =
            new ConcurrentQueue<>();
    private static final int BUFFER_LIMIT = 5;
    private static int itemsToSend = 0;

    // Called at the end of an instance execution, with the thread-specific MetricsCollector
    // simpleton mapping its metrics to this global environment class
    public static void accumulateItem() {
        if (itemsToSend >= BUFFER_LIMIT) {
            System.out
                    .println("Buffer limit reached. Uploading Metrics and accumulating new item.");
            uploadMetrics();
        }

        List<Metric> metrics = MetricsCollector.getInstance().getAllMetrics();
        MetricsCollector.getInstance().clearMetrics();
        for (Metric m : metrics) {
            itemsBuffer.push(metricToMap(m));
            itemsToSend++;
        }
    }

    private static Map<String, AttributeValue> metricToMap(Metric m) {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("GameName", AttributeValue.builder().s(m.gameName()).build());
        itemMap.put("Timestamp", AttributeValue.builder().n(Long.toString(m.timestamp())).build());
        itemMap.put("Methods", AttributeValue.builder().n(Long.toString(m.methods())).build());
        itemMap.put("BasicBlocks",
                AttributeValue.builder().n(Long.toString(m.basicBlocks())).build());
        itemMap.put("Instructions",
                AttributeValue.builder().n(Long.toString(m.instructions())).build());

        Map<String, AttributeValue> params = new HashMap<>();
        for (Entry<String, Integer> e : m.params().entrySet()) {
            params.put(e.getKey(),
                    AttributeValue.builder().n(Integer.toString(e.getValue())).build());
        }

        itemMap.put("Params", AttributeValue.builder().m(params).build());
        return itemMap;
    }

    public static void uploadMetrics() {
        List<WriteRequest> writeRequests = new ArrayList<>();

        while (!itemsBuffer.isEmpty() && writeRequests.size() < BUFFER_LIMIT) {
            Map<String, AttributeValue> item = itemsBuffer.pop();
            writeRequests.add(WriteRequest.builder().putRequest(pr -> pr.item(item)).build());
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
