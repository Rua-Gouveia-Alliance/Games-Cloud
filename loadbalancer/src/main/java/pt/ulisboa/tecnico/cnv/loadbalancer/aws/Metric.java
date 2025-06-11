package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Represents the metrics collected for a single request
public class Metric {
    private final String gameName; // Partition-Key
    private final String timestamp; // Sort-Key
    private final String params;
    private final Long methods;
    private final Long basicBlocks;
    private final Long instructions;

    public Metric(String gameName, LocalDateTime timestamp, String params, Long methods,
            Long basicBlocks, Long instructions) {
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

    public String params() {
        return params;
    }

    public Long methods() {
        return methods;
    }

    public Long basicBlocks() {
        return basicBlocks;
    }

    public Long instructions() {
        return instructions;
    }

    @Override
    public String toString() {
        return String.format(
                "Metric { gameName: %s, timestamp: %s, params: %s, methods: %s, basicBlocks: %s, instructions: %s }",
                gameName, timestamp, params, methods, basicBlocks, instructions);
    }
}
