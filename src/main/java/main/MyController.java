package main;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import main.client.RpcClientHelper;

public class MyController {

    @FXML private TextField inputField;
    @FXML private Label statusLabel;

    private RpcClientHelper rpc;

    public void initialize() {
        try {
            rpc = new RpcClientHelper("127.0.0.1", 9000); // server on same PC
            statusLabel.setText("Connected");
        } catch (Exception e) {
            statusLabel.setText("Not connected: " + e.getMessage());
        }
    }

    @FXML
    private void onSendButtonClicked() {
        String text = inputField.getText();

        new Thread(() -> {
            try {
                JsonNode resp = rpc.call("insertText", RpcClientHelper.paramsWithMessage(text));

                Platform.runLater(() -> statusLabel.setText("Reply: " + resp.toString()));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }
}