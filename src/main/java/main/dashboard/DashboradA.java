package main.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import main.client.RpcClientHelper;

public class DashboradA {

    private RpcClientHelper rpc;

    public Parent createView() {
        Label titleLabel = new Label("Admin Dashboard");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label statusLabel = new Label("Not connected");

        // ---- Server connection row (IP + connect + disconnect) ----
        Label serverIpLabel = new Label("Server IP:");
        TextField serverIpField = new TextField("127.0.0.1");
        serverIpField.setPromptText("e.g. 192.168.0.1");

        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");
        disconnectButton.setDisable(true);

        HBox connectionRow = new HBox(10, serverIpLabel, serverIpField, connectButton, disconnectButton);
        connectionRow.setPadding(new Insets(0, 0, 10, 0));

        // ---- Example fields ----
        Label label1 = new Label("Example1");
        TextField field1 = new TextField();
        field1.setPromptText("Example1");

        Label label2 = new Label("Example2");
        TextField field2 = new TextField();
        field2.setPromptText("Example2");

        Label label3 = new Label("Example3");
        TextField field3 = new TextField();
        field3.setPromptText("Example3");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        formGrid.add(label1, 0, 0);
        formGrid.add(field1, 1, 0);

        formGrid.add(label2, 0, 1);
        formGrid.add(field2, 1, 1);

        formGrid.add(label3, 0, 2);
        formGrid.add(field3, 1, 2);

        // ---- Buttons ----
        Button button1 = new Button("Example1");
        Button button2 = new Button("Example2");
        Button button3 = new Button("Example3");

        // Disable until connected
        button1.setDisable(true);
        button2.setDisable(true);
        button3.setDisable(true);

        VBox buttonBox = new VBox(10, button1, button2, button3);
        buttonBox.setPrefWidth(120);

        HBox mainLayout = new HBox(20, formGrid, buttonBox);
        VBox root = new VBox(15, titleLabel, connectionRow, statusLabel, mainLayout);
        root.setPadding(new Insets(20));

        // Connect action
        connectButton.setOnAction(evt -> {
            String ip = serverIpField.getText().trim();
            connect(statusLabel, ip, 9000, connectButton, disconnectButton, button1, button2, button3);
        });

        // Disconnect action
        disconnectButton.setOnAction(evt ->
                disconnect(statusLabel, connectButton, disconnectButton, button1, button2, button3)
        );

        // Enter in IP field triggers connect
        serverIpField.setOnAction(evt -> connectButton.fire());

        // Button handlers (work only when connected)
        button1.setOnAction(evt -> callServer(statusLabel, "insertText", field1.getText()));
        button2.setOnAction(evt -> callServer(statusLabel, "insertText", field2.getText()));
        button3.setOnAction(evt -> callServer(statusLabel, "insertText", field3.getText()));

        return root;
    }

    private void connect(Label statusLabel,
                         String host,
                         int port,
                         Button connectButton,
                         Button disconnectButton,
                         Button... rpcButtons) {

        statusLabel.setText("Connecting to " + host + ":" + port + " ...");
        statusLabel.setStyle("-fx-text-fill: #ffb433");
        connectButton.setDisable(true);

        new Thread(() -> {
            try {
                // Close old connection if present
                RpcClientHelper old = rpc;
                if (old != null) {
                    try { old.close(); } catch (Exception ignored) {}
                }

                rpc = new RpcClientHelper(host, port);

                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #11c214");
                    statusLabel.setText("Connected to " + host + ":" + port);

                    disconnectButton.setDisable(false);
                    connectButton.setDisable(false);

                    for (Button b : rpcButtons) b.setDisable(false);
                });
            } catch (Exception e) {
                rpc = null;
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #c20202");
                    statusLabel.setText("Connection failed: " + e.getMessage());

                    disconnectButton.setDisable(true);
                    connectButton.setDisable(false);

                    for (Button b : rpcButtons) b.setDisable(true);
                });
            }
        }, "rpc-connect-thread").start();
    }

    private void disconnect(Label statusLabel,
                            Button connectButton,
                            Button disconnectButton,
                            Button... rpcButtons) {

        // Close immediately on UI thread (fast), but catch errors
        RpcClientHelper old = rpc;
        rpc = null;

        if (old != null) {
            try { old.close(); } catch (Exception ignored) {}
        }

        statusLabel.setStyle("-fx-text-fill: #c20202");
        statusLabel.setText("Disconnected");

        disconnectButton.setDisable(true);
        connectButton.setDisable(false);

        for (Button b : rpcButtons) b.setDisable(true);
    }

    private void callServer(Label statusLabel, String method, String message) {
        if (rpc == null) {
            statusLabel.setText("Not connected");
            statusLabel.setStyle("-fx-text-fill: #c20202");
            return;
        }

        new Thread(() -> {
            try {
                JsonNode resp = rpc.call(method, RpcClientHelper.paramsWithMessage(message));
                Platform.runLater(() -> statusLabel.setText("Reply: " + resp));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
            }
        }, "rpc-call-thread").start();
    }
}