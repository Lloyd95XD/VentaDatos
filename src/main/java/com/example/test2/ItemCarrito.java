package com.example.test2;

public class ItemCarrito {
    //
    private int idProducto;
    private String nombre;
    private int precio;
    private int cantidad;

    public ItemCarrito(int idProducto, String nombre, int precio, int cantidad) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    public int getIdProducto() { return idProducto; }
    public String getNombre() { return nombre; }
    public int getPrecio() { return precio; }
    public int getCantidad() { return cantidad; }

    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
