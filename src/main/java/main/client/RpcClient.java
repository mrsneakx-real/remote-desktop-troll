package main.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class RpcClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        String host = "127.0.0.1"; // <-- server LAN IP
        int port = 9000;

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            ObjectNode req = MAPPER.createObjectNode();
            req.put("id", 1);
            req.put("method", "function");
            req.set("params", MAPPER.createObjectNode());

            out.write(req.toString());
            out.write("\n");
            out.flush();

            String replyLine = in.readLine();
            System.out.println("Server reply: " + replyLine);
        }
    }
}