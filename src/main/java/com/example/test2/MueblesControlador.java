package com.example.test2;

/// Clase que representa un mueblo dentro del sistema
public class MueblesControlador {

    /// ID unico del producto
    private int idProducto;

    /// Categoria del mueble
    private String categoria;

    /// Nombre del mueble
    private String nombre;

    /// Descripcion del mueble
    private String descripcion;

    /// Stock disponible
    private int stock;

    /// Precio del producto
    private int precio;

    /// Inicializacion de todos los atributos
    public MueblesControlador(int idProducto, String categoria, String nombre,
                    String descripcion, int stock, int precio) {
        this.idProducto = idProducto;
        this.categoria = categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.stock = stock;
        this.precio = precio;
    }

    /// Devuelve el ID del producto
    public int getIdProducto() { return idProducto; }

    /// Devuelve la categoria del producto
    public String getCategoria() { return categoria; }

    /// Devuelve el nombre del mueble
    public String getNombre() { return nombre; }

    /// Devuelve la descripcion
    public String getDescripcion() { return descripcion; }

    /// Devuelve la cantidad del stock
    public int getStock() { return stock; }

    /// Devuelve el precio del producto
    public int getPrecio() { return precio; }
}
