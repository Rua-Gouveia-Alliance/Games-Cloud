package pt.ulisboa.tecnico.cnv.loadbalancer;

import pt.ulisboa.tecnico.cnv.loadbalancer.util.ConcurrentQueue;
import pt.ulisboa.tecnico.cnv.loadbalancer.util.HttpUtils;

import java.io.IOException;
import java.io.OutputStream;

public class WorkerHandler extends Thread {
    private final int port;
    private final String ip = "127.0.0.1";

    public ConcurrentQueue<Request> queue = new ConcurrentQueue<>();

    public WorkerHandler(int port) {
        this.port = port;
        this.queue = queue;
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

                Request req = queue.pop();
                processRequest(req);
                sendResponse(req);
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

}