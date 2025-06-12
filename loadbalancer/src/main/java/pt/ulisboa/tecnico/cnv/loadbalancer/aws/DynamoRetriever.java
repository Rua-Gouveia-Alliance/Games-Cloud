package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoRetriever {

    private static final DynamoDbClient client = DynamoDbClient.create();

    public static List<Map<String, Object>> retrieveMetrics(String gameName) {
        List<Map<String, Object>> allMetrics = new ArrayList<>();

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName("MetricStorageSystem")
                .keyConditionExpression("GameName = :gameName")
                .expressionAttributeValues(Map.of(":gameName", AttributeValue.fromS(gameName)))
                .build();
        try {
            QueryResponse queryResponse = client.query(queryRequest);
            for (Map<String, AttributeValue> item : queryResponse.items()) {
                Map<String, Object> metrics = new HashMap<>();
                for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                    String key = entry.getKey();
                    AttributeValue value = entry.getValue();

                    if (value.s() != null) {
                        metrics.put(key, value.s());
                    } else if (value.n() != null) {
                        metrics.put(key, Long.parseLong(value.n()));
                    } else if (value.m() != null) {
                        metrics.put(key, value.m());
                    }
                }
                allMetrics.add(metrics);
            }
        } catch (DynamoDbException e) {
            System.err.println("Error querying DynamoDB: " + e.getMessage());
            e.printStackTrace();
        }
        return allMetrics;
    }

    public static ArrayList<Object> retrieveRecentClosestMetric(
            String gameName, Map<String, Integer> inputParams, int limit) {
        Map<String, Long> closestMetric = null;
        Map<String, Integer> closestParams = null;
        double minDistance = Double.MAX_VALUE;

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName("MetricStorageSystem")
                .keyConditionExpression("GameName = :gameName")
                .expressionAttributeValues(Map.of(":gameName", AttributeValue.fromS(gameName)))
                .scanIndexForward(false)
                .limit(limit)
                .build();
        try {
            QueryResponse queryResponse = client.query(queryRequest);
            for (Map<String, AttributeValue> item : queryResponse.items()) {
                Map<String, Long> metrics = new HashMap<>();
                Map<String, AttributeValue> storedInputParams = null;
                Map<String, Integer> parsedParams = new HashMap<>();

                for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                    String key = entry.getKey();
                    AttributeValue value = entry.getValue();

                    if (value.n() != null) {
                        try {
                            metrics.put(key, Long.parseLong(value.n()));
                        } catch (NumberFormatException e) {
                            metrics.put(key, (long) 0);
                        }
                    } else if (value.m() != null && key.equals("InputParameters")) {
                        storedInputParams = value.m();
                        for (Map.Entry<String, AttributeValue> param : storedInputParams.entrySet()) {
                            if (param.getValue() == null || param.getValue().n() == null) continue;
                            parsedParams.put(param.getKey(), Integer.parseInt(param.getValue().n()));
                        }
                    }
                }

                if (storedInputParams != null) {
                    double distance = computeEuclideanDistanceMaps(inputParams, parsedParams);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestMetric = metrics;
                        closestParams = parsedParams;
                    }
                }
            }

        } catch (DynamoDbException e) {
            System.err.println("Error querying DynamoDB: " + e.getMessage());
            e.printStackTrace();
        }

        ArrayList<Object> closestMetrics = new ArrayList<>();
        closestMetrics.add(closestMetric);
        closestMetrics.add(closestParams);
        return closestMetrics;
    }

    public static double computeEuclideanDistanceMaps(Map<String, Integer> input, Map<String, Integer> stored) {
        double sum = 0.0;
        for (String key : input.keySet()) {
            if (!stored.containsKey(key)) continue;

            double inputVal = input.get(key);
            double storedVal = stored.get(key);
            sum += Math.pow(inputVal - storedVal, 2);
        }
        return Math.sqrt(sum);
    }

}
