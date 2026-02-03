package main.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import main.client.RpcClientHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboradA {

    private RpcClientHelper rpc;
    private final ExecutorService rpcExecutor = Executors.newSingleThreadExecutor();

    public Parent createView() {
        Label titleLabel = new Label("Admin Dashboard");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label statusLabel = new Label("Not connected");

        TextArea console = new TextArea();
        console.setEditable(false);
        console.setPrefRowCount(6);
        console.setMinHeight(120);

        // ---- Server connection row (IP + connect + disconnect) ----
        Label serverIpLabel = new Label("Server IP:");
        TextField serverIpField = new TextField("127.0.0.1");
        serverIpField.setPromptText("e.g. 192.168.0.1");

        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");
        Button quitButton = new Button("Quit");
        disconnectButton.setDisable(true);

        HBox connectionRow = new HBox(10, serverIpLabel, serverIpField, connectButton, disconnectButton, quitButton);
        connectionRow.setPadding(new Insets(0, 0, 10, 0));

        // ---- Modular fields ----
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        TextField field1 = addFieldRow(formGrid, 0, "Example1");
        TextField field2 = addFieldRow(formGrid, 1, "Example2");
        TextField field3 = addFieldRow(formGrid, 2, "Example3");

        // ---- Modular buttons ----
        Button button1 = createActionButton("Example1");
        Button button2 = createActionButton("Example2");
        Button button3 = createActionButton("Example3");

        button3.setTooltip(new Tooltip("No permissions to use this function"));

        VBox buttonBox = new VBox(10, button1, button2, button3);
        buttonBox.setPrefWidth(120);

        HBox mainLayout = new HBox(20, formGrid, buttonBox);
        VBox root = new VBox(15, titleLabel, connectionRow, statusLabel, mainLayout, console);
        root.setPadding(new Insets(20));

        VBox.setVgrow(console, Priority.ALWAYS);

        // Connect action
        connectButton.setOnAction(evt -> {
            String ip = serverIpField.getText().trim();
            connect(statusLabel, console, ip, 9000, connectButton, disconnectButton, button1, button2, button3);
        });

        // Disconnect action
        disconnectButton.setOnAction(evt ->
                disconnect(statusLabel, console, connectButton, disconnectButton, button1, button2, button3)
        );

        //Quit Button
        quitButton.setOnAction(evt ->
                shutdownClient()
        );

        // Enter in IP field triggers connect
        serverIpField.setOnAction(evt -> connectButton.fire());

        // Button handlers
        button1.setOnAction(evt -> callServer(statusLabel, console, "insertText", field1.getText()));
        button2.setOnAction(evt -> callServer(statusLabel, console, "insertText", field2.getText()));
        button3.setOnAction(evt -> callServer(statusLabel, console, "insertText", field3.getText()));

        return root;
    }

    private TextField addFieldRow(GridPane grid, int row, String labelText) {
        Label label = new Label(labelText);
        TextField field = new TextField();
        field.setPromptText(labelText);

        grid.add(label, 0, row);
        grid.add(field, 1, row);
        return field;
    }

    private Button createActionButton(String text) {
        Button button = new Button(text);
        button.setDisable(true);
        return button;
    }

    private void connect(Label statusLabel,
                         TextArea console,
                         String host,
                         int port,
                         Button connectButton,
                         Button disconnectButton,
                         Button button1,
                         Button button2,
                         Button button3) {

        statusLabel.setText("Connecting to " + host + ":" + port + " ...");
        statusLabel.setStyle("-fx-text-fill: #ffb433");
        connectButton.setDisable(true);

        rpcExecutor.execute(() -> {
            try {
                RpcClientHelper old = rpc;
                if (old != null) {
                    try { old.close(); } catch (Exception ignored) {}
                }

                rpc = new RpcClientHelper(host, port);

                JsonNode resp = rpc.call("getAdminStatus", RpcClientHelper.paramsWithMessage(""));
                boolean isAdmin = resp.path("result").path("isAdmin").asBoolean(false);

                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #11c214");
                    statusLabel.setText("Connected to " + host + ":" + port + " (admin=" + isAdmin + ")");
                    appendConsole(console, "Connected. Admin=" + isAdmin);

                    disconnectButton.setDisable(false);
                    connectButton.setDisable(false);

                    button1.setDisable(false);
                    button2.setDisable(false);
                    button3.setDisable(!isAdmin);
                });
            } catch (Exception e) {
                rpc = null;
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #c20202");
                    statusLabel.setText("Connection failed: " + e.getMessage());
                    appendConsole(console, "Connection failed: " + e.getMessage());

                    disconnectButton.setDisable(true);
                    connectButton.setDisable(false);

                    button1.setDisable(true);
                    button2.setDisable(true);
                    button3.setDisable(true);
                });
            }
        });
    }

    private void disconnect(Label statusLabel,
                            TextArea console,
                            Button connectButton,
                            Button disconnectButton,
                            Button... rpcButtons) {

        RpcClientHelper old = rpc;
        rpc = null;

        if (old != null) {
            try { old.close(); } catch (Exception ignored) {}
        }

        statusLabel.setStyle("-fx-text-fill: #c20202");
        statusLabel.setText("Disconnected");
        appendConsole(console, "Disconnected");

        disconnectButton.setDisable(true);
        connectButton.setDisable(false);

        for (Button b : rpcButtons) b.setDisable(true);
    }

    private void callServer(Label statusLabel, TextArea console, String method, String message) {
        if (rpc == null) {
            statusLabel.setText("Not connected");
            statusLabel.setStyle("-fx-text-fill: #c20202");
            return;
        }

        rpcExecutor.execute(() -> {
            try {
                JsonNode resp = rpc.call(method, RpcClientHelper.paramsWithMessage(message));
                String serverMessage = resp.path("result").path("message").asText("Executed");

                Platform.runLater(() -> {
                    statusLabel.setText("Reply: " + resp);
                    appendConsole(console, serverMessage);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    appendConsole(console, "Error: " + e.getMessage());
                });
            }
        });
    }

    private void appendConsole(TextArea console, String line) {
        String current = console.getText();
        String next = current + (current.isEmpty() ? "" : "\n") + line;

        String[] lines = next.split("\n");
        if (lines.length > 100) {
            StringBuilder sb = new StringBuilder();
            for (int i = lines.length - 100; i < lines.length; i++) {
                sb.append(lines[i]).append("\n");
            }
            next = sb.toString().trim();
        }

        console.setText(next);

        // Always scroll to bottom
        console.positionCaret(console.getLength());
        console.setScrollTop(Double.MAX_VALUE);
    }

    private void shutdownClient() {
        // Close RPC connection
        if (rpc != null) {
            try { rpc.close(); } catch (Exception ignored) {}
            rpc = null;
        }

        // Stop background executor
        rpcExecutor.shutdownNow();

        // Close JavaFX application (but not the server)
        Platform.exit();
    }
}