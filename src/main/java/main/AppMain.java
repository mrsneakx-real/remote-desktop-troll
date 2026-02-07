package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.dashboard.Dashborad;

public class AppMain extends Application {

    private Dashborad dash;

    @Override
    public void start(Stage stage) throws Exception {
        dash = new Dashborad();
        Scene scene = new Scene(dash.createView(), 800, 700);

        scene.getStylesheets().add(getClass().getResource("/dashboard/dashboard.css").toExternalForm());

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