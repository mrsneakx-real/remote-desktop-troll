package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.dashboard.DashboradA;

public class AppMain extends Application {

    private DashboradA dash;

    @Override
    public void start(Stage stage) {
        dash = new DashboradA();
        Scene scene = new Scene(dash.createView(), 700, 500);

        stage.setTitle("Client Dashboard");
        stage.setScene(scene);

        // Ensure closing the window actually shuts down background threads and sockets
        stage.setOnCloseRequest(evt -> dash.shutdownClient());

        stage.show();
    }

    @Override
    public void stop() {
        // Called by JavaFX when the app is exiting
        if (dash != null) dash.shutdownClient();
    }

    public static void main(String[] args) {
        launch(args);
    }
}