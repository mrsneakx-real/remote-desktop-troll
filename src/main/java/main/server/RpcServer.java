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

    // Each method maps to a handler that takes the request and returns a result node (or null)
    private final Map<String, Function<JsonNode, JsonNode>> methods = new HashMap<>();

    public RpcServer() {
        methods.put("insertText", this::handleInsertText);
    }

    // Your real server-side function (business logic)
    private void insertText(String message) {
        System.out.println("insertText(message) called with: " + message);
        // Insert it wherever you need (DB, list, UI model, file, etc.)
    }

    private JsonNode handleInsertText(JsonNode req) {
        JsonNode params = req.path("params");
        String message = params.path("message").asText(null);
        if (message == null) {
            throw new IllegalArgumentException("params.message is required");
        }

        insertText(message);

        // Optional: return something meaningful
        ObjectNode result = MAPPER.createObjectNode();
        result.put("stored", true);
        return result;
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