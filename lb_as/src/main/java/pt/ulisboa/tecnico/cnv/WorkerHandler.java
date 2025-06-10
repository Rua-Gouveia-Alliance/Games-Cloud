package pt.ulisboa.tecnico.cnv;

import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerHandler {
    private final int port;
    private final ExecutorService executor;
    private final AtomicInteger queueSize = new AtomicInteger(0);

    public WorkerHandler(int port) {
        this.port = port;
        this.executor = Executors.newSingleThreadExecutor(); // single-threaded executor
    }

    public void enqueue(RequestTask task) {
        queueSize.incrementAndGet(); // track task
        executor.submit(() -> {
            try {
                String normalized = task.path.startsWith("/") ? task.path.substring(1) : task.path;
                String response = HttpUtils.get("127.0.0.1", port, normalized);
                System.out.println("Processed by worker on port " + port + ".");
                task.responseFuture.complete(response);
            } catch (SocketTimeoutException e) {
                task.responseFuture.completeExceptionally(e);
                System.err.println("Timeout in worker " + port + ": " + e.getMessage());
            } catch (Exception e) {
                task.responseFuture.completeExceptionally(e);
                System.err.println("Error in worker " + port + ": " + e.getMessage());
            } finally {
                queueSize.decrementAndGet(); // task done
            }
        });
    }

    public int getQueueSize() {
        return queueSize.get();
    }
    public int getPort() {
        return port;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class RequestTask {
        public final String path;
        public final CompletableFuture<String> responseFuture;

        public RequestTask(String path) {
            this.path = path;
            this.responseFuture = new CompletableFuture<>();
        }
    }
}
