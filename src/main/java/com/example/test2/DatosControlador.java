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

    /// Nuevo campo
    private int suspendido;

    /// Constructor COMPLETO para AdminUsuarios
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

    /// Obtiene el ID del usuario
    public String getIdUsuario() { return idUsuario; }
    /// Establece el ID del usuario
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    /// Obtiene el nombre del usuario
    public String getNombre() { return nombre; }
    /// Establece el nombre del usuario
    public void setNombre(String nombre) { this.nombre = nombre; }

    /// Obtiene el apellido del usuario
    public String getApellido() { return apellido; }
    /// Establece el apellido del usuario
    public void setApellido(String apellido) { this.apellido = apellido; }

    /// Obtiene el email del usuario
    public String getEmail() { return email; }
    /// Establece el email del usuario
    public void setEmail(String email) { this.email = email; }

    /// Obtiene el telefono del usuario
    public String getTelefono() { return telefono; }
    /// Establece el telefono del usuario
    public void setTelefono(String telefono) { this.telefono = telefono; }

    /// Obtiene la contraseña del usuario
    public String getPassword() { return password; }
    /// Establece la contraseña del usuario
    public void setPassword(String password) { this.password = password; }

    /// Obtiene la fecha de creacion de la cuenta
    public String getFechaCreacion() { return fechaCreacion; }
    /// Establece la fecha de creacion de la cuenta
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    /// Obtiene el estado de administrador
    public int getAdmin() { return admin; }
    /// Establece el estado de administrador
    public void setAdmin(int admin) { this.admin = admin; }

    /// Obtiene el ID del rol
    public int getIdRol() { return idRol; }
    /// Establece el ID del rol
    public void setIdRol(int idRol) { this.idRol = idRol; }

    /// Obtiene el ID de la sucursal
    public int getIdSucursal() { return idSucursal; }
    /// Establece el ID de la sucursal
    public void setIdSucursal(int idSucursal) { this.idSucursal = idSucursal; }

    /// Obtiene el nombre del rol
    public String getNombreRol() { return nombreRol; }
    /// Establece el nombre del rol
    public void setNombreRol(String nombreRol) { this.nombreRol = nombreRol; }

    /// Obtiene el nombre de la sucursal
    public String getNombreSucursal() { return nombreSucursal; }
    /// Establece el nombre de la sucursal
    public void setNombreSucursal(String nombreSucursal) { this.nombreSucursal = nombreSucursal; }

    /// Obtiene el estado de suspension
    public int getSuspendido() { return suspendido; }
    /// Establece el estado de suspension
    public void setSuspendido(int suspendido) { this.suspendido = suspendido; }
}
