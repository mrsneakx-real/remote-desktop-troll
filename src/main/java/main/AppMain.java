package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.dashboard.DashboradA;

public class AppMain extends Application {

    @Override
    public void start(Stage stage) {
        DashboradA dash = new DashboradA();
        Scene scene = new Scene(dash.createView(), 700, 300);

        stage.setTitle("Client Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}