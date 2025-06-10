package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.sun.net.httpserver.HttpExchange;

public class Request {
    String path;
    String result;
    HttpExchange exchange;

    public Request(String path, HttpExchange exchange) {
        this.path = path;
        this.exchange = exchange;
    }

    public long calculateWorkload() {
        // TODO calculate workload with the help of dynamo metrics
        return 0;
    }
}
