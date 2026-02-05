package main.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcClientHelper implements Closeable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final AtomicInteger nextId = new AtomicInteger(1);

    public RpcClientHelper(String host, int port) throws IOException {
        try {
            this.socket = createTlsSocket(host, port);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException("Failed to create TLS socket: " + e.getMessage(), e);
        }
    }

    private SSLSocket createTlsSocket(String host, int port) throws Exception {
        KeyStore ts = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream("src/main/resources/client-truststore.jks")) {
            ts.load(fis, "changeit".toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);

        SSLSocketFactory sf = ctx.getSocketFactory();
        return (SSLSocket) sf.createSocket(host, port);
    }

    public synchronized JsonNode call(String method, ObjectNode params) throws IOException {
        int id = nextId.getAndIncrement();

        ObjectNode req = MAPPER.createObjectNode();
        req.put("id", id);
        req.put("method", method);
        req.set("params", params == null ? MAPPER.createObjectNode() : params);

        out.write(req.toString());
        out.write("\n");
        out.flush();

        String reply = in.readLine();
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