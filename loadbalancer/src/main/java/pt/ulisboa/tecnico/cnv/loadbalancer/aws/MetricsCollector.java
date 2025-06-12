package pt.ulisboa.tecnico.cnv.loadbalancer.aws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MetricsCollector {

    private final List<Metric> metrics = Collections.synchronizedList(new ArrayList<>());

    private static final ThreadLocal<MetricsCollector> instance =
            ThreadLocal.withInitial(MetricsCollector::new);

    public static MetricsCollector getInstance() {
        return instance.get();
    }

    public void addMetric(Metric metric) {
        metrics.add(metric);
        System.out.print("[MetricsCollector] Metric logged: " + metric);
    }

    public List<Metric> getAllMetrics() {
        return new ArrayList<>(metrics);
    }

    public void clearMetrics() {
        metrics.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MetricsCollector { ");
        synchronized (metrics) {
            Iterator<Metric> i = metrics.iterator();
            while (i.hasNext()) {
                sb.append(i.next());
            }
            if (!metrics.isEmpty()) {
                sb.setLength(sb.length() - 2); // Remove the trailing ", "
            }
        }
        sb.append(" }");
        return sb.toString();
    }
}
