package com.example.test2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ConexionBD {
    private static final String URL  = "jdbc:mysql://localhost:3306/gestor_de_ventas?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = ""; // sin contraseña (XAMPP por defecto)

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            System.out.println("❌ Conexión fallida: " + e.getMessage());
            return null;
        }
    }
}
