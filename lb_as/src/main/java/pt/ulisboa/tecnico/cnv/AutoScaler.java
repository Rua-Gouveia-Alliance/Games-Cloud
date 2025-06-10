package pt.ulisboa.tecnico.cnv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoScaler {

    // List of worker instance IPs managed by AutoScaler
    //private final CopyOnWriteArrayList<String> workerIPs = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Integer> workerPorts = new CopyOnWriteArrayList<>();

    // Configurable thresholds
    private static final int MAX_REQUESTS_PER_WORKER = 2;
    private static final int MIN_WORKERS = 1;
    private static final int MAX_WORKERS = 10;

    private static final int BASE_PORT = 8000; // Starting worker port

    // Simulated metric: total incoming requests count
    private AtomicInteger incomingRequestCount = new AtomicInteger(0);
    private final AtomicInteger unresolvedRequestCount = new AtomicInteger(0);

    public AutoScaler() {
        workerPorts.add(BASE_PORT);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAndScale,
                0, 5, TimeUnit.SECONDS);
    }

    // Called by LB to update current request load
    public void incrementIncomingRequestCount() {
        incomingRequestCount.incrementAndGet();
    }

    // Called by LB to update current request load
    public void decrementUnresolvedRequestCount() {
        unresolvedRequestCount.decrementAndGet();
    }

    // Called by LB to update current request load
    public int getUnresolvedRequestCount() {
        return unresolvedRequestCount.get();
    }

    // Expose current worker IPs for LoadBalancer to use
    public List<Integer> getWorkerPorts() {
        return new ArrayList<>(workerPorts);
    }

    // Periodically called by LoadBalancer or scheduled internally
    public void checkAndScale() {
        int requestCount = incomingRequestCount.getAndSet(0);
        System.out.println("Incoming Requests count: " + requestCount);

        unresolvedRequestCount.addAndGet(requestCount);
        System.out.println("Unresolved Requests count: " + unresolvedRequestCount);

        int currentWorkers = workerPorts.size();
        int idealWorkers = (unresolvedRequestCount.get() / MAX_REQUESTS_PER_WORKER) + 1;

        System.out.println("Ideal Workers: " + idealWorkers);

        if (idealWorkers > currentWorkers && currentWorkers < MAX_WORKERS) {
            // scale up
            int newWorkerPort = launchNewWorkerInstance();
            workerPorts.add(newWorkerPort);
            System.out.println("Scaled UP: added worker " + newWorkerPort);
        } else if (idealWorkers < currentWorkers && currentWorkers > MIN_WORKERS) {
            // scale down
            int removedWorkerPort = workerPorts.remove(workerPorts.size() - 1);
            terminateWorkerInstance(removedWorkerPort);
            System.out.println("Scaled DOWN: removed worker " + removedWorkerPort);
        }
    }

    private int launchNewWorkerInstance() {
        // Use AWS SDK to launch EC2 instance, install server, etc.
        // Return new worker private IP
        return BASE_PORT + workerPorts.size();
    }

    private void terminateWorkerInstance(int ip) {
        // Use AWS SDK to terminate EC2 instance by IP or ID
    }
}
