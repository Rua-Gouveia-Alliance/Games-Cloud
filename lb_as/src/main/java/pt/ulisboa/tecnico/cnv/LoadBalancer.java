package pt.ulisboa.tecnico.cnv;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancer {

    // 1. LOAD-BALANCER WILL START A SERVER SOCKET AND LISTEN FOR REQUESTS
    // 2. WHEN IT ACCEPTS A REQUEST, CHOOSES ONE THREAD FROM A CACHED POOL TO DEAL WITH IT
    // 3. LB PARSES THE INCOMING REQUEST AND CHECKS VALIDITY
    // 4. IT THEN CHOOSES THE BEST WORKER FOR THE PARSED REQUEST
    // 5. IT THEN DELEGATES THIS PARSED REQUEST TO THE CHOSEN WORKER
    // 6. AFTER SENDING THE REQUEST, IT WAITS FOR A RESPONSE
    // 7. WHEN IT RECEIVES THE RESPONSE, SENDS IT BACK TO THE CLIENT

    private static final int LB_PORT = 8080;
    private static final String WORKER_IP = "127.0.0.1"; // For local testing

    private final AutoScaler autoScaler = new AutoScaler();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final AtomicInteger currentWorkerIndex = new AtomicInteger(0);
    private final List<WorkerHandler> workerHandlers = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        new LoadBalancer().start();
    }

    private void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(LB_PORT);
        System.out.println("Load Balancer listening on port " + LB_PORT);

        // 1. LOAD-BALANCER WILL START A SERVER SOCKET AND LISTEN FOR REQUESTS
        while (true) {
            Socket clientSocket = serverSocket.accept();

            // 2. WHEN IT ACCEPTS A REQUEST, CHOOSES ONE THREAD FROM A CACHED POOL TO DEAL WITH IT
            threadPool.execute(() -> handleClientRequest(clientSocket));
        }
    }

    private void handleClientRequest(Socket clientSocket) {
        System.out.println("Client request from socket: " + clientSocket);

        try (
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            // 3. LB PARSES THE INCOMING REQUEST AND CHECKS VALIDITY
            String requestPath = parseRequest(clientSocket);

            autoScaler.incrementUnresolvedRequestCount();

            String response = delegateToWorker(requestPath);
            HttpUtils.sendResponse(out, response);

            autoScaler.decrementUnresolvedRequestCount();

        } catch (IOException e) {
            System.err.println("Error while processing request from socket: " + clientSocket);
            e.getMessage();
        }

    }

    private String parseRequest(Socket socket) throws IOException, IllegalArgumentException {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            String line = in.readLine();
            // discard rest of headers
            while (in.ready() && !in.readLine().isEmpty());

            System.out.println("LINE: " + line);    // DEBUGGING

            HttpUtils.ParsedRequest parsed = HttpUtils.parseRequestLine(line);
            if (parsed == null || parsed.path() == null || parsed.path().isEmpty() || !parsed.method().equals("GET")) {
                throw new IllegalArgumentException("Invalid request line: " + line);
            }
            return parsed.path();
        }
    }

    private String delegateToWorker(String path) {
        // 1. LOAD-BALANCER WILL FIRST CHOOSE WHICH WORKER TO DELEGATE TO FROM THE AVAILABLE WORKERS
        WorkerHandler chosenWorker = chooseWorker();
        WorkerHandler.RequestTask task = new WorkerHandler.RequestTask(path);

        // 2. WHEN THE WORKER HAS BEEN CHOSEN, ENQUEUE REQUEST-TASK
        chosenWorker.enqueueRequest(task);

        // 3. THIS THREAD THEN WAITS FOR A RESPONSE
        // awaitResponse()      // Await response

        return task.response;
    }

    private WorkerHandler chooseWorker() {
        // 1. LB CHECKS FOR AVAILABLE WORKER BY TALKING WITH AUTO-SCALER (syncWorkers())
        // 2. WITHIN THE AVAILABLE WORKERS, LB WILL PERFORM A CALCULATION TO FIND THE BEST SUITOR
        // 3. FINALLY, RETURNS THE CORRESPONDING WORKER (WorkerHandler)

        // 1. LB CHECKS FOR AVAILABLE WORKER BY TALKING WITH AUTO-SCALER (syncWorkers())
        // List<String> liveWorkers = getLiveWorkers();        // for IP addresses instead of Ports
        List<Integer> liveWorkers = getLiveWorkers();

        // 2. WITHIN THE AVAILABLE WORKERS, LB WILL PERFORM A CALCULATION TO FIND THE BEST SUITOR
        // System.out.println("Forwarding to worker port " + chosen.getPort() + " (queue=" + chosen.getQueueSize() + ")");
        // return calculateBestWorker(liveWorkers);

        return null;
    }

    private List<Integer> getLiveWorkers() {
        // 1. LB CHECKS AUTO-SCALER LIST OF WORKERS
        return autoScaler.getWorkerPorts();
    }


}
