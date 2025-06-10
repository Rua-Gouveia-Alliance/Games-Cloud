package pt.ulisboa.tecnico.cnv;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancer {

    private static final int LB_PORT = 8080;
    private static final String WORKER_IP = "127.0.0.1"; // or EC2 private IP

    private final AutoScaler autoScaler = new AutoScaler();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final AtomicInteger currentWorkerIndex = new AtomicInteger(0);
    private final List<WorkerHandler> workerHandlers = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        new LoadBalancer().start();
    }

    private void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(LB_PORT);
        System.out.println("Load Balancer listening on port " + LB_PORT);

        while (true) {
            System.out.println("Ping.1.");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Ping.2.");
            threadPool.execute(() -> handleClient(clientSocket));
            System.out.println("Ping.3.");
        }
    }

    private void handleClient(Socket clientSocket) {
        System.out.println("Client connected. Socket: " + clientSocket);
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            String line = in.readLine();
            // discard rest of headers
            while (in.ready() && !in.readLine().isEmpty());

            System.out.println("LINE: " + line);
            HttpUtils.ParsedRequest parsed = HttpUtils.parseRequestLine(line);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid request line: " + line);
            }

            if (!parsed.method.equals("GET")) {
                throw new IllegalArgumentException("Invalid request line: " + line);
            }

            autoScaler.incrementIncomingRequestCount();

            // Send request to be enqueued
            CompletableFuture<String> responseFuture = forwardToWorker(parsed.path);
            String responseBody = responseFuture.get(5, TimeUnit.SECONDS);
            System.out.println("Responding " );
            HttpUtils.sendResponse(out, responseBody);

        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            System.out.println("Client disconnected.");
            autoScaler.decrementUnresolvedRequestCount();
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private WorkerHandler chooseWorker() {
        // 1) re-calculate how many workers you need right now
        List<Integer> livePorts = autoScaler.getWorkerPorts();

        // 2) pull in the updated ports list
        syncWorkers(livePorts);

        // 3) pick the least-loaded worker
        WorkerHandler chosen;
        synchronized (workersLock) {
            chosen = workerHandlers.stream()
                    .min(Comparator.comparingInt(WorkerHandler::getQueueSize))
                    .orElseThrow(() -> new IllegalStateException("No workers!"));
        }

        System.out.println("Forwarding to worker port " + chosen.getPort()
                + " (queue=" + chosen.getQueueSize() + ")");
        return chosen;
    }

    private CompletableFuture<String> forwardToWorker(String path) {

        WorkerHandler chosen = chooseWorker();
        WorkerHandler.RequestTask task = new WorkerHandler.RequestTask(path);
        try {
            chosen.enqueue(task);
            return task.responseFuture;
        } catch (Exception e) {
            // if enqueue somehow fails, return a completed future
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.complete("Error enqueuing to worker on port "
                    + chosen.getPort() + ": " + e.getMessage());
            return failed;
        }
    }

    private final Object workersLock = new Object();
    private void syncWorkers(List<Integer> livePorts) {
        synchronized (workersLock) {
            // Remove any handlers whose port is no longer in desiredPorts
            workerHandlers.removeIf(h -> {
                if (!livePorts.contains(h.getPort())) {
                    h.shutdown();
                    return true;
                }
                return false;
            });
            // Add any new ports
            for (int port : livePorts) {
                boolean already = workerHandlers.stream()
                        .anyMatch(h -> h.getPort() == port);
                if (!already) {
                    workerHandlers.add(new WorkerHandler(port));
                }
            }
        }
    }


}
