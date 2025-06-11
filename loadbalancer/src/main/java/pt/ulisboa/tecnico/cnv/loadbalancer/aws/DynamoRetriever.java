package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

public class DynamoRetriever {

    //TODO: Find region from environment?
    private static final DynamoDbClient client = DynamoDbClient.create();

    public static List<Map<String, AttributeValue>> getExecutionsByClass(String className) {
        QueryRequest request = QueryRequest.builder()
                .tableName("MetricStorageSystem")
                .keyConditionExpression("ClassName = :v_class")
                .expressionAttributeValues(Map.of(":v_class", AttributeValue.fromS(className)))
                .scanIndexForward(true) // true = ascending timestamp
                .build();

        QueryResponse response = client.query(request);
        return response.items();
    }

    public static List<Map<String, AttributeValue>> getExecutionsByClassAndTimeRange(String className, long startTs, long endTs) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":v_class", AttributeValue.fromS(className));
        expressionValues.put(":start", AttributeValue.fromN(Long.toString(startTs)));
        expressionValues.put(":end", AttributeValue.fromN(Long.toString(endTs)));

        QueryRequest request = QueryRequest.builder()
                .tableName("MetricStorageSystem")
                .keyConditionExpression("ClassName = :v_class AND Timestamp BETWEEN :start AND :end")
                .expressionAttributeValues(expressionValues)
                .build();

        QueryResponse response = client.query(request);
        return response.items();
    }

    public static void printMetrics(List<Map<String, AttributeValue>> items) {
        for (Map<String, AttributeValue> item : items) {
            System.out.println("-----");
            System.out.println("ClassName: " + item.get("ClassName").s());
            System.out.println("Timestamp: " + item.get("Timestamp").n());
            System.out.println("MethodsExecuted: " + item.getOrDefault("MethodsExecuted", AttributeValue.fromN("0")).n());
            System.out.println("Status: " + item.getOrDefault("Status", AttributeValue.fromS("UNKNOWN")).s());
            System.out.println("Duration: " + item.getOrDefault("Duration", AttributeValue.fromN("0")).n());
            // optionally print InputParameters as raw JSON
            if (item.containsKey("InputParameters")) {
                System.out.println("InputParameters: " + item.get("InputParameters").m());
            }
        }
    }
}

