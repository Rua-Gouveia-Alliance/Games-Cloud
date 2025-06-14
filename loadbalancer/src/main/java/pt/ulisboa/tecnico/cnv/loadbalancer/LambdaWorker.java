package pt.ulisboa.tecnico.cnv.loadbalancer;

import pt.ulisboa.tecnico.cnv.loadbalancer.aws.LambdaInvoker;

import java.io.IOException;
import java.io.OutputStream;

public class LambdaWorker extends Thread {

    Request req;

    public LambdaWorker(Request req) {
        this.req = req;
    }

    @Override
    public void run() {
        System.out.println("[LambdaWorker] Lambda request: " + req.path);

        String response = null;
        while (response == null) {
            String json = req.getParamsDictLambda().toString();
            response = LambdaInvoker.invokeLambda(req.getType() + "Handler", json);
            if (response != null)
                System.out.println("[LambdaWorker] Lambda response: " + response);
            else
                System.out.println("[LambdaWorker] Lambda failed, retrying...");
        }

        try {
            System.out.println("[LambdaWorker] Sending response: " + response);
            req.exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            req.exchange.sendResponseHeaders(200, req.result.length());
            try (OutputStream os = req.exchange.getResponseBody()) {
                os.write(req.result.getBytes());
            }
        } catch (IOException e) {
            System.out.println("[LambdaWorker] Failed sending response to client: " + e.toString());
        }
    }
}
