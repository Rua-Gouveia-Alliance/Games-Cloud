package pt.ulisboa.tecnico.cnv.loadbalancer;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import pt.ulisboa.tecnico.cnv.loadbalancer.aws.EC2InstanceManager;
import pt.ulisboa.tecnico.cnv.loadbalancer.aws.EC2Metrics;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Autoscaler extends Thread {

    private static final int MIN_NUM_WORKERS = 1;
    private static final int MAX_NUM_WORKERS = 10;

    private final List<WorkerHandler> lbWorkers;
    private final LoadBalancer loadBalancer;

    public Autoscaler(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        this.lbWorkers = loadBalancer.workers;
    }

    static private class WorkerInfo {
        double price;
        double capacity;

        public WorkerInfo(double price, double capacity) {
            this.price = price;
            this.capacity = capacity;
        }
    }

    static private HashMap<String, WorkerInfo> workers = new HashMap<>() {
        {
            // Since we're only using a single type of VM, price can be set to anything
            put("VM", new WorkerInfo(1.0, 1.0));
        }
    };

    static private HashMap<String, BigDecimal> computeConfiguration(double lambda) {
        HashMap<String, AbstractMap.SimpleEntry<Integer, Variable>> variables = new HashMap<>();
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        int i = 0;
        for (Map.Entry<String, WorkerInfo> e : workers.entrySet()) {
            String name = e.getKey();
            WorkerInfo info = e.getValue();
            AbstractMap.SimpleEntry<Integer, Variable> entry = new AbstractMap.SimpleEntry<>(i++,
                    model.newVariable(name).integer(true).lower(0).weight(info.price));
            variables.put(name, entry);
        }

        // Constraints
        Expression capacity = model.addExpression("Capacity");
        for (Map.Entry<String, WorkerInfo> e : workers.entrySet()) {
            String name = e.getKey();
            WorkerInfo info = e.getValue();
            capacity.set(variables.get(name).getValue(), info.capacity);
        }
        capacity.lower(lambda);

        // Minimize cost
        Optimisation.Result result = model.minimise();

        HashMap<String, BigDecimal> configuration = new HashMap<>();
        for (Map.Entry<String, AbstractMap.SimpleEntry<Integer, Variable>> e : variables
                .entrySet()) {
            String name = e.getKey();
            AbstractMap.SimpleEntry<Integer, Variable> entry = e.getValue();
            BigDecimal count = result.get(entry.getKey().intValue());
            configuration.put(name, count);
        }
        System.out.println("[Autoscaler] Total cost: " + result.getValue());
        return configuration;
    }

    private long getCurrentLambda() {
        // Weights (TODO adjust these based on tuning and experiments)
        final double workloadWeight = 0.7;
        final double cpuWeight = 0.3;

        // Normalization constants
        final double maxCpuPercent = 100.0;

        long totalWorkload = 0;
        double totalCpu = 0.0;
        int count = 0;

        List<WorkerHandler> snapshot;
        synchronized (lbWorkers) {
            snapshot = new ArrayList<>(lbWorkers);
        }

        for (WorkerHandler worker : snapshot) {
            totalWorkload += worker.getCurrentWorkload();
        }

        for (WorkerHandler worker : snapshot) {
            double cpu = EC2Metrics.getAverageCpuUtilization(worker.instanceId);
            if (cpu >= 0) {
                totalCpu += cpu;
                count++;
            }
        }

        double avgCpu = count > 0 ? totalCpu / count : 0.0;

        // Normalize both to [0, 1] scale
        double normalizedWorkload = (double) totalWorkload / (snapshot.size() * LoadBalancer.MAX_WORKLOAD);
        double normalizedCpu = avgCpu / maxCpuPercent;

        // Compute weighted lambda in [0, 1] range
        double normalizedLambda = workloadWeight * normalizedWorkload + cpuWeight * normalizedCpu;

        long lambda = (long) normalizedLambda * MAX_NUM_WORKERS;

        System.out.printf("λ = workload=%.2f, cpu=%.2f → normalized λ=%.2f → final λ=%d%n",
                normalizedWorkload, normalizedCpu, normalizedLambda, lambda);

        return lambda;
    }

    public void scaleOut() {
        String instanceId = EC2InstanceManager.createInstance();
        if (instanceId.isEmpty())
            return;

        String ip = EC2InstanceManager.getInstanceIP(instanceId);
        try {
        EC2InstanceManager.waitUntilInstanceIsReady(ip);
        } catch (InterruptedException e) {
            System.out.println("[Autoscaler] scaleOut interrupted: " + e.toString());
        }
        WorkerHandler newHandler = new WorkerHandler(instanceId, ip, this.loadBalancer);
        newHandler.start();
        synchronized (lbWorkers) {
            lbWorkers.add(newHandler);
        }
    }

    public void scaleIn() {
        synchronized (lbWorkers) {
            for (WorkerHandler w : new ArrayList<>(lbWorkers)) {
                if (w.getCurrentRequest() == null && w.queue.isEmpty()) {
                    lbWorkers.remove(w);
                    EC2InstanceManager.terminateInstance(w.instanceId);
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            long lambda = getCurrentLambda();
            HashMap<String, BigDecimal> config = computeConfiguration(lambda);
            for (Map.Entry<String, BigDecimal> e : config.entrySet()) {
                System.out.println("[AutoScaler] " + e.getKey() + ": " + e.getValue());
            }

            System.out.println("[AutoScaler] Current worker size: " + workers.size());
            if (config.get("VM").compareTo(BigDecimal.valueOf(workers.size())) < 0 && lbWorkers.size() > MIN_NUM_WORKERS) {
                System.out.println("[AutoScaler] Going to scaleIn");
                scaleIn();
            } else if (config.get("VM").compareTo(BigDecimal.valueOf(workers.size())) > 0 && lbWorkers.size() < MAX_NUM_WORKERS) {
                System.out.println("[AutoScaler] Going to scaleOut");
                scaleOut();
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("[AutoScaler] Terminated unexpectedly: " + e.toString());
            }
        }
    }

}
