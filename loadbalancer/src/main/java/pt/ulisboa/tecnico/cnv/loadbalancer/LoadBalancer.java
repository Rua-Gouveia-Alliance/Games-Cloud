package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.loadbalancer.util.HttpUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadBalancer {

    private static final int LB_PORT = 8080;
    public static final long MAX_WORKLOAD = (long) 3066607530L;

    public final List<WorkerHandler> workers = Collections.synchronizedList(new ArrayList<>());

    private final Autoscaler autoscaler = new Autoscaler(this);

    public static void main(String[] args) throws IOException {
        new LoadBalancer().start();
    }

    private void start() throws IOException {
        // Start autoscaler
        autoscaler.start();

        // Start initial worker
        autoscaler.scaleOut();

        // Start load balancer server
        HttpUtils.createServer(LB_PORT, new LoadBalancerHandler());
        System.out.println("[LoadBalancer] Load balancer started on port " + LB_PORT);
    }

    class LoadBalancerHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String requestPath = exchange.getRequestURI().getPath();

                if (requestPath.equals("/") || requestPath.equals("/index.html")) {
                    System.out.println("[LoadBalancerHandler] Request for root path. Going to respond with default String.");
                    String response = "Welcome to Games@Cloud LoadBalancer! The system is up and running.";
                    exchange.getResponseHeaders().add("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                String fullPath = requestPath + (query != null ? "?" + query : "");
                System.out.println("[LoadBalancerHandler] Request for path: " + fullPath);

                Request req = new Request(fullPath, exchange);
                delegateToWorker(req);
            } catch (Exception e) {
                String error = "Error processing request: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        }

    }

    private void delegateToWorker(Request req) {
        WorkerHandler chosenWorker = chooseWorker(req);

        if (chosenWorker != null) {
            System.out.println("[LoadBalancer] Chosen worker for req (" + req.path + "): "  + chosenWorker.ip);
        } else {
            System.out.println("[LoadBalancer] Going to use lambda for req (" + req.path + ").");
        }

        if (chosenWorker == null) {
            useLambda(req);
            return;
        }

        chosenWorker.queue.push(req);
        synchronized (chosenWorker.queue.cond) {
            chosenWorker.queue.notifyChange();
        }
        System.out.println("[LoadBalancer] Notified worker " + chosenWorker.ip);
    }

    private WorkerHandler chooseWorker(Request req) {
        synchronized (workers) {
            if (workers.isEmpty()) {
                return null;
            }

            ArrayList<Long> scores = new ArrayList<>(workers.size());

            long newReqWorkload = req.calculateWorkload();
            System.out.println("[LoadBalancer] New req (" + req.path + ") workload: " + newReqWorkload);
            boolean available = false;
            for (int i = 0; i < workers.size(); i++) {
                long workerWorkload = workers.get(i).getCurrentWorkload();

                System.out.println("[LoadBalancer] Worker " + workers.get(i).ip + " workload: " + workerWorkload);
                if (workerWorkload + newReqWorkload > MAX_WORKLOAD) {
                    scores.add(Long.MAX_VALUE);
                } else {
                    scores.add((long) workerWorkload);
                    available = true;
                }
            }

            if (!available) {
                return null;
            }
            return workers.get(scores.indexOf(Collections.min(scores)));
        }
    }

    private void useLambda(Request req) {
        new LambdaWorker(req).start();
    }

    public void reassignRequest(Request req) {
        delegateToWorker(req);
    }

}
