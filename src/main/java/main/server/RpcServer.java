package main.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RpcServer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Function<JsonNode, JsonNode>> methods = new HashMap<>();

    public RpcServer() {
        methods.put("insertText", this::handleInsertText);
        methods.put("getAdminStatus", this::handleGetAdminStatus);
    }

    private void insertText(String message) {
        System.out.println("insertText(message) called with: " + message);
    }

    private JsonNode handleInsertText(JsonNode req) {
        JsonNode params = req.path("params");
        String message = params.path("message").asText(null);
        if (message == null) {
            throw new IllegalArgumentException("params.message is required");
        }

        insertText(message);

        ObjectNode result = MAPPER.createObjectNode();
        result.put("stored", true);
        result.put("message", "insertText executed with: " + message);
        return result;
    }

    private JsonNode handleGetAdminStatus(JsonNode req) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("isAdmin", isRunningAsAdmin());
        result.put("message", "Admin status checked");
        return result;
    }

    private boolean isRunningAsAdmin() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                Process p = new ProcessBuilder("cmd", "/c", "net", "session")
                        .redirectErrorStream(true)
                        .start();
                int exit = p.waitFor();
                return exit == 0;
            } else {
                Process p = new ProcessBuilder("id", "-u")
                        .redirectErrorStream(true)
                        .start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String uid = r.readLine();
                    p.waitFor();
                    return "0".equals(uid);
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("RPC server listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {
                ObjectNode response = MAPPER.createObjectNode();
                try {
                    JsonNode req = MAPPER.readTree(line);
                    int id = req.path("id").asInt(-1);
                    String method = req.path("method").asText("");

                    response.put("id", id);

                    Function<JsonNode, JsonNode> handler = methods.get(method);
                    if (handler == null) {
                        response.put("ok", false);
                        response.put("error", "Unknown method: " + method);
                    } else {
                        JsonNode result = handler.apply(req);
                        response.put("ok", true);
                        if (result != null) response.set("result", result);
                    }
                } catch (Exception e) {
                    response.put("id", -1);
                    response.put("ok", false);
                    response.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                }

                out.write(response.toString());
                out.write("\n");
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        new RpcServer().start(9000);
    }
}