package com.example.test2;

/// Clase que maneja los datos de la sesion actual de el usuario.
public class UsuarioSesion {

    /// ID del usuario en la sesion actual
    private static String idUsuario;

    /// Nombre del usuario en la sesion actual
    private static String nombre;

    /// Indica si el usuario tiene el rol de administrador
    private static boolean esAdmin;

    /// Indica si existe una sesion activa
    private static boolean haySesion = false;

    /// Configura los datos de la sesion al iniciar sesion
    public static void setSesion(String id, String nom, boolean admin) {
        idUsuario = id;
        nombre = nom;
        esAdmin = admin;
        haySesion = true;
    }

    /// Obtiene el ID del usuario en la sesion actual
    public static String getIdUsuario() {
        return idUsuario;
    }

    /// Obtiene el nombre del usuario en la sesion actual
    public static String getNombre() {
        return nombre;
    }

    /// Indica si el usuario en la sesion actual es administrador
    public static boolean isAdmin() {
        return esAdmin;
    }

    /// Devuelve true si hay sesion activa
    public static boolean haySesion() {
        return haySesion;
    }
//
    /// cierra la sesion y limpia los datos almacenados
    public static void cerrarSesion() {
        haySesion = false;
        idUsuario = null;
        nombre = null;
        esAdmin = false;
    }
}
