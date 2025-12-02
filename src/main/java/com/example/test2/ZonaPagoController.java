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
import java.text.NumberFormat;
import java.util.Locale;
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

    // Usuario
    private String idUsuario = "";
    private int idSucursalUsuario = -1;
    private String nombreSucursalUsuario = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarTablaCarrito();
        configurarDescripcion();
        cargarMetodosPago();
        cargarDatosUsuarioYSucursal();
        configurarFormatoRutCliente();
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

    // ==========================
    //  CONFIG TABLA CARRITO + CLP
    // ==========================
    private void configurarTablaCarrito() {
        colNombreCarrito.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecioCarrito.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colCantidadCarrito.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        // Formatear precios CLP
        colPrecioCarrito.setCellFactory(col -> new TableCell<ItemCarrito, Integer>() {
            @Override
            protected void updateItem(Integer p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    setText("$ " + formatearCLP(p));
                }
            }
        });

        tablaCarrito.setItems(carrito);
    }

    // ==========================
    //  FORMATO CLP
    // ==========================
    private String formatearCLP(int valor) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CL"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(valor);
    }

    // ==========================
    // DESCRIPCIÓN (con CLP)
    // ==========================
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
                                    "\nPrecio unidad: $ " + formatearCLP(nuevo.getPrecio())
                    );
                });
    }

    private void cargarMetodosPago() {
        comboMetodoPago.setItems(FXCollections.observableArrayList(
                "Débito", "Crédito", "Efectivo"
        ));
    }

    // ==========================
    // FORMATO AUTOMÁTICO RUT CLIENTE
    // ==========================
    private void configurarFormatoRutCliente() {
        if (txtRutCliente == null) return;

        final boolean[] actualizando = { false };

        txtRutCliente.textProperty().addListener((obs, oldValue, newValue) -> {
            if (actualizando[0]) return;
            actualizando[0] = true;

            String soloDigitos = newValue.replaceAll("\\D", "");
            if (soloDigitos.length() > 9)
                soloDigitos = soloDigitos.substring(0, 9);

            String formateado = formatearRut(soloDigitos);
            txtRutCliente.setText(formateado);
            txtRutCliente.positionCaret(formateado.length());

            actualizando[0] = false;
        });
    }

    private String formatearRut(String digitos) {
        if (digitos.isEmpty()) return "";
        if (digitos.length() == 1) return digitos;

        String cuerpo = digitos.substring(0, digitos.length() - 1);
        String dv = digitos.substring(digitos.length() - 1);

        StringBuilder sb = new StringBuilder();
        int c = 0;

        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            sb.insert(0, cuerpo.charAt(i));
            c++;
            if (c == 3 && i != 0) {
                sb.insert(0, ".");
                c = 0;
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

        if (idUsuarioSesion == null || idUsuarioSesion.isEmpty()) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Sesión", "Usuario no encontrado.");
            return;
        }

        idUsuario = idUsuarioSesion;

        String sql = """
                SELECT u.Id_Sucursales, s.localidad
                FROM usuario u
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
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error", "No se pudo cargar la sucursal.");
        }
    }

    // ==========================
    // TOTAL CLP
    // ==========================
    private void actualizarTextoTotal() {
        if (lblMontoTotal != null) {
            lblMontoTotal.setText("Monto Total: $ " + formatearCLP(totalAPagar));
        }
    }

    // ==========================
    // REALIZAR PAGO
    // ==========================
    @FXML
    private void realizarPago() {

        if (carrito.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Carrito vacío", "No hay productos.");
            return;
        }

        String metodo = comboMetodoPago.getValue();
        String direccion = txtDireccion.getText().trim();
        String rutF = txtRutCliente.getText().trim();
        String rutNum = rutF.replaceAll("\\D", "");

        if (metodo == null) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Método de pago", "Selecciona un método.");
            return;
        }

        Integer rutCliente = null;
        if (!rutNum.isEmpty()) {
            try {
                rutCliente = Integer.parseInt(rutNum);
            } catch (NumberFormatException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "RUT inválido",
                        "Debe ser solo números.");
                return;
            }
        }

        // Descuento por segunda compra
        int descuento = 0;
        int totalFinal = totalAPagar;

        if (rutCliente != null && clienteYaComproAntes(rutCliente)) {
            descuento = (int) (totalAPagar * 0.10);
            totalFinal = totalAPagar - descuento;
        }

        // Insertar venta
        int idBoleta = insertarVenta(totalFinal, metodo, direccion, rutCliente, descuento);

        if (idBoleta == -1) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error", "No se pudo registrar la venta.");
            return;
        }

        insertarDetalles(idBoleta);
        actualizarStock();

        abrirVentanaBoleta(idBoleta, nombreSucursalUsuario, metodo,
                rutF, direccion, totalAPagar, descuento, totalFinal, carrito);
    }

    private int insertarVenta(int totalFinal, String metodo, String direccion,
                              Integer rutCliente, int descuento) {

        String sql = """
            INSERT INTO venta
            (Id_Usuario, Id_Sucursales, Precio_Total, Metodo_de_pago, Direccion, Rut_Cliente, Descuento)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, idUsuario);
            stmt.setInt(2, idSucursalUsuario);
            stmt.setInt(3, totalFinal);
            stmt.setString(4, metodo);
            stmt.setString(5, direccion.isEmpty() ? null : direccion);

            if (rutCliente == null)
                stmt.setNull(6, Types.INTEGER);
            else
                stmt.setInt(6, rutCliente);

            stmt.setInt(7, descuento);

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void insertarDetalles(int idBoleta) {

        String sql = """
            INSERT INTO detalles_ventas (Id_Boleta, Id_Producto, Cantidad_de_compras)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (ItemCarrito item : carrito) {
                stmt.setInt(1, idBoleta);
                stmt.setInt(2, item.getIdProducto());
                stmt.setInt(3, item.getCantidad());
                stmt.addBatch();
            }
            stmt.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actualizarStock() {

        String sql = """
            UPDATE producto SET Stock = Stock - ? WHERE Id_Producto = ?
        """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (ItemCarrito item : carrito) {
                stmt.setInt(1, item.getCantidad());
                stmt.setInt(2, item.getIdProducto());
                stmt.addBatch();
            }
            stmt.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean clienteYaComproAntes(int rutCliente) {
        String sql = "SELECT COUNT(*) AS total FROM venta WHERE Rut_Cliente = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rutCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total") >= 1;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==========================
    // BOLETA (CON CLP)
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

            controller.setTextoBoleta(
                    construirTextoBoleta(idBoleta, nombreSucursal, metodoPago,
                            rut, direccion, totalOriginal, descuento, totalFinal,
                            carrito)
            );

            Stage stage = (Stage) lblMontoTotal.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir la boleta.");
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

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        StringBuilder sb = new StringBuilder();

        sb.append("      Boleta N° ").append(idBoleta).append("\n\n");
        sb.append("Fecha: ").append(fecha).append("\n");
        sb.append("Sucursal: ").append(nombreSucursal).append("\n");
        sb.append("Método de pago: ").append(metodoPago).append("\n");

        if (rut != null && !rut.isEmpty()) sb.append("Rut cliente: ").append(rut).append("\n");
        if (direccion != null && !direccion.isEmpty()) sb.append("Dirección: ").append(direccion).append("\n");

        sb.append("----------------------------------------\n");
        sb.append(String.format("%-4s %-20s %12s\n", "Cant", "Producto", "Subtotal"));
        sb.append("----------------------------------------\n");

        for (ItemCarrito item : carrito) {
            int subtotal = item.getCantidad() * item.getPrecio();
            sb.append(String.format("%-4d %-20s %12s\n",
                    item.getCantidad(),
                    item.getNombre(),
                    "$ " + formatearCLP(subtotal)));
        }

        sb.append("----------------------------------------\n");
        sb.append(String.format("TOTAL BRUTO: %18s\n", "$ " + formatearCLP(totalOriginal)));

        if (descuento > 0)
            sb.append(String.format("DESCUENTO (10%%): %13s-\n", "$ " + formatearCLP(descuento)));

        sb.append(String.format("TOTAL A PAGAR: %15s\n", "$ " + formatearCLP(totalFinal)));
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
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error", "No se pudo volver al menú.");
        }
    }
}
