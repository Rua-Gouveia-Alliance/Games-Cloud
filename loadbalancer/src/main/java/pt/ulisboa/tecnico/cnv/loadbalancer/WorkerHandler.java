package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.IOException;

public class WorkerHandler {
    private final int port;
    private final String ip = "127.0.0.1";

    public WorkerHandler(int port) {
        this.port = port;
    }

    public String processRequest(String request) throws IOException, InterruptedException {
        String normalized = request.startsWith("/") ? request.substring(1) : request;
        return HttpUtils.sendGet(ip, port, normalized);
    }

    public int getPort() {
        return port;
    }
}