package main.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcClientHelper implements Closeable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final AtomicInteger nextId = new AtomicInteger(1);

    public RpcClientHelper(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    // Simple RPC call with params JSON object
    public synchronized JsonNode call(String method, ObjectNode params) throws IOException {
        int id = nextId.getAndIncrement();

        ObjectNode req = MAPPER.createObjectNode();
        req.put("id", id);
        req.put("method", method);
        req.set("params", params == null ? MAPPER.createObjectNode() : params);

        out.write(req.toString());
        out.write("\n");
        out.flush();

        String reply = in.readLine();          // one-line JSON response
        return MAPPER.readTree(reply);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public static ObjectNode paramsWithMessage(String message) {
        ObjectNode p = MAPPER.createObjectNode();
        p.put("message", message);
        return p;
    }
}