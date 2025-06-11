package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

// Pega numa request ou request.type e parametros
// DBLoadAll(req.type, 10)
// Distância Euclidiana dos parâmetros
// Encontrar a mais próxima

public class DynamoRetriever {

    private static final DynamoDbClient client = DynamoDbClient.create();

    public static Map<String, Object> retrieveMetrics(String gameName, Integer last) {
        Map<String, Object> metrics = new HashMap<>();

        // Prepare the query request
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName("MetricStorageSystem")
                .keyConditionExpression("GameName = :gameName")
                .expressionAttributeValues(Map.of(":gameName", AttributeValue.fromS(gameName)))
                .build();

        try {
            // Query the DynamoDB table
            QueryResponse queryResponse = client.query(queryRequest);

            // Process the results
            for (Map<String, AttributeValue> item : queryResponse.items()) {
                // Convert the DynamoDB item back into the corresponding metrics
                String timestamp = item.get("Timestamp").s(); // Assuming Timestamp is a string

                // Assuming you store metrics in a key-value manner, you would process each attribute
                for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                    String key = entry.getKey();
                    AttributeValue value = entry.getValue();

                    // Add the metric to the result map
                    if (value.s() != null) {
                        metrics.put(key, value.s());
                    } else if (value.n() != null) {
                        metrics.put(key, Integer.parseInt(value.n()));
                    } else if (value.m() != null) {
                        metrics.put(key, value.m());  // Handling nested maps if any
                    }
                }
            }

        } catch (DynamoDbException e) {
            System.err.println("Error querying DynamoDB: " + e.getMessage());
            e.printStackTrace();
        }

        return metrics;
    }

    // Optionally, you could implement a method to retrieve metrics for a given Timestamp range or other filters
    public static Map<String, Object> retrieveMetricsByTimestampRange(String gameName, String startTimestamp, String endTimestamp) {
        Map<String, Object> metrics = new HashMap<>();

        // Prepare the query request with timestamp range condition
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName("MetricStorageSystem")
                .keyConditionExpression("GameName = :gameName and Timestamp between :startTimestamp and :endTimestamp")
                .expressionAttributeValues(Map.of(
                        ":gameName", AttributeValue.fromS(gameName),
                        ":startTimestamp", AttributeValue.fromS(startTimestamp),
                        ":endTimestamp", AttributeValue.fromS(endTimestamp)))
                .build();

        try {
            // Query the DynamoDB table with the timestamp range
            QueryResponse queryResponse = client.query(queryRequest);

            // Process the results
            for (Map<String, AttributeValue> item : queryResponse.items()) {
                // Convert the DynamoDB item back into the corresponding metrics
                for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                    String key = entry.getKey();
                    AttributeValue value = entry.getValue();

                    // Add the metric to the result map
                    if (value.s() != null) {
                        metrics.put(key, value.s());
                    } else if (value.n() != null) {
                        metrics.put(key, Integer.parseInt(value.n()));
                    } else if (value.m() != null) {
                        metrics.put(key, value.m());  // Handling nested maps if any
                    }
                }
            }

        } catch (DynamoDbException e) {
            System.err.println("Error querying DynamoDB: " + e.getMessage());
            e.printStackTrace();
        }

        return metrics;
    }
}
