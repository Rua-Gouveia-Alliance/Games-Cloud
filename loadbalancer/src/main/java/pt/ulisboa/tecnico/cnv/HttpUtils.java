package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpUtils {

    private static final HttpClient client = HttpClient.newHttpClient();
    public static class ParsedRequest {
        private String method;
        private String path;
        private String httpVersion;
        ParsedRequest(String method, String path, String httpVersion) {
            this.method = method;
            this.path = path;
            this.httpVersion = httpVersion;
        }
    }

    public static String sendGet(String ip, int port, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + ip + ":" + port + "/" + path))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public static void sendResponse(BufferedWriter out, String responseBody) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Access-Control-Allow-Origin: *\r\n");
        out.write("Content-Type: text/plain\r\n");
        out.write("Content-Length: " + responseBody.length() + "\r\n");
        out.write("Connection: close\r\n");
        out.write("\r\n");
        out.write(responseBody);
        out.flush();
    }

    public static void sendError(BufferedWriter out, int statusCode, String message) throws IOException {
        out.write("HTTP/1.1 " + statusCode + " " + message + "\r\n");
        out.write("Content-Length: 0\r\n");
        out.write("\r\n");
        out.flush();
    }

    /**
     * Parses an HTTP request line like "GET /path HTTP/1.1".
     * @param requestLine the first line of the HTTP request.
     * @return a ParsedRequest object or null if invalid.
     */
    public static ParsedRequest parseRequestLine(String requestLine) {
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 3) {
            return null;
        }

        String method = parts[0];
        String path = parts[1];
        String httpVersion = parts[2];

        return new ParsedRequest(method, path, httpVersion);
    }

}
