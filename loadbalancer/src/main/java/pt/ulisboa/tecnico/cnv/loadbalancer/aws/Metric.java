package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Represents the metrics collected for a single request
public class Metric {
    private final String gameName; // Partition-Key
    private final String timestamp; // Sort-Key
    private final Map<String, Integer> params;
    private final Integer methods;
    private final Integer basicBlocks;
    private final Integer instructions;

    public Metric(String gameName, LocalDateTime timestamp, Map<String, Integer> params,
            int methods, int basicBlocks, int instructions) {
        this.gameName = gameName;
        this.params = params;
        this.methods = methods;
        this.basicBlocks = basicBlocks;
        this.instructions = instructions;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.timestamp = timestamp.format(formatter);
    }

    public String gameName() {
        return gameName;
    }

    public String timestamp() {
        return timestamp;
    }

    public Map<String, Integer> params() {
        return params;
    }

    public Integer methods() {
        return methods;
    }

    public Integer basicBlocks() {
        return basicBlocks;
    }

    public Integer instructions() {
        return instructions;
    }

    @Override
    public String toString() {
        return String.format(
                "Metric { gameName: %s, timestamp: %s, params: %s, methods: %s, basicBlocks: %s, instructions: %s }",
                gameName, timestamp, params, methods, basicBlocks, instructions);
    }
}
