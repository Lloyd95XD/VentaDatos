package com.example.test2;

///Importaciones JavaFX

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/// Clase que inicia la aplicacion de JavaFX
public class HelloApplication extends Application {

    ///  Metodo que carga la ventana inicial
    @Override
    public void start(Stage stage) throws IOException {

        /// Carga archivo FXML de la ventana login
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("VentanaLoginV2.fxml"));

        /// Crea la ventana con tamaño de 512x512
        Scene scene = new Scene(fxmlLoader.load(), 512, 512);

        /// Titulo de la ventana
        stage.setTitle("Gestion para la tienda");

        /// Escena de la ventana
        stage.setScene(scene);

        /// Muestra la ventana
        stage.show();

        /// Conexion con base de datos
       ConexionBD.conectar();//
    }
}
