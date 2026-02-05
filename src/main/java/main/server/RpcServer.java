package main.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class RpcServer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Function<JsonNode, JsonNode>> methods = new HashMap<>();

    private final ExecutorService clientExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "rpc-client-handler");
        t.setDaemon(false); // server should stay alive until stopped
        return t;
    });

    private volatile SSLServerSocket serverSocket;

    public RpcServer() {
        methods.put("insertText", this::handleInsertText);
        methods.put("getAdminStatus", this::handleGetAdminStatus);
        methods.put("runCmdIgnoreErrors", this::handlerunCmdIgnoreErrors);
    }

    private void insertText(String message) {
        System.out.println("insertText(message) called with: " + message);
    }

    public static void runCmdIgnoreErrors(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe", "/c", command
            );
            builder.redirectErrorStream(true);

            Process process = builder.start();

            // Consume and ignore all output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // do nothing
                }
            }

            process.waitFor();
        } catch (Exception e) {
            // Completely ignore all errors
        }
    }

    private JsonNode handleInsertText(JsonNode req) {
        JsonNode params = req.path("params");
        String message = params.path("message").asText(null);
        if (message == null) throw new IllegalArgumentException("params.message is required");

        insertText(message);

        ObjectNode result = MAPPER.createObjectNode();
        result.put("stored", true);
        result.put("message", "insertText executed with: " + message);
        return result;
    }

    private JsonNode handlerunCmdIgnoreErrors(JsonNode req) {
        JsonNode params = req.path("params");
        String message = params.path("message").asText(null);
        if (message == null) throw new IllegalArgumentException("params.message is required");

        runCmdIgnoreErrors(message);

        ObjectNode result = MAPPER.createObjectNode();
        result.put("stored", true);
        result.put("message", "cmd executed with: " + message);
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
                Process p = new ProcessBuilder("cmd", "/c", "net", "session").redirectErrorStream(true).start();
                return p.waitFor() == 0;
            }
            Process p = new ProcessBuilder("id", "-u").redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String uid = r.readLine();
                p.waitFor();
                return "0".equals(uid);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private SSLServerSocket createTlsServerSocket(int port) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream("src/main/java/main/server/server-keystore.jks")) {
            ks.load(fis, "changeit".toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "changeit".toCharArray());

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);

        SSLServerSocketFactory ssf = ctx.getServerSocketFactory();
        return (SSLServerSocket) ssf.createServerSocket(port);
    }

    public void start(int port) throws IOException {
        try (SSLServerSocket ss = createTlsServerSocket(port)) {
            serverSocket = ss;
            System.out.println("RPC TLS server listening on port " + port);

            while (!ss.isClosed()) {
                try {
                    SSLSocket socket = (SSLSocket) ss.accept();
                    clientExecutor.execute(() -> handleClient(socket));
                } catch (SocketException e) {
                    // accept() unblocked because socket closed during stop()
                    break;
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to start TLS server: " + e.getMessage(), e);
        } finally {
            stop();
        }
    }

    public void stop() {
        SSLServerSocket ss = serverSocket;
        serverSocket = null;
        if (ss != null && !ss.isClosed()) {
            try { ss.close(); } catch (IOException ignored) {}
        }
        clientExecutor.shutdownNow();
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
        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) throws IOException {
        RpcServer server = new RpcServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start(9000);
    }
}