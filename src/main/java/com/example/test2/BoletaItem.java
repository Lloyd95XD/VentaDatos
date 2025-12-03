package com.example.test2;

public class BoletaItem {
    private int idBoleta;

    /// Constructor de la clase BoletaItem
    public BoletaItem(int idBoleta) {
        this.idBoleta = idBoleta;
    }

    /// Obtiene el ID de la boleta
    public int getIdBoleta() {
        return idBoleta;
    }

    /// Establece el ID de la boleta
    public void setIdBoleta(int idBoleta) {
        this.idBoleta = idBoleta;
    }
}
