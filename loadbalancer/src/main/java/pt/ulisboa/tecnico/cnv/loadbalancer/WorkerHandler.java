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

    public WorkerHandler(String instanceId, String ip) {
        this.instanceId = instanceId;
        this.ip = ip;
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
                processRequest(currentRequest);
                sendResponse(currentRequest);
                currentRequest = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void processRequest(Request req) {
        try {
            req.result = HttpUtils.sendGet(ip, port, req.path);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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