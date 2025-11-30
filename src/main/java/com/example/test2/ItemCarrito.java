package com.example.test2;

/// Clase que representa un item en un carrito de compras
public class ItemCarrito {

    /// Identificador de producto
    private int idProducto;

    /// Nombre del Producto
    private String nombre;

    /// Precio por unidad del producto
    private int precio;

    /// Cantidad de unidades del producto en el carrito
    private int cantidad;

//
    /// Constructor que inicializa todos los campos del item
    public ItemCarrito(int idProducto, String nombre, int precio, int cantidad) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    /// Devuelve la ID del producto
    public int getIdProducto() { return idProducto; }

    /// Devuelve el Nombre del producto
    public String getNombre() { return nombre; }

    /// Devuelve el precio
    public int getPrecio() { return precio; }

    /// Devuelve cuantas unidades hay en el carrito
    public int getCantidad() { return cantidad; }

    /// Modifica la cantidad del item en el carrito
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
