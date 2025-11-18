package com.example.test2;

public class UsuarioSesion {

    private static int idUsuario;
    private static String nombre;
    private static boolean esAdmin;
    private static boolean haySesion = false;

    public static void setSesion(int id, String nom, boolean admin) {
        idUsuario = id;
        nombre = nom;
        esAdmin = admin;
        haySesion = true;
    }

    public static int getIdUsuario() {
        return idUsuario;
    }

    public static String getNombre() {
        return nombre;
    }

    public static boolean isAdmin() {
        return esAdmin;
    }

    public static boolean haySesion() {
        return haySesion;
    }

    public static void cerrarSesion() {
        haySesion = false;
        idUsuario = 0;
        nombre = null;
        esAdmin = false;
    }
}
