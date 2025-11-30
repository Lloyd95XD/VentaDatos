package com.example.test2;

public class DatosControlador {

    private String idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String password;
    private String fechaCreacion;
    private int admin;
    private int idRol;
    private int idSucursal;

    private String nombreRol;
    private String nombreSucursal;

    // 🔹 Nuevo campo
    private int suspendido;

    // Constructor COMPLETO para AdminUsuarios
    public DatosControlador(
            String idUsuario, String nombre, String apellido,
            String email, String telefono, String password,
            String fechaCreacion, int admin,
            int idRol, int idSucursal,
            String nombreRol, String nombreSucursal,
            int suspendido
    ) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.password = password;
        this.fechaCreacion = fechaCreacion;
        this.admin = admin;
        this.idRol = idRol;
        this.idSucursal = idSucursal;
        this.nombreRol = nombreRol;
        this.nombreSucursal = nombreSucursal;
        this.suspendido = suspendido;
    }

    // ==== GETTERS & SETTERS ====

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public int getAdmin() { return admin; }
    public void setAdmin(int admin) { this.admin = admin; }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }

    public int getIdSucursal() { return idSucursal; }
    public void setIdSucursal(int idSucursal) { this.idSucursal = idSucursal; }

    public String getNombreRol() { return nombreRol; }
    public void setNombreRol(String nombreRol) { this.nombreRol = nombreRol; }
//
    public String getNombreSucursal() { return nombreSucursal; }
    public void setNombreSucursal(String nombreSucursal) { this.nombreSucursal = nombreSucursal; }

    // 🔹 Suspendido
    public int getSuspendido() { return suspendido; }
    public void setSuspendido(int suspendido) { this.suspendido = suspendido; }
}
