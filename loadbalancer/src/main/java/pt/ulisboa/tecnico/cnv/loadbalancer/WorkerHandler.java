package pt.ulisboa.tecnico.cnv.loadbalancer;

import pt.ulisboa.tecnico.cnv.loadbalancer.util.ConcurrentQueue;
import pt.ulisboa.tecnico.cnv.loadbalancer.util.HttpUtils;
import pt.ulisboa.tecnico.cnv.loadbalancer.aws.MetricsCollector;

import java.io.IOException;
import java.io.OutputStream;


public class WorkerHandler extends Thread {
    public String instanceId;
    private final String ip;
    private final int port = 8000;

    public ConcurrentQueue<Request> queue = new ConcurrentQueue<>();
    private Request currentRequest;
    private LoadBalancer loadBalancer;

    public WorkerHandler(String instanceId, String ip, LoadBalancer loadBalancer) {
        this.instanceId = instanceId;
        this.ip = ip;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public void run() {
        try {
            while (true) {
                while (queue.length() == 0) {
                    synchronized (queue.cond) {
                        queue.waitChange();
                    }
                }

                currentRequest = queue.pop();
                boolean success = processRequest(currentRequest);
                if (!success) {
                    handleCrash();
                    return;
                }

                sendResponse(currentRequest);
                currentRequest = null;
            }
        } catch (InterruptedException e) {
            System.out.println("[WorkerHandler] Terminated unexpectedly: " + e.toString());
        }
    }

    private void handleCrash() {
        System.err.println("[WORKER FAILURE] Instance " + instanceId + " failed. Reassigning tasks.");

        synchronized (loadBalancer.workers) {
            loadBalancer.workers.remove(this);
        }

        // reassign current request
        if (currentRequest != null) {
            loadBalancer.reassignRequest(currentRequest);
        }

        // reassign all queued requests
        for (Request r : queue.getContainer()) {
            loadBalancer.reassignRequest(r);
        }

        queue.clear();
    }

    private boolean processRequest(Request req) {
        try {
            req.result = HttpUtils.sendGet(ip, port, req.path);
            return true;
        } catch (InterruptedException | IOException e) {
            // VM crashed
            req.result = null;
            return false;
        }
    }

    private void sendResponse(Request req) {
        try {
            req.exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            req.exchange.sendResponseHeaders(200, req.result.length());
            try (OutputStream os = req.exchange.getResponseBody()) {
                os.write(req.result.getBytes());
            }
        } catch (IOException e) {
            System.out.println("[WorkerHandler] Failed sending response to client: " + e.toString());
        }
    }

    public long getCurrentWorkload() {
        long total = 0;
        for (Request req : queue.getContainer()) {
            total += req.calculateWorkload();
        }

        if (currentRequest != null) {
            total += currentRequest.calculateWorkload();
        }

        return total;
    }

    public Request getCurrentRequest() {
        return currentRequest;
    }

}
