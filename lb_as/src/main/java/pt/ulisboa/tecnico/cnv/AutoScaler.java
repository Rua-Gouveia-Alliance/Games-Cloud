package pt.ulisboa.tecnico.cnv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoScaler {

    // 1. AUTO-SCALER WILL BE CONSTANTLY CHECKING FOR UPDATES BY READING METRICS IN DATABASE
    // 2. BASED ON THESE METRICS, IT WILL DECIDE TO SCALE UP, DOWN, OR KEEP THE SAME AMOUNT
    // 3. FOR NOW, USING SIMULATED METRICS

    //private final CopyOnWriteArrayList<String> workerIPs = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<WorkerHandler> workers = new CopyOnWriteArrayList<>();

    // Configurable thresholds
    private static final int MAX_REQUESTS_PER_WORKER = 2;
    private static final int MIN_WORKERS = 1;
    private static final int MAX_WORKERS = 10;

    private static final int BASE_PORT = 8000; // Starting worker port

    // Simulated metrics: unresolved request count
    // In reality, the metrics it uses are written by the WorkerHandlers to Storage and A-S goes fetch them
    private final AtomicInteger unresolvedRequestCount = new AtomicInteger(0);

    public AutoScaler() {
        workers.add(new WorkerHandler(BASE_PORT));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAndScale,
                0, 5, TimeUnit.SECONDS);
    }

    public void checkAndScale() {
        // TODO: Instead of simulated metrics, use real ones from DB

        int requestCount = getUnresolvedRequestCount();
        System.out.println("Unresolved Request count: " + requestCount);

        int currentWorkers = workers.size();
        int idealWorkers = (requestCount / MAX_REQUESTS_PER_WORKER) + 1;

        System.out.println("Ideal Workers: " + idealWorkers);

        if (idealWorkers > currentWorkers && currentWorkers < MAX_WORKERS) {    // SCALE UP
            launchNewWorkerInstance();
        } else if (idealWorkers < currentWorkers && currentWorkers > MIN_WORKERS) { // SCALE DOWN
            // TODO: Decide which worker to terminate. For now, highest port
            terminateWorkerInstance(workers.size()-1);
        }
    }

    private void launchNewWorkerInstance() {
        // Use AWS SDK to launch EC2 instance, install server, etc.
        // Return new worker private IP
        WorkerHandler newWorker = new WorkerHandler(BASE_PORT + workers.size());
        workers.add(newWorker);
        System.out.println("Scaled UP: added worker " + newWorker.getPort());
    }

    private void terminateWorkerInstance(int index) {
        // Use AWS SDK to terminate EC2 instance by IP or ID
        workers.get(index).shutdown();
        workers.remove(index);
        System.out.println("Scaled DOWN: removed worker " + workers.get(index).getPort());
    }

    // Setters
    public void incrementUnresolvedRequestCount() {
        unresolvedRequestCount.incrementAndGet();
    }
    public void decrementUnresolvedRequestCount() {
        unresolvedRequestCount.decrementAndGet();
    }

    // Getters
    public int getUnresolvedRequestCount() { return unresolvedRequestCount.get(); }

    public List<WorkerHandler> getWorkers() {
        return new ArrayList<>(workers);
    }
    public Integer getWorkersTotalNumber() { return workers.size(); }
}
