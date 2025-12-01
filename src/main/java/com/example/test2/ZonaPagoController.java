package com.example.test2;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ZonaPagoController implements Initializable {

    // ======== TABLA CARRITO ========
    @FXML private TableView<ItemCarrito> tablaCarrito;
    @FXML private TableColumn<ItemCarrito, String>  colNombreCarrito;
    @FXML private TableColumn<ItemCarrito, Integer> colPrecioCarrito;
    @FXML private TableColumn<ItemCarrito, Integer> colCantidadCarrito;

    @FXML private TextArea txtDescripcion;

    private final ObservableList<ItemCarrito> carrito =
            FXCollections.observableArrayList();

    // ======== ZONA DERECHA ========
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private TextField txtRutCliente;
    @FXML private TextField txtDireccion;
    @FXML private Text lblMontoTotal;

    private int totalAPagar = 0;

    // Usuario logueado y sucursal asociada a su cuenta
    private String idUsuario = "";
    private int idSucursalUsuario = -1;
    private String nombreSucursalUsuario = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarTablaCarrito();
        configurarDescripcion();
        cargarMetodosPago();
        cargarDatosUsuarioYSucursal();
        configurarFormatoRutCliente();   // <-- NUEVO: formateo del RUT
        actualizarTextoTotal();
    }

    // ==========================
    //  Setters desde ventana anterior
    // ==========================
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = String.valueOf(idUsuario);
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public void setCarritoYTotal(ObservableList<ItemCarrito> carritoOrigen, int total) {
        carrito.clear();
        carrito.addAll(carritoOrigen);
        tablaCarrito.setItems(carrito);
        totalAPagar = total;
        actualizarTextoTotal();
    }

    private void configurarTablaCarrito() {
        colNombreCarrito.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecioCarrito.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colCantidadCarrito.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        tablaCarrito.setItems(carrito);
    }

    private void configurarDescripcion() {
        tablaCarrito.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, viejo, nuevo) -> {
                    if (nuevo == null) {
                        txtDescripcion.clear();
                        return;
                    }

                    txtDescripcion.setText(
                            "Producto: " + nuevo.getNombre() +
                                    "\nCantidad: " + nuevo.getCantidad() +
                                    "\nPrecio unidad: " + nuevo.getPrecio()
                    );
                });
    }

    private void cargarMetodosPago() {
        comboMetodoPago.setItems(FXCollections.observableArrayList(
                "Débito",
                "Crédito",
                "Efectivo"
        ));
    }

    // ==========================
    // FORMATEO AUTOMÁTICO RUT CLIENTE
    // ==========================
    private void configurarFormatoRutCliente() {
        if (txtRutCliente == null) return;

        final boolean[] actualizando = { false };

        txtRutCliente.textProperty().addListener((obs, oldValue, newValue) -> {
            if (actualizando[0]) return;
            actualizando[0] = true;

            // Solo dígitos
            String soloDigitos = newValue.replaceAll("\\D", "");
            if (soloDigitos.length() > 9) {
                soloDigitos = soloDigitos.substring(0, 9);
            }

            String formateado = formatearRut(soloDigitos);
            txtRutCliente.setText(formateado);
            txtRutCliente.positionCaret(formateado.length());

            actualizando[0] = false;
        });
    }

    // 12345678K -> 12.345.678-K
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

    // ==========================
    // DATOS DE USUARIO / SUCURSAL
    // ==========================
    private void cargarDatosUsuarioYSucursal() {
        String idUsuarioSesion = UsuarioSesion.getIdUsuario();

        if (idUsuarioSesion == null || idUsuarioSesion.trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Sesión",
                    "No se encontró un usuario logueado. Inicie sesión nuevamente.");
            return;
        }

        idUsuario = idUsuarioSesion;

        String sql = """
                SELECT u.Id_Sucursales, s.localidad
                FROM Usuario u
                LEFT JOIN sucursales s ON u.Id_Sucursales = s.Id_Sucursales
                WHERE u.Id_Usuario = ?
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idUsuario);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    idSucursalUsuario = rs.getInt("Id_Sucursales");
                    nombreSucursalUsuario = rs.getString("localidad");

                    if (rs.wasNull() || idSucursalUsuario == 0 || nombreSucursalUsuario == null) {
                        mostrarAlerta(Alert.AlertType.WARNING,
                                "Sucursal no definida",
                                "Tu cuenta no tiene una sucursal asociada.\n" +
                                        "Por favor, contacta al administrador.");
                    }
                } else {
                    mostrarAlerta(Alert.AlertType.ERROR,
                            "Usuario no encontrado",
                            "No se pudo encontrar el usuario en la base de datos.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo cargar la sucursal del usuario.");
        }
    }

    private void actualizarTextoTotal() {
        if (lblMontoTotal != null) {
            lblMontoTotal.setText("Monto Total $ " + totalAPagar);
        }
    }

    // ==========================
    //   REALIZAR PAGO
    // ==========================
    @FXML
    private void realizarPago() {
        if (carrito.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Carrito vacío", "No hay productos para pagar.");
            return;
        }

        String metodo = comboMetodoPago.getValue();
        String direccion = txtDireccion.getText().trim();
        String rutFormateado = txtRutCliente.getText().trim();      // con puntos y guion
        String rutSoloDigitos = rutFormateado.replaceAll("\\D", ""); // solo números

        if (idSucursalUsuario <= 0 || nombreSucursalUsuario == null || nombreSucursalUsuario.isEmpty()) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Sucursal no definida",
                    "Tu cuenta no tiene una sucursal asociada.\nNo se puede realizar la venta.");
            return;
        }

        if (metodo == null || metodo.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Datos faltantes", "Selecciona un método de pago.");
            return;
        }

        Integer rutCliente = null;
        if (!rutSoloDigitos.isEmpty()) {
            try {
                rutCliente = Integer.parseInt(rutSoloDigitos);
            } catch (NumberFormatException e) {
                mostrarAlerta(Alert.AlertType.WARNING,
                        "RUT inválido", "El RUT debe ser numérico (sin puntos ni guion).");
                return;
            }
        }

        // ===== DESCUENTO POR RUT (segunda compra o más) =====
        int totalVenta = totalAPagar;
        int descuentoAplicado = 0;

        if (rutCliente != null && clienteYaComproAntes(rutCliente)) {
            descuentoAplicado = (int) Math.round(totalAPagar * 0.10);  // 10%
            totalVenta = totalAPagar - descuentoAplicado;
        }

        // ==========================
        // Insertar en VENTA
        // ==========================
        String sqlVenta = """
            INSERT INTO venta
            (Id_Usuario, Id_Sucursales, Precio_Total, Metodo_de_pago, Direccion, Rut_Cliente, Descuento)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        int idBoletaGenerada = -1;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmtVenta = conn.prepareStatement(
                     sqlVenta, Statement.RETURN_GENERATED_KEYS)) {

            stmtVenta.setString(1, idUsuario);
            stmtVenta.setInt(2, idSucursalUsuario);
            stmtVenta.setInt(3, totalVenta);              // total con descuento (si aplica)
            stmtVenta.setString(4, metodo);
            stmtVenta.setString(5, direccion.isEmpty() ? null : direccion);

            if (rutCliente == null) {
                stmtVenta.setNull(6, Types.INTEGER);
            } else {
                stmtVenta.setInt(6, rutCliente);
            }

            stmtVenta.setInt(7, descuentoAplicado);       // monto descontado (0 si no hay promo)

            stmtVenta.executeUpdate();

            try (ResultSet keys = stmtVenta.getGeneratedKeys()) {
                if (keys.next()) {
                    idBoletaGenerada = keys.getInt(1);
                }
            }

            if (idBoletaGenerada == -1) {
                throw new Exception("No se pudo obtener el Id_Boleta generado.");
            }

            // ==========================
            // DETALLES_VENTAS
            // ==========================
            String sqlDetalles = """
                INSERT INTO detalles_ventas
                (Id_Boleta, Id_Producto, Cantidad_de_compras)
                VALUES (?, ?, ?)
                """;

            try (PreparedStatement stmtDet = conn.prepareStatement(sqlDetalles)) {
                for (ItemCarrito item : carrito) {
                    stmtDet.setInt(1, idBoletaGenerada);
                    stmtDet.setInt(2, item.getIdProducto());
                    stmtDet.setInt(3, item.getCantidad());
                    stmtDet.addBatch();
                }
                stmtDet.executeBatch();
            }

            // ==========================
            // ACTUALIZAR STOCK
            // ==========================
            String sqlUpdateStock = """
                UPDATE producto
                SET Stock = Stock - ?
                WHERE Id_Producto = ?
                """;
            try (PreparedStatement stmtStock = conn.prepareStatement(sqlUpdateStock)) {
                for (ItemCarrito item : carrito) {
                    stmtStock.setInt(1, item.getCantidad());
                    stmtStock.setInt(2, item.getIdProducto());
                    stmtStock.addBatch();
                }
                stmtStock.executeBatch();
            }

            // Boleta final
            abrirVentanaBoleta(
                    idBoletaGenerada,
                    nombreSucursalUsuario,
                    metodo,
                    rutFormateado,        // se muestra formateado en la boleta
                    direccion,
                    totalAPagar,          // total original
                    descuentoAplicado,
                    totalVenta,           // total final
                    carrito
            );

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error al pagar", "Ocurrió un error al procesar el pago.");
        }
    }

    // ==========================
    //  ¿Cliente ya compró antes?
    // ==========================
    private boolean clienteYaComproAntes(int rutCliente) {
        String sql = "SELECT COUNT(*) AS total FROM venta WHERE Rut_Cliente = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rutCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    // Si ya tiene al menos 1 compra previa → esta es la segunda o más
                    return total >= 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Si hay error, no aplicamos descuento por seguridad
        }
        return false;
    }

    // ==========================
    //   ABRIR VENTANA BOLETA
    // ==========================
    private void abrirVentanaBoleta(int idBoleta,
                                    String nombreSucursal,
                                    String metodoPago,
                                    String rut,
                                    String direccion,
                                    int totalOriginal,
                                    int descuento,
                                    int totalFinal,
                                    ObservableList<ItemCarrito> carrito) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ZonadePagoRealizado.fxml"));
            Parent root = loader.load();

            BoletaController controller = loader.getController();

            String textoBoleta = construirTextoBoleta(
                    idBoleta,
                    nombreSucursal,
                    metodoPago,
                    rut,
                    direccion,
                    totalOriginal,
                    descuento,
                    totalFinal,
                    carrito
            );

            controller.setTextoBoleta(textoBoleta);

            Stage stage = (Stage) lblMontoTotal.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error", "No se pudo abrir la boleta.");
        }
    }

    private String construirTextoBoleta(int idBoleta,
                                        String nombreSucursal,
                                        String metodoPago,
                                        String rut,
                                        String direccion,
                                        int totalOriginal,
                                        int descuento,
                                        int totalFinal,
                                        ObservableList<ItemCarrito> carrito) {

        String fecha = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        StringBuilder sb = new StringBuilder();

        sb.append("      Boleta N° ").append(idBoleta).append("\n\n");
        sb.append("Fecha: ").append(fecha).append("\n");
        sb.append("Sucursal: ").append(nombreSucursal != null ? nombreSucursal : "Sin sucursal").append("\n");
        sb.append("Método de pago: ").append(metodoPago).append("\n");
        if (rut != null && !rut.isEmpty())
            sb.append("Rut cliente: ").append(rut).append("\n");
        if (direccion != null && !direccion.isEmpty())
            sb.append("Dirección: ").append(direccion).append("\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-4s %-20s %8s\n", "Cant", "Producto", "Subtotal"));
        sb.append("----------------------------------------\n");

        for (ItemCarrito item : carrito) {
            int subtotal = item.getCantidad() * item.getPrecio();
            sb.append(String.format("%-4d %-20s %8d\n",
                    item.getCantidad(),
                    item.getNombre(),
                    subtotal));
        }

        sb.append("----------------------------------------\n");
        sb.append(String.format("TOTAL BRUTO: %20d\n", totalOriginal));
        if (descuento > 0) {
            sb.append(String.format("DESCUENTO (10%%): %15d-\n", descuento));
        }
        sb.append(String.format("TOTAL A PAGAR: %16d\n", totalFinal));
        sb.append("\nGracias por su compra.\n");
        sb.append("      JOHEX.inc\n");
        return sb.toString();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void ExitToMenu() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("MenuIniciadasesionListoV2.fxml"));
            Stage stage = (Stage) lblMontoTotal.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo volver al menú.");
        }
    }
}
