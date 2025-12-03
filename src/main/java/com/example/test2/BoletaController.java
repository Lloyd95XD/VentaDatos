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
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class BoletaController implements Initializable {

    // ===== Ventana "VerBoletas.fxml" =====
    @FXML private TableView<BoletaItem> tablaBoletas;
    @FXML private TableColumn<BoletaItem, Integer> colIdBoleta;

    // ===== Compartido por ambas ventanas (VerBoletas y ZonadePagoRealizado) =====
    @FXML private TextArea txtBoleta;

    private final ObservableList<BoletaItem> listaBoletas =
            FXCollections.observableArrayList();

    /// Inicializa el controlador y configura la tabla si existe
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
    /// Establece el texto del area de texto de la boleta
    public void setTextoBoleta(String texto) {
        if (txtBoleta != null) {
            txtBoleta.setText(texto);
        }
    }

    // --------------------------------------------------------------------
    //  CONFIGURACIÓN DE LA VENTANA DE HISTORIAL
    // --------------------------------------------------------------------
    /// Configura las columnas de la tabla de boletas
    private void configurarTabla() {
        colIdBoleta.setCellValueFactory(new PropertyValueFactory<>("idBoleta"));
        tablaBoletas.setItems(listaBoletas);
    }

    /// Carga la lista de boletas desde la base de datos
    private void cargarBoletas() {
        listaBoletas.clear();

        final String sql = "{ CALL sp_listar_boletas() }";

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


    /// Configura el listener para detectar seleccion en la tabla
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

    /// Obtiene y muestra los detalles completos de una boleta
    private void mostrarBoletaCompleta(int idBoleta) {
        if (txtBoleta == null) return;

        String sqlVenta = "{ CALL sp_obtener_boleta_por_id(?) }";

        String sqlDetalles = "{ CALL sp_listar_detalles_boleta(?) }";

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

            int totalFinal = rsVenta.getInt("Precio_Total");     // total con descuento (si lo hubo)
            int descuento = rsVenta.getInt("Descuento");         // monto descontado
            boolean descEsNull = rsVenta.wasNull();
            if (descEsNull) descuento = 0;

            int totalBruto = totalFinal + descuento;

            String metodo = rsVenta.getString("Metodo_de_pago");
            String direccion = rsVenta.getString("Direccion");

            // RUT guardado como INT en BD → lo armamos como String y lo podemos formatear
            String rutCliente = null;
            int rutNum = rsVenta.getInt("Rut_Cliente");
            boolean rutEsNull = rsVenta.wasNull();
            if (!rutEsNull) {
                rutCliente = formatearRut(String.valueOf(rutNum));
            }

            // Nuevos datos de pago (pueden ser nulos en boletas antiguas)
            Integer montoEfectivo = null;
            int montoEf = rsVenta.getInt("Monto_Efectivo");
            boolean montoEfNull = rsVenta.wasNull();
            if (!montoEfNull) montoEfectivo = montoEf;

            Integer vuelto = null;
            int vto = rsVenta.getInt("Vuelto");
            boolean vtoNull = rsVenta.wasNull();
            if (!vtoNull) vuelto = vto;

            String marcaTarjeta = rsVenta.getString("Marca_Tarjeta");
            String ultimos4 = rsVenta.getString("Ultimos_4_Tarjeta");

            String sucursal = rsVenta.getString("localidad");

            // ----- Detalles -----
            stmtDet.setInt(1, idBoleta);
            ResultSet rsDet = stmtDet.executeQuery();

            StringBuilder sb = new StringBuilder();

            sb.append("      Boleta N° ").append(idBoleta).append("\n\n");
            sb.append("Fecha: ").append(fecha).append("\n");
            sb.append("Sucursal: ").append(sucursal != null ? sucursal : "-").append("\n");
            sb.append("Método de pago: ").append(metodo != null ? metodo : "-").append("\n");

            if (!rutEsNull && rutCliente != null) {
                sb.append("Rut cliente: ").append(rutCliente).append("\n");
            }
            if (direccion != null && !direccion.isEmpty()) {
                sb.append("Dirección: ").append(direccion).append("\n");
            }

            // ----- Datos específicos según metodo ----- //
            if (metodo != null) {
                String metodoLower = metodo.toLowerCase();

                // Efectivo: cuánto pagó y cuánto de vuelto
                if (metodoLower.contains("efectivo")) {
                    if (montoEfectivo != null) {
                        sb.append("Pago en efectivo: $ ")
                                .append(formatearCLP(montoEfectivo))
                                .append("\n");
                    }
                    if (vuelto != null) {
                        sb.append("Vuelto entregado: $ ")
                                .append(formatearCLP(vuelto))
                                .append("\n");
                    }
                }

                // Tarjeta: marca + últimos 4 dígitos si existen
                if (metodoLower.contains("visa") || metodoLower.contains("master")) {
                    if ((marcaTarjeta != null && !marcaTarjeta.isEmpty())
                            || (ultimos4 != null && !ultimos4.isEmpty())) {
                        sb.append("Tarjeta: ");
                        if (marcaTarjeta != null && !marcaTarjeta.isEmpty()) {
                            sb.append(marcaTarjeta).append(" ");
                        }
                        if (ultimos4 != null && !ultimos4.isEmpty()) {
                            sb.append("terminada en ").append(ultimos4);
                        }
                        sb.append("\n");
                    }
                }
            }

            sb.append("----------------------------------------\n");
            sb.append(String.format("%-4s %-20s %12s\n", "Cant", "Producto", "Subtotal"));
            sb.append("----------------------------------------\n");

            while (rsDet.next()) {
                int cant = rsDet.getInt("Cantidad_de_compras");
                String nombre = rsDet.getString("Nombre");
                int precio = rsDet.getInt("Precio");
                int subtotal = cant * precio;

                String subtotalStr = "$ " + formatearCLP(subtotal);

                sb.append(String.format("%-4d %-20s %12s\n",
                        cant,
                        nombre != null ? nombre : "-",
                        subtotalStr));
            }

            sb.append("----------------------------------------\n");
            sb.append(String.format("TOTAL BRUTO: %18s\n", "$ " + formatearCLP(totalBruto)));
            if (descuento > 0) {
                sb.append(String.format("DESCUENTO (10%%): %13s-\n", "$ " + formatearCLP(descuento)));
            }
            sb.append(String.format("TOTAL A PAGAR: %15s\n", "$ " + formatearCLP(totalFinal)));
            sb.append("\nGracias por su compra.\n");
            sb.append("      JOHEX.inc\n");
            txtBoleta.setText(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error", "No se pudo cargar el detalle de la boleta.");
        }
    }

    // Formatear CLP con puntos: 25990 -> 25.990
    private String formatearCLP(int valor) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CL"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(valor);
    }

    // Formatear RUT tipo 12345678K -> 12.345.678-K
    private String formatearRut(String digitos) {
        if (digitos == null || digitos.isEmpty()) return "";

        if (digitos.length() == 1) return digitos;

        String cuerpo = digitos.substring(0, digitos.length() - 1);
        String dv = digitos.substring(digitos.length() - 1);

        StringBuilder sb = new StringBuilder();
        int contador = 0;

        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            sb.insert(0, cuerpo.charAt(i));
            contador++;

            if (contador == 3 && i != 0) {
                sb.insert(0, ".");
                contador = 0;
            }
        }

        sb.append("-").append(dv);
        return sb.toString();
    }

    // --------------------------------------------------------------------
    //  BOTÓN VOLVER (para ambas ventanas)
    // --------------------------------------------------------------------
    /// Regresa a la ventana del menu principal
    @FXML
    private void volver(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("MenuiniciadasesionListoV2.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /// Muestra una alerta en pantalla
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /// Regresa a la ventana del menu principal (alternativa)
    @FXML
    private void volveralmenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MenuiniciadasesionListoV2.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
