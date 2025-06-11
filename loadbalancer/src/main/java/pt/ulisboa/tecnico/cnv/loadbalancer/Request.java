package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import pt.ulisboa.tecnico.cnv.loadbalancer.aws.DynamoRetriever;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* Paths of games:
 * http://${host}:${port}/gameoflife?iterations=${iterations}&mapFilename=${mapFilename}
 * http://${host}:${port}/fifteenpuzzle?size=${size}&shuffles=${shuffles}
 * http://${host}:${port}/capturetheflag?gridSize=${gridSize}&numBlueAgents=${numBlueAgents}&numRedAgents=${numRedAgents}&flagPlacementType=${flagPlacementType}
 */

public class Request {
    String path;
    String result;
    HttpExchange exchange;

    public Request(String path, HttpExchange exchange) {
        this.path = path;
        this.exchange = exchange;
    }

    public long calculateWorkload() {
        Map<String, Integer> paramsDict = this.getParamsDict();
        ArrayList<Object> recentClosest = DynamoRetriever.retrieveRecentClosestMetric(this.getType(), this.getParamsDict(), 100);
        Map<String, Long> recent = (Map<String, Long>) recentClosest.get(0);
        double distance = (double) recentClosest.get(1);

        if (recent == null) {
            System.out.println("[Request] No metric found, assuming default workload of 0");
            return 0;
        }

        try {
            long methods = recent.getOrDefault("methods", (long) 0);
            long blocks = recent.getOrDefault("basicBlocks", (long) 0);
            long instructions = recent.getOrDefault("instructions", (long) 0);

            // TODO tune these weights based on experiments
            // "If you have logs of real executions (with actual durations), you can compare those to the metrics and compute the best weights using a linear regression to predict duration from metrics."
            double workload = 1.0 * methods + 2.0 * blocks + 1.5 * instructions;

            // Distance-based adjustment
            // Example heuristic: 1% adjustment per unit of distance
            double adjustmentFactor = 1.0 + (distance * 0.01);
            double adjustedWorkload = workload * adjustmentFactor;

            return Math.round(adjustedWorkload);
        } catch (Exception e) {
            System.out.println("[Request] Error parsing metric values: " + e.getMessage());
            return 0;
        }
    }

    public String getType() {
        // extracts the type from the path, e.g., "/gameoflife" -> "gameoflife"
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

    public Map<String, Integer> getParamsDict() {
        Map<String, Integer> result = new HashMap<>();
        String type = getType();

        // initialize default values
        result.put("arg0", null);
        result.put("arg1", null);
        result.put("arg2", null);

        String[] params = getParams().split("&");
        Map<String, String> paramMap = new HashMap<>();
        for (String param : params) {
            String[] kv = param.split("=");
            if (kv.length == 2) {
                paramMap.put(kv[0], kv[1]);
            }
        }

        try {
            switch (type) {
                case "gameoflife":
                    // arg0 = iterations
                    result.put("arg0", Integer.parseInt(paramMap.getOrDefault("iterations", "0")));
                    break;

                case "fifteenpuzzle":
                    // arg0 = size, arg1 = shuffles
                    result.put("arg0", Integer.parseInt(paramMap.getOrDefault("size", "0")));
                    result.put("arg1", Integer.parseInt(paramMap.getOrDefault("shuffles", "0")));
                    break;

                case "capturetheflag":
                    // arg0 = gridSize, arg1 = numBlueAgents, arg2 = numRedAgents
                    result.put("arg0", Integer.parseInt(paramMap.getOrDefault("gridSize", "0")));
                    result.put("arg1", Integer.parseInt(paramMap.getOrDefault("numBlueAgents", "0")));
                    result.put("arg2", Integer.parseInt(paramMap.getOrDefault("numRedAgents", "0")));
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.print("[Request] Error parsing request params: " + e.toString());
        }

        return result;
    }
}
