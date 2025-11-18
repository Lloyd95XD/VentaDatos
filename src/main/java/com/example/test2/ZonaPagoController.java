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

    // Carrito que viene desde la ventana anterior
    private final ObservableList<ItemCarrito> carrito =
            FXCollections.observableArrayList();

    // ======== ZONA DERECHA ========
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private TextField txtRutCliente;
    @FXML private TextField txtDireccion;
    @FXML private Text lblMontoTotal;

    private int totalAPagar = 0;

    // Usuario logueado y sucursal asociada a su cuenta
    private int idUsuario = 0;
    private int idSucursalUsuario = -1;
    private String nombreSucursalUsuario = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarTablaCarrito();
        configurarDescripcion();
        cargarMetodosPago();
        cargarDatosUsuarioYSucursal();   // <<< NUEVO: carga sucursal desde la cuenta
        actualizarTextoTotal();          // por si llega 0 al inicio
    }

    // Este método lo llamarás desde la ventana anterior
    public void setCarritoYTotal(ObservableList<ItemCarrito> carritoOrigen, int total) {
        carrito.clear();
        carrito.addAll(carritoOrigen);
        tablaCarrito.setItems(carrito);
        totalAPagar = total;
        actualizarTextoTotal();
    }

    // Si quieres, puedes seguir usando esto, pero ahora también
    // cargamos el usuario desde UsuarioSesion en initialize()
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
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

    /**
     * Carga el Id_Usuario desde UsuarioSesion y obtiene
     * la sucursal asociada a su cuenta (Id_Sucursales + localidad).
     *
     * Tabla Usuario: Id_Usuario, Id_Sucursales
     * Tabla sucursales: Id_Sucursales, localidad
     */
    private void cargarDatosUsuarioYSucursal() {
        // Tomamos el usuario que inició sesión
        idUsuario = UsuarioSesion.getIdUsuario();

        if (idUsuario <= 0) {
            // Si por alguna razón no hay sesión, se puede manejar acá
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Sesión",
                    "No se encontró un usuario logueado. Inicie sesión nuevamente.");
            return;
        }

        String sql = """
                SELECT u.Id_Sucursales, s.localidad
                FROM usuario u
                LEFT JOIN sucursales s ON u.Id_Sucursales = s.Id_Sucursales
                WHERE u.Id_Usuario = ?
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    idSucursalUsuario = rs.getInt("Id_Sucursales");
                    nombreSucursalUsuario = rs.getString("localidad");

                    // Si la sucursal es NULL o 0, avisamos
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
        String rutTexto = txtRutCliente.getText().trim();

        // Verificar que tenemos sucursal del usuario
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
        if (!rutTexto.isEmpty()) {
            try {
                rutCliente = Integer.parseInt(rutTexto);
            } catch (NumberFormatException e) {
                mostrarAlerta(Alert.AlertType.WARNING,
                        "RUT inválido", "El RUT debe ser numérico (sin puntos ni guion).");
                return;
            }
        }

        // ==========================
        // 1) Insertar en VENTA
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

            stmtVenta.setInt(1, idUsuario);
            stmtVenta.setInt(2, idSucursalUsuario);           // <- sucursal del usuario
            stmtVenta.setInt(3, totalAPagar);
            stmtVenta.setString(4, metodo);
            stmtVenta.setString(5, direccion.isEmpty() ? null : direccion);
            if (rutCliente == null) {
                stmtVenta.setNull(6, Types.INTEGER);
            } else {
                stmtVenta.setInt(6, rutCliente);
            }
            stmtVenta.setInt(7, 0); // Descuento = 0 por ahora

            stmtVenta.executeUpdate();

            // Obtener Id_Boleta generado
            try (ResultSet keys = stmtVenta.getGeneratedKeys()) {
                if (keys.next()) {
                    idBoletaGenerada = keys.getInt(1);
                }
            }

            if (idBoletaGenerada == -1) {
                throw new Exception("No se pudo obtener el Id_Boleta generado.");
            }

            // ==========================
            // 2) Insertar en DETALLES_VENTAS
            // ==========================
            String sqlDetalles = """
                INSERT INTO detalles_ventas
                (Id_Boleta, Id_Producto, Cantidad_de_compras, Historial_de_movimiento)
                VALUES (?, ?, ?, ?)
                """;

            try (PreparedStatement stmtDet = conn.prepareStatement(sqlDetalles)) {

                for (ItemCarrito item : carrito) {
                    stmtDet.setInt(1, idBoletaGenerada);
                    stmtDet.setInt(2, item.getIdProducto());
                    stmtDet.setInt(3, item.getCantidad());
                    stmtDet.setString(4,
                            "Venta " + idBoletaGenerada + " - " + item.getCantidad() +
                                    " x " + item.getNombre());
                    stmtDet.addBatch();
                }

                stmtDet.executeBatch();
            }

            // (Opcional) 3) Actualizar STOCK en producto
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

            // Si todo salió bien, mostramos boleta
            abrirVentanaBoleta(
                    idBoletaGenerada,
                    nombreSucursalUsuario,   // <- nombre sucursal del usuario
                    metodo,
                    rutTexto,
                    direccion,
                    totalAPagar,
                    carrito
            );

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error al pagar", "Ocurrió un error al procesar el pago.");
        }
    }

    // ==========================
    //   ABRIR VENTANA BOLETA
    // ==========================
    private void abrirVentanaBoleta(int idBoleta,
                                    String nombreSucursal,
                                    String metodoPago,
                                    String rut,
                                    String direccion,
                                    int total,
                                    ObservableList<ItemCarrito> carrito) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ZonadePagoRealizado.fxml"));
            Parent root = loader.load();

            BoletaController controller = loader.getController();

            // Construimos el texto de la boleta
            String textoBoleta = construirTextoBoleta(
                    idBoleta,
                    nombreSucursal,
                    metodoPago,
                    rut,
                    direccion,
                    total,
                    carrito
            );

            controller.setTextoBoleta(textoBoleta);

            // ★★ REEMPLAZAR VENTANA ★★
            // Obtiene la ventana donde está el botón pagar
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
                                        int total,
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
        sb.append(String.format("TOTAL: %28d\n", total));
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

}
