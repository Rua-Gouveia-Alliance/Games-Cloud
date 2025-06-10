package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancer {

    // 1. LOAD-BALANCER WILL START A SERVER SOCKET AND LISTEN FOR REQUESTS
    // 2. WHEN IT ACCEPTS A REQUEST, CHOOSES ONE THREAD FROM A CACHED POOL TO DEAL WITH IT
    // 3. LB PARSES THE INCOMING REQUEST AND CHECKS VALIDITY
    // 4. IT THEN CHOOSES THE BEST WORKER FOR THE PARSED REQUEST
    // 5. IT THEN DELEGATES THIS PARSED REQUEST TO THE CHOSEN WORKER
    // 6. AFTER SENDING THE REQUEST, IT WAITS FOR A RESPONSE
    // 7. WHEN IT RECEIVES THE RESPONSE, SENDS IT BACK TO THE CLIENT

    private static final int LB_PORT = 8080;
    private static final String WORKER_IP = "127.0.0.1"; // For local testing
    private static final int WORKER_BASE_PORT = 8000; // For local testing

    private final List<WorkerHandler> workers = new CopyOnWriteArrayList<>();
    private final AtomicInteger currentWorkerIndex = new AtomicInteger(0);

    private Autoscaler autoscaler = new Autoscaler();

    public static void main(String[] args) throws IOException {
        new LoadBalancer().start();
    }

    private void start() throws IOException {
        // Start initial worker
        workers.add(new WorkerHandler(WORKER_BASE_PORT));

        // Start load balancer server
        HttpUtils.createServer(LB_PORT, new LoadBalancerHandler());
        System.out.println("Load balancer started on port " + LB_PORT);
    }

    class LoadBalancerHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String requestPath = exchange.getRequestURI().getPath();
                System.out.println(requestPath);
                String query = exchange.getRequestURI().getQuery();
                System.out.println(query);
                String fullPath = requestPath + (query != null ? "?" + query : "");
                System.out.println(fullPath);

                String response = delegateToWorker(fullPath);

                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (Exception e) {
                String error = "Error processing request: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        }

    }

    private String delegateToWorker(String path) {
        WorkerHandler chosenWorker = chooseWorker();
        try {
            return chosenWorker.processRequest(path);
        } catch (IOException | InterruptedException e) {
            workers.remove(chosenWorker);
            return "Error processing request: " + e.getMessage();
        }
    }

    private WorkerHandler chooseWorker() {
        if (workers.isEmpty()) {
            throw new IllegalStateException("No available workers");
        }

        //TODO: Load-Balancer choosing Logic
        int index = currentWorkerIndex.getAndUpdate(i -> (i + 1) % workers.size());
        return workers.get(index);
    }
}
