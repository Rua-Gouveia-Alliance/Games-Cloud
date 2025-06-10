package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerHandler {

    // 1. WORKER HANDLER REPRESENTS THE VM WORKER
    // 2. IT HAS A QUEUE OF REQUESTS TO PROCESS, PROCESSES ONE AT A TIME, STARTS BY LAST REQUEST IN THE LIST
    // 3. AFTER PROCESSING, RESPONDS TO LOAD-BALANCER AND WRITES METRICS TO DATABASE
    // NOTE: LAMBDA-HANDLER CAN BE A TWIN CLASS FOR FAAS WORKERS

    public static class RequestTask {
        public final String requestPath;
        public String response;

        public RequestTask(String path) {
            this.requestPath = path;
        }
    }

    private final int port;
    private final String ip = "127.0.0.1";
    private volatile boolean isBusy = true;
    private final ExecutorService executor;
    private final BlockingQueue<RequestTask> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger queueSize = new AtomicInteger(0);


    public WorkerHandler(int port) {
        this.port = port;
        this.executor = Executors.newSingleThreadExecutor(); // single-threaded executor
        startConsumerLoop();
    }

    private void startConsumerLoop() {
        executor.submit(() -> {
            while (isBusy) {
                try {
                    // This blocks until a task is available
                    RequestTask task = queue.take();
                    queueSize.decrementAndGet();

                    isBusy = true;

                    String path = task.requestPath;
                    String normalized = path.startsWith("/") ? path.substring(1) : path;

                    task.response = processRequest(normalized);

                    // TODO: respond to load-balancer, write metrics, etc.
                    //

                } catch (InterruptedException ie) {
                    // If shutting down, break; otherwise, restore interrupt and continue
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    System.err.println("Error processing request: " + e.getMessage());
                } finally {
                    isBusy = false;
                }
            }
        });
    }

    // Request Processing
    public String processRequest(String request) throws IOException, InterruptedException {
        return HttpUtils.sendGet(this.ip, this.port, request);
    }

    public void notifyLoadBalancer(String response) throws IOException {
        //TODO: Send response back to Load-Balancer
    }

    // Shut down cleanly when you’re done with this worker:
    public void shutdown() {
        isBusy = false;
        executor.shutdown();
    }

    // Setters
    public void enqueueRequest(RequestTask task) {
        queue.add(task);
        queueSize.incrementAndGet();
    }
    public void enqueueRequests(List<RequestTask> tasks) {
        queue.addAll(tasks);
        queueSize.addAndGet(tasks.size());
    }

    // Getters
    public String getWorkerIP() { return ip; }
    public boolean isBusy() { return isBusy; }
    public BlockingQueue<RequestTask> getQueue() { return queue; }
    public int getQueueSize() { return queueSize.get(); }
    public int getPort() { return port; }



}
