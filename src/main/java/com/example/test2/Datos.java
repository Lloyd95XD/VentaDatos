package com.example.test2;

public class Datos {
    private int idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;

    // Campos admin
    private String rol;           // admin.Rol
    private String descripcion;   // admin.Descripcion
    private Boolean verificador;  // admin.Verificador (tinyint -> Boolean)

    // Password (HASH almacenado en BD)
    private String password;      // usuario.Password

    // Constructor corto
    public Datos(int idUsuario, String nombre, String apellido, String email, String telefono) {
        this(idUsuario, nombre, apellido, email, telefono, null, null, null, null);
    }

    // Constructor completo
    public Datos(int idUsuario, String nombre, String apellido, String email, String telefono,
                 String rol, String descripcion, Boolean verificador, String password) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.rol = rol;
        this.descripcion = descripcion;
        this.verificador = verificador;
        this.password = password;
    }

    // Getters
    public int getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getRol() { return rol; }
    public String getDescripcion() { return descripcion; }
    public Boolean getVerificador() { return verificador; }
    public String getPassword() { return password; }

    // Setters
    public void setNombre(String v) { this.nombre = v; }
    public void setApellido(String v) { this.apellido = v; }
    public void setEmail(String v) { this.email = v; }
    public void setTelefono(String v) { this.telefono = v; }
    public void setRol(String v) { this.rol = v; }
    public void setDescripcion(String v) { this.descripcion = v; }
    public void setVerificador(Boolean v) { this.verificador = v; }
    public void setPassword(String v) { this.password = v; }

    // --- Fila para tabla inventario ---
    public static class ProdInvRow {
        private int idProducto;
        private String nombre;
        private String descripcion;
        private Integer stock;
        private String historialMovimiento;
        private String editarSucursales;

        public ProdInvRow(int idProducto, String nombre, String descripcion,
                          Integer stock, String historialMovimiento, String editarSucursales) {
            this.idProducto = idProducto;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.stock = stock;
            this.historialMovimiento = historialMovimiento;
            this.editarSucursales = editarSucursales;
        }

        public int getIdProducto() { return idProducto; }
        public String getNombre() { return nombre; }
        public String getDescripcion() { return descripcion; }
        public Integer getStock() { return stock; }
        public String getHistorialMovimiento() { return historialMovimiento; }
        public String getEditarSucursales() { return editarSucursales; }

        public void setNombre(String v) { this.nombre = v; }
        public void setDescripcion(String v) { this.descripcion = v; }
        public void setStock(Integer v) { this.stock = v; }
        public void setHistorialMovimiento(String v) { this.historialMovimiento = v; }
        public void setEditarSucursales(String v) { this.editarSucursales = v; }
    }
}
