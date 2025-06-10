package pt.ulisboa.tecnico.cnv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpUtils {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static void sendError(BufferedWriter out, int statusCode, String message) throws IOException {
        out.write("HTTP/1.1 " + statusCode + " " + message + "\r\n");
        out.write("Content-Length: 0\r\n");
        out.write("\r\n");
        out.flush();
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

    /**
     * Sends a basic GET request to the given URL.
     */

    public static String get(String ip, int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + ip + ":" + port + "/" + path))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }


    /**
     * Sends a POST request to the given URL with optional body content.
     */
    public static String post(String ip, int port, String path, String body) throws IOException, InterruptedException {
        String url = "http://" + ip + ":" + port + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Sends a GET request with query parameters.
     */
    public static String getWithParams(String ip, int port, String path, Map<String, String> params) throws Exception {
        StringBuilder query = new StringBuilder("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            query.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("&");
        }
        query.setLength(query.length() - 1); // remove trailing "&"

        return get(ip, port, path + query.toString());
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

    /**
     * Represents a parsed HTTP request line.
     */
    public static class ParsedRequest {
        public final String method;
        public final String path;
        public final String httpVersion;

        public ParsedRequest(String method, String path, String httpVersion) {
            this.method = method;
            this.path = path;
            this.httpVersion = httpVersion;
        }
    }

}
