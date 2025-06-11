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
        String response = null;
        while (response == null) {
            String json = "{ \"params\": \"" + req.getParams() + "\" }";
            response = LambdaInvoker.invokeLambda(req.getType() + "Handler", json);
            System.out.println("Lambda response: " + response);
        }

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