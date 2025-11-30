package com.example.test2;

/// Representa una sucursal que se muestra en una lista o ComboBox
public class SucursalItem {

    /// ID de sucursal
    private final int id;

    /// Nombre de la sucursal
    private final String nombre;

    /// Inicializa el ID y nombre
    public SucursalItem(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
//
    /// Obtiene el ID de la sucursal
    public int getId() { return id; }

    /// Obtiene el nombre de la sucursal
    public String getNombre() { return nombre; }

    @Override
    public String toString() {
        /// Esto será lo que se ve en el ComboBox
        return nombre;
    }
}

