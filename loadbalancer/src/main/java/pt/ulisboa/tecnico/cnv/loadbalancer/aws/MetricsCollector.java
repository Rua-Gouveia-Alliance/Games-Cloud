package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MetricsCollector {

    // Which Metrics to keep track of
    public static final String GAME_NAME = "GameName";        // Partition-Key
    public static final String TIMESTAMP = "Timestamp";         // Sort-Key
    public static final String INPUT_PARAMS = "InputParameters";
    public static final String METHODS_EXECUTED = "MethodsExecuted";
    public static final String BASIC_BLOCKS = "BasicBlocksExecuted";
    public static final String INSTRUCTIONS = "InstructionsExecuted";

    private final Map<String, Object> metrics = new ConcurrentHashMap<>();

    private static final ThreadLocal<MetricsCollector> instance = ThreadLocal.withInitial(MetricsCollector::new);

    private MetricsCollector() {}

    public static MetricsCollector getInstance() {
        return instance.get();
    }

    public void addMetric(String key, Object value) {
        metrics.put(key, value);
    }

    public Map<String, Object> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    public void clearMetrics() {
        metrics.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MetricsCollector { ");
        metrics.forEach((key, value) -> sb.append(key).append("=").append(value).append(", "));
        if (!metrics.isEmpty()) {
            sb.setLength(sb.length() - 2);  // Remove the trailing ", "
        }
        sb.append(" }");
        return sb.toString();
    }
}

