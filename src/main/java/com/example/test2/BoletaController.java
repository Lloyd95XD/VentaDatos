package com.example.test2;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BoletaController implements Initializable {

    // ===== Ventana "VerBoletas.fxml" =====
    @FXML private TableView<BoletaItem> tablaBoletas;
    @FXML private TableColumn<BoletaItem, Integer> colIdBoleta;

    // ===== Compartido por ambas ventanas (VerBoletas y ZonadePagoRealizado) =====
    @FXML private TextArea txtBoleta;

    private final ObservableList<BoletaItem> listaBoletas =
            FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Si tablaBoletas es null, significa que estamos en la ventana de "Boleta simple"
        if (tablaBoletas != null) {
            configurarTabla();
            cargarBoletas();
            configurarSeleccion();
        }
    }

    // --------------------------------------------------------------------
    //  USADO POR ZONAPAGOCONTROLLER PARA MOSTRAR UNA BOLETA CONCRETA
    // --------------------------------------------------------------------
    public void setTextoBoleta(String texto) {
        if (txtBoleta != null) {
            txtBoleta.setText(texto);
        }
    }

    // --------------------------------------------------------------------
    //  CONFIGURACIÓN DE LA VENTANA DE HISTORIAL
    // --------------------------------------------------------------------
    private void configurarTabla() {
        colIdBoleta.setCellValueFactory(new PropertyValueFactory<>("idBoleta"));
        tablaBoletas.setItems(listaBoletas);
    }

    private void cargarBoletas() {
        listaBoletas.clear();

        String sql = "SELECT Id_Boleta FROM venta ORDER BY Id_Boleta DESC";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("Id_Boleta");
                listaBoletas.add(new BoletaItem(id));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error", "No se pudieron cargar las boletas.");
        }
    }

    private void configurarSeleccion() {
        tablaBoletas.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, vieja, nueva) -> {
                    if (nueva != null) {
                        mostrarBoletaCompleta(nueva.getIdBoleta());
                    } else if (txtBoleta != null) {
                        txtBoleta.clear();
                    }
                });
    }

    private void mostrarBoletaCompleta(int idBoleta) {
        if (txtBoleta == null) return;

        String sqlVenta = """
                SELECT v.Id_Boleta,
                       v.Hora_de_venta,
                       v.Precio_Total,
                       v.Metodo_de_pago,
                       v.Direccion,
                       v.Rut_Cliente,
                       s.localidad
                FROM venta v
                LEFT JOIN sucursales s ON v.Id_Sucursales = s.Id_Sucursales
                WHERE v.Id_Boleta = ?
                """;

        String sqlDetalles = """
                SELECT d.Cantidad_de_compras,
                       p.Nombre,
                       p.Precio
                FROM detalles_ventas d
                JOIN producto p ON p.Id_Producto = d.Id_Producto
                WHERE d.Id_Boleta = ?
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmtVenta = conn.prepareStatement(sqlVenta);
             PreparedStatement stmtDet = conn.prepareStatement(sqlDetalles)) {

            // ----- Datos de la venta -----
            stmtVenta.setInt(1, idBoleta);
            ResultSet rsVenta = stmtVenta.executeQuery();

            if (!rsVenta.next()) {
                txtBoleta.setText("No se encontraron datos para la boleta " + idBoleta);
                return;
            }

            String fecha = "";
            if (rsVenta.getTimestamp("Hora_de_venta") != null) {
                fecha = rsVenta.getTimestamp("Hora_de_venta")
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }

            int total = rsVenta.getInt("Precio_Total");
            String metodo = rsVenta.getString("Metodo_de_pago");
            String direccion = rsVenta.getString("Direccion");
            int rutCliente = rsVenta.getInt("Rut_Cliente");
            boolean rutEsNull = rsVenta.wasNull();
            String sucursal = rsVenta.getString("localidad");

            // ----- Detalles -----
            stmtDet.setInt(1, idBoleta);
            ResultSet rsDet = stmtDet.executeQuery();

            StringBuilder sb = new StringBuilder();

            sb.append("      Boleta N° ").append(idBoleta).append("\n\n");
            sb.append("Fecha: ").append(fecha).append("\n");
            sb.append("Sucursal: ").append(sucursal != null ? sucursal : "-").append("\n");
            sb.append("Método de pago: ").append(metodo != null ? metodo : "-").append("\n");
            if (!rutEsNull) {
                sb.append("Rut cliente: ").append(rutCliente).append("\n");
            }
            if (direccion != null && !direccion.isEmpty()) {
                sb.append("Dirección: ").append(direccion).append("\n");
            }
            sb.append("----------------------------------------\n");
            sb.append(String.format("%-4s %-20s %8s\n", "Cant", "Producto", "Subtotal"));
            sb.append("----------------------------------------\n");

            while (rsDet.next()) {
                int cant = rsDet.getInt("Cantidad_de_compras");
                String nombre = rsDet.getString("Nombre");
                int precio = rsDet.getInt("Precio");
                int subtotal = cant * precio;

                sb.append(String.format("%-4d %-20s %8d\n",
                        cant,
                        nombre != null ? nombre : "-",
                        subtotal));
            }

            sb.append("----------------------------------------\n");
            sb.append(String.format("TOTAL: %28d\n", total));
            sb.append("\nGracias por su compra.\n");
            sb.append("      JOHEX.inc\n");
            txtBoleta.setText(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error", "No se pudo cargar el detalle de la boleta.");
        }
    }

    // --------------------------------------------------------------------
    //  BOTÓN VOLVER (para ambas ventanas)
    // --------------------------------------------------------------------
    @FXML
    private void volver(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("VentanaIniciadaLista.fxml")
            );
            Parent root = loader.load();

            // Obtener el stage actual desde el botón presionado
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void volveralmenu(ActionEvent event) {
        try {
            // Cargar la nueva ventana
            FXMLLoader loader = new FXMLLoader(getClass().getResource("VentanaIniciadalista.fxml"));
            Parent root = loader.load();

            // Obtener la ventana actual desde el botón (o control) que disparó el evento
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Reemplazar la escena
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
