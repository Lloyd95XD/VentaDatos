package com.example.test2;

public class MueblesControlador {

    private int idProducto;
    private String categoria;
    private String nombre;
    private String descripcion;
    private int stock;
    private int precio;

    public MueblesControlador(int idProducto, String categoria, String nombre,
                    String descripcion, int stock, int precio) {
        this.idProducto = idProducto;
        this.categoria = categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.stock = stock;
        this.precio = precio;
    }
    //
    public int getIdProducto() { return idProducto; }
    public String getCategoria() { return categoria; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getStock() { return stock; }
    public int getPrecio() { return precio; }
}
