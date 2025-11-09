// ProdInvRow.java
package com.example.test2;

public class ProdInvRow {

    private int idProducto;
    private String nombre;
    private String descripcion;
    private Integer stock;
    private String historialMovimiento;
    private String editarSucursales;

    // --- Constructor ---
    public ProdInvRow(int idProducto, String nombre, String descripcion,
                      Integer stock, String historialMovimiento, String editarSucursales) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.stock = stock;
        this.historialMovimiento = historialMovimiento;
        this.editarSucursales = editarSucursales;
    }

    // --- Getters ---
    public int getIdProducto() { return idProducto; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Integer getStock() { return stock; }
    public String getHistorialMovimiento() { return historialMovimiento; }
    public String getEditarSucursales() { return editarSucursales; }

    // --- Setters ---
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setHistorialMovimiento(String historialMovimiento) { this.historialMovimiento = historialMovimiento; }
    public void setEditarSucursales(String editarSucursales) { this.editarSucursales = editarSucursales; }
}
