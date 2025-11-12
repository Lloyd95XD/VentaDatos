package com.example.test2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("tu_vista.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 512, 512);
        stage.setTitle("Gestion para la tienda");
        stage.setScene(scene);
        stage.show();
        /// joooooooooooooooooooooooooooorgiiiiiiiiiiiiiiiiiitooooooooooooooooooooo
        /// joooooooooooooooooooooooooooorgiiiiiiiiiiiiiiiiiitooooooooooooooooooooo
        /// joooooooooooooooooooooooooooorgiiiiiiiiiiiiiiiiiitooooooooooooooooooooo
        /// joooooooooooooooooooooooooooorgiiiiiiiiiiiiiiiiiitooooooooooooooooooooo
        /// joooooooooooooooooooooooooooorgiiiiiiiiiiiiiiiiiitooooooooooooooooooooo
        /// joooooooooooooooooooooooooooorgiiiiiiiiiiiiiiiiiitooooooooooooooooooooo
        /// joooooooooooooooooooooooooooorgiiiiiiiiiiiiiiiiiitooooooooooooooooooooo
        /// joooooooooooooooooooooooooooorgiiiiiiiiiiiiiiiiiitooooooooooooooooooooo

       ConexionBD.conectar();
    }
}
