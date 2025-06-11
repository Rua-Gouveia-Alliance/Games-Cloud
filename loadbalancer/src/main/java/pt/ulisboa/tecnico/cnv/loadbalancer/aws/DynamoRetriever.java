package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

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

    public static Map<String, Object> retrieveRecentClosestMetric(
            String gameName, Map<String, Number> inputParams, int limit) {
        Map<String, Object> closestMetric = null;
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
                Map<String, Object> metrics = new HashMap<>();
                Map<String, AttributeValue> storedInputParams = null;
                for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                    String key = entry.getKey();
                    AttributeValue value = entry.getValue();

                    if (value.s() != null) {
                        metrics.put(key, value.s());
                    } else if (value.n() != null) {
                        try {
                            metrics.put(key, value.n().contains(".")
                                    ? Double.parseDouble(value.n())
                                    : Long.parseLong(value.n()));
                        } catch (NumberFormatException e) {
                            metrics.put(key, value.n());
                        }
                    } else if (value.m() != null && key.equals("InputParameters")) {
                        storedInputParams = value.m();
                        Map<String, Number> parsedParams = new HashMap<>();
                        for (Map.Entry<String, AttributeValue> param : storedInputParams.entrySet()) {
                            if (param.getValue() == null || param.getValue().n() == null) continue;
                            parsedParams.put(param.getKey(),
                                    param.getValue().n().contains(".")
                                            ? Double.parseDouble(param.getValue().n())
                                            : Long.parseLong(param.getValue().n()));
                        }
                        metrics.put("InputParameters", parsedParams);
                    }
                }

                if (storedInputParams != null) {
                    double distance = computeEuclideanDistance(inputParams, storedInputParams);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestMetric = metrics;
                    }
                }
            }

        } catch (DynamoDbException e) {
            System.err.println("Error querying DynamoDB: " + e.getMessage());
            e.printStackTrace();
        }

        return closestMetric;
    }

    private static double computeEuclideanDistance(Map<String, Number> input, Map<String, AttributeValue> stored) {
        double sum = 0.0;
        for (String key : input.keySet()) {
            if (!stored.containsKey(key)) continue;

            double inputVal = input.get(key).doubleValue();
            double storedVal = Double.parseDouble(stored.get(key).n());
            sum += Math.pow(inputVal - storedVal, 2);
        }
        return Math.sqrt(sum);
    }
    
}
