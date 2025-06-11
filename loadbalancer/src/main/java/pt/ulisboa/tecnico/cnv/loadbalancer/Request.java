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

    public String getType() {
        // Extracts the type from the path, e.g., "/gameoflife" -> "gameoflife"
        int start = path.startsWith("/") ? 1 : 0;
        int end = path.indexOf('?');
        if (end == -1) {
            return path.substring(start);
        } else {
            return path.substring(start, end);
        }
    }

    public String getParams() {
        int idx = path.indexOf('?');
        if (idx == -1 || idx == path.length() - 1) {
            return "";
        }
        return path.substring(idx + 1);
    }
}
