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
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

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
    @FXML private Text lblDescuentoInfo;
    @FXML private Text textoerror;

    // Tarjeta
    @FXML private VBox paneTarjeta;
    @FXML private TextField txtNumeroTarjeta;
    @FXML private TextField txtNombreTarjeta;
    @FXML private TextField txtFechaVenc;
    @FXML private TextField txtCVV;

    // Efectivo
    @FXML private VBox paneEfectivo;
    @FXML private TextField txtMontoEfectivo;
    @FXML private Text lblVuelto;

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
        configurarFormatoTarjeta();
        configurarFormatoFechaVenc();
        configurarFormatoCVV();
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
                "Visa", "Mastercard", "Efectivo"
        ));

        comboMetodoPago.valueProperty().addListener((obs, oldVal, newVal) -> {
            actualizarSeccionesPago(newVal);
            limpiarErroresCampos();
        });

        actualizarSeccionesPago(null);
    }

    private void actualizarSeccionesPago(String metodo) {
        if (paneTarjeta != null && paneEfectivo != null) {
            boolean esTarjeta = "Visa".equals(metodo) || "Mastercard".equals(metodo);
            boolean esEfectivo = "Efectivo".equals(metodo);

            paneTarjeta.setVisible(esTarjeta);
            paneTarjeta.setManaged(esTarjeta);

            paneEfectivo.setVisible(esEfectivo);
            paneEfectivo.setManaged(esEfectivo);

            if (!esEfectivo && lblVuelto != null) {
                lblVuelto.setText("");
            }
        }
    }

    // ==========================
    // FORMATO AUTOMÁTICO RUT CLIENTE + MÓDULO 11
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

            // Cada vez que cambia el RUT, se resetea el descuento mostrado
            if (lblDescuentoInfo != null) {
                lblDescuentoInfo.setText("");
            }
            actualizarTextoTotal();
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

    private boolean validarRutChileno(String cuerpo, String dv) {
        try {
            int suma = 0;
            int factor = 2;

            for (int i = cuerpo.length() - 1; i >= 0; i--) {
                int num = Character.getNumericValue(cuerpo.charAt(i));
                suma += num * factor;
                factor++;
                if (factor > 7) {
                    factor = 2;
                }
            }

            int resto = suma % 11;
            int dvCalculado = 11 - resto;

            String dvFinal;
            if (dvCalculado == 11) {
                dvFinal = "0";
            } else if (dvCalculado == 10) {
                dvFinal = "K";
            } else {
                dvFinal = String.valueOf(dvCalculado);
            }

            return dvFinal.equalsIgnoreCase(dv);
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================
    // FORMATO TARJETA / FECHA / CVV
    // ==========================
    private void configurarFormatoTarjeta() {
        if (txtNumeroTarjeta == null) return;

        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (!change.isContentChange()) return change;

            String oldText = change.getControlText();
            String newText = change.getControlNewText();

            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > 16) {
                digits = digits.substring(0, 16);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) sb.append(" ");
                sb.append(digits.charAt(i));
            }

            String formatted = sb.toString();

            change.setRange(0, oldText.length());
            change.setText(formatted);
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());

            return change;
        };

        txtNumeroTarjeta.setTextFormatter(new TextFormatter<>(filter));
    }

    private void configurarFormatoFechaVenc() {
        if (txtFechaVenc == null) return;

        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (!change.isContentChange()) return change;

            String oldText = change.getControlText();
            String newText = change.getControlNewText();

            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > 4) {
                digits = digits.substring(0, 4);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i == 2) sb.append("/");
                sb.append(digits.charAt(i));
            }

            String formatted = sb.toString();

            change.setRange(0, oldText.length());
            change.setText(formatted);
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());

            return change;
        };

        txtFechaVenc.setTextFormatter(new TextFormatter<>(filter));
    }

    private void configurarFormatoCVV() {
        if (txtCVV == null) return;

        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (!change.isContentChange()) return change;

            String oldText = change.getControlText();
            String newText = change.getControlNewText();

            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > 3) {
                digits = digits.substring(0, 3);
            }

            change.setRange(0, oldText.length());
            change.setText(digits);
            change.setCaretPosition(digits.length());
            change.setAnchor(digits.length());

            return change;
        };

        txtCVV.setTextFormatter(new TextFormatter<>(filter));
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
    // HELPERS VISUALES (BORDES Y TOOLTIP)
    // ==========================
    private void setMensajeError(String msg) {
        if (textoerror != null) {
            textoerror.setText(msg == null ? "" : msg);
        }
    }

    private void marcarErrorCampo(TextField campo, String mensaje) {
        if (campo == null) return;

        if (!campo.getProperties().containsKey("baseStyle")) {
            campo.getProperties().put("baseStyle", campo.getStyle());
        }
        String base = (String) campo.getProperties().get("baseStyle");
        campo.setStyle(base + "; -fx-border-color: #ff5555; -fx-border-width: 1.5; -fx-border-radius: 6;");

        Tooltip tooltip = new Tooltip(mensaje);
        campo.setTooltip(tooltip);
    }

    private void limpiarEstiloCampo(TextField campo) {
        if (campo == null) return;
        Object baseObj = campo.getProperties().get("baseStyle");
        if (baseObj != null) {
            campo.setStyle((String) baseObj);
        }
        campo.setTooltip(null);
    }

    private void limpiarErroresCampos() {
        setMensajeError("");
        if (lblVuelto != null) lblVuelto.setText("");

        limpiarEstiloCampo(txtNumeroTarjeta);
        limpiarEstiloCampo(txtNombreTarjeta);
        limpiarEstiloCampo(txtFechaVenc);
        limpiarEstiloCampo(txtCVV);
        limpiarEstiloCampo(txtRutCliente);
        limpiarEstiloCampo(txtMontoEfectivo);
        // NO limpio lblDescuentoInfo aquí para no borrar el preview solo por tocar otra cosa
    }

    // ==========================
    // VALIDACIÓN TARJETA (Luhn + marca + formato)
    // ==========================
    private boolean validarDatosTarjeta(String metodo) {

        String numero = txtNumeroTarjeta != null
                ? txtNumeroTarjeta.getText().replaceAll("\\s", "")
                : "";
        String nombre = txtNombreTarjeta != null
                ? txtNombreTarjeta.getText().trim()
                : "";
        String fecha = txtFechaVenc != null
                ? txtFechaVenc.getText().trim()
                : "";
        String cvv   = txtCVV != null
                ? txtCVV.getText().trim()
                : "";

        if (numero.isEmpty() || nombre.isEmpty() || fecha.isEmpty() || cvv.isEmpty()) {
            setMensajeError("Complete todos los datos de la tarjeta.");
            if (numero.isEmpty()) marcarErrorCampo(txtNumeroTarjeta, "Número requerido");
            if (nombre.isEmpty()) marcarErrorCampo(txtNombreTarjeta, "Nombre requerido");
            if (fecha.isEmpty())  marcarErrorCampo(txtFechaVenc, "Fecha requerida");
            if (cvv.isEmpty())    marcarErrorCampo(txtCVV, "CVV requerido");
            return false;
        }

        if (!numero.matches("\\d{13,16}")) {
            setMensajeError("El número de tarjeta debe tener entre 13 y 16 dígitos.");
            marcarErrorCampo(txtNumeroTarjeta, "Solo dígitos, 13 a 16 números");
            return false;
        }

        if (!validarLuhn(numero)) {
            setMensajeError("El número de tarjeta no es válido (falló el algoritmo).");
            marcarErrorCampo(txtNumeroTarjeta, "Número inválido");
            return false;
        }

        String marcaDetectada = detectarMarcaTarjeta(numero);
        if ("Visa".equals(metodo) && !"Visa".equals(marcaDetectada)) {
            setMensajeError("El número no corresponde a una tarjeta Visa.");
            marcarErrorCampo(txtNumeroTarjeta, "No es Visa");
            return false;
        }

        if ("Mastercard".equals(metodo) && !"Mastercard".equals(marcaDetectada)) {
            setMensajeError("El número no corresponde a una tarjeta Mastercard.");
            marcarErrorCampo(txtNumeroTarjeta, "No es Mastercard");
            return false;
        }

        if (!fecha.matches("\\d{2}/\\d{2}")) {
            setMensajeError("La fecha debe tener formato MM/AA.");
            marcarErrorCampo(txtFechaVenc, "Formato MM/AA");
            return false;
        }

        try {
            int mes = Integer.parseInt(fecha.substring(0, 2));
            if (mes < 1 || mes > 12) {
                setMensajeError("El mes debe estar entre 01 y 12.");
                marcarErrorCampo(txtFechaVenc, "Mes inválido");
                return false;
            }
        } catch (NumberFormatException e) {
            setMensajeError("Fecha de vencimiento inválida.");
            marcarErrorCampo(txtFechaVenc, "Fecha inválida");
            return false;
        }

        if (!cvv.matches("\\d{3}")) {
            setMensajeError("El CVV debe tener exactamente 3 dígitos.");
            marcarErrorCampo(txtCVV, "3 dígitos numéricos");
            return false;
        }

        return true;
    }

    private boolean validarLuhn(String numero) {
        int suma = 0;
        boolean alternar = false;

        for (int i = numero.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(numero.charAt(i));
            if (alternar) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            suma += n;
            alternar = !alternar;
        }
        return suma % 10 == 0;
    }

    private String detectarMarcaTarjeta(String numero) {
        if (numero == null || numero.isEmpty()) return "Desconocida";

        if (numero.startsWith("4")) {
            return "Visa";
        }

        try {
            if (numero.length() >= 2) {
                int pref2 = Integer.parseInt(numero.substring(0, 2));
                if (pref2 >= 51 && pref2 <= 55) {
                    return "Mastercard";
                }
            }
            if (numero.length() >= 4) {
                int pref4 = Integer.parseInt(numero.substring(0, 4));
                if (pref4 >= 2221 && pref4 <= 2720) {
                    return "Mastercard";
                }
            }
        } catch (NumberFormatException ignored) {}

        return "Desconocida";
    }

    // ==========================
    // BOTÓN VERIFICAR DESCUENTO (PREVIEW EN VENTANA)
    // ==========================
    @FXML
    private void verificarDescuento() {
        if (lblDescuentoInfo != null) {
            lblDescuentoInfo.setText("");
        }
        setMensajeError("");
        limpiarEstiloCampo(txtRutCliente);

        String rutF = txtRutCliente != null ? txtRutCliente.getText().trim() : "";

        if (rutF.isEmpty()) {
            if (lblDescuentoInfo != null) {
                lblDescuentoInfo.setText("Ingrese RUT para verificar descuento.");
            }
            actualizarTextoTotal();
            return;
        }

        String rutLimpio = rutF.replace(".", "").replace("-", "").toUpperCase();

        if (rutLimpio.length() < 2) {
            setMensajeError("El RUT del cliente es inválido.");
            marcarErrorCampo(txtRutCliente, "RUT inválido");
            actualizarTextoTotal();
            return;
        }

        String cuerpo = rutLimpio.substring(0, rutLimpio.length() - 1);
        String dv = rutLimpio.substring(rutLimpio.length() - 1);

        if (!cuerpo.matches("\\d{7,8}")) {
            setMensajeError("El RUT debe tener 7 u 8 dígitos en el cuerpo.");
            marcarErrorCampo(txtRutCliente, "Largo inválido");
            actualizarTextoTotal();
            return;
        }

        if (!validarRutChileno(cuerpo, dv)) {
            setMensajeError("El RUT del cliente no es válido.");
            marcarErrorCampo(txtRutCliente, "RUT inválido");
            actualizarTextoTotal();
            return;
        }

        int rutCliente = Integer.parseInt(cuerpo);

        if (clienteYaComproAntes(rutCliente)) {
            int descuento = (int) (totalAPagar * 0.10);
            int totalConDesc = totalAPagar - descuento;

            if (lblDescuentoInfo != null) {
                lblDescuentoInfo.setText(
                        "Descuento 10% aplicado: -$ " + formatearCLP(descuento) +
                                " | Total con descuento: $ " + formatearCLP(totalConDesc)
                );
            }
            if (lblMontoTotal != null) {
                lblMontoTotal.setText("Monto Total: $ " + formatearCLP(totalConDesc));
            }
        } else {
            if (lblDescuentoInfo != null) {
                lblDescuentoInfo.setText("No aplica descuento para este RUT.");
            }
            actualizarTextoTotal();
        }
    }

    // ==========================
    // REALIZAR PAGO
    // ==========================
    @FXML
    private void realizarPago() {

        limpiarErroresCampos();

        if (carrito.isEmpty()) {
            setMensajeError("No hay productos en el carrito.");
            return;
        }

        String metodo = comboMetodoPago.getValue();
        String direccion = txtDireccion.getText().trim();
        String rutF = txtRutCliente.getText().trim();

        if (metodo == null) {
            setMensajeError("Seleccione un método de pago.");
            return;
        }

        // Variables para guardar en BD y mostrar en boleta
        Integer montoEfectivoDB = null;
        Integer vueltoDB = null;
        String marcaTarjetaDB = null;
        String ultimos4DB = null;

        // Si es tarjeta, validar y preparar datos
        if ("Visa".equals(metodo) || "Mastercard".equals(metodo)) {
            if (!validarDatosTarjeta(metodo)) {
                return;
            }

            String numero = txtNumeroTarjeta.getText().replaceAll("\\s", "");
            marcaTarjetaDB = detectarMarcaTarjeta(numero);

            if (numero.length() >= 4) {
                ultimos4DB = numero.substring(numero.length() - 4);
            }
        }

        // Validar RUT cliente opcional con fórmula 11
        Integer rutCliente = null;
        if (!rutF.isEmpty()) {
            String rutLimpio = rutF.replace(".", "").replace("-", "").toUpperCase();

            if (rutLimpio.length() < 2) {
                setMensajeError("El RUT del cliente es inválido.");
                marcarErrorCampo(txtRutCliente, "RUT inválido");
                return;
            }

            String cuerpo = rutLimpio.substring(0, rutLimpio.length() - 1);
            String dv = rutLimpio.substring(rutLimpio.length() - 1);

            if (!cuerpo.matches("\\d{7,8}")) {
                setMensajeError("El RUT debe tener 7 u 8 dígitos en el cuerpo.");
                marcarErrorCampo(txtRutCliente, "Largo inválido");
                return;
            }

            if (!validarRutChileno(cuerpo, dv)) {
                setMensajeError("El RUT del cliente no es válido.");
                marcarErrorCampo(txtRutCliente, "RUT inválido");
                return;
            }

            rutCliente = Integer.parseInt(cuerpo);
        }

        // Descuento por segunda compra
        int descuento = 0;
        int totalFinal = totalAPagar;

        if (rutCliente != null && clienteYaComproAntes(rutCliente)) {
            descuento = (int) (totalAPagar * 0.10);
            totalFinal = totalAPagar - descuento;
        }

        // Validar efectivo contra totalFinal
        if ("Efectivo".equals(metodo)) {
            String montoStr = txtMontoEfectivo != null ? txtMontoEfectivo.getText().trim() : "";
            if (montoStr.isEmpty()) {
                setMensajeError("Ingrese con cuánto paga el cliente.");
                marcarErrorCampo(txtMontoEfectivo, "Monto requerido");
                return;
            }
            int monto;
            try {
                monto = Integer.parseInt(montoStr);
            } catch (NumberFormatException e) {
                setMensajeError("El monto en efectivo debe ser numérico.");
                marcarErrorCampo(txtMontoEfectivo, "Solo números");
                return;
            }

            if (monto < totalFinal) {
                setMensajeError("El monto en efectivo no alcanza para pagar el total.");
                marcarErrorCampo(txtMontoEfectivo, "Monto insuficiente");
                return;
            }

            int vuelto = monto - totalFinal;
            montoEfectivoDB = monto;
            vueltoDB = vuelto;

            if (lblVuelto != null) {
                lblVuelto.setText("Vuelto: $ " + formatearCLP(vuelto));
            }
        }

        // Insertar venta
        int idBoleta = insertarVenta(
                totalFinal,
                metodo,
                direccion,
                rutCliente,
                descuento,
                montoEfectivoDB,
                vueltoDB,
                marcaTarjetaDB,
                ultimos4DB
        );

        if (idBoleta == -1) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Error", "No se pudo registrar la venta.");
            return;
        }

        insertarDetalles(idBoleta);
        actualizarStock();

        abrirVentanaBoleta(
                idBoleta,
                nombreSucursalUsuario,
                metodo,
                rutF,
                direccion,
                totalAPagar,
                descuento,
                totalFinal,
                carrito,
                montoEfectivoDB,
                vueltoDB,
                marcaTarjetaDB,
                ultimos4DB
        );
    }

    @FXML
    private void calcularVueltoEfectivo() {
        limpiarErroresCampos();

        if (!"Efectivo".equals(comboMetodoPago.getValue())) {
            setMensajeError("Seleccione 'Efectivo' para calcular el vuelto.");
            return;
        }

        String montoStr = txtMontoEfectivo != null ? txtMontoEfectivo.getText().trim() : "";
        if (montoStr.isEmpty()) {
            setMensajeError("Ingrese con cuánto paga el cliente.");
            marcarErrorCampo(txtMontoEfectivo, "Monto requerido");
            return;
        }

        int monto;
        try {
            monto = Integer.parseInt(montoStr);
        } catch (NumberFormatException e) {
            setMensajeError("El monto en efectivo debe ser numérico.");
            marcarErrorCampo(txtMontoEfectivo, "Solo números");
            return;
        }

        int totalFinal = totalAPagar; // Aquí podrías replicar lógica de descuento si quieres que considere el RUT también

        if (monto < totalFinal) {
            setMensajeError("El monto en efectivo no alcanza para pagar el total.");
            marcarErrorCampo(txtMontoEfectivo, "Monto insuficiente");
            return;
        }

        int vuelto = monto - totalFinal;
        if (lblVuelto != null) {
            lblVuelto.setText("Vuelto: $ " + formatearCLP(vuelto));
        }
    }

    private int insertarVenta(int totalFinal,
                              String metodo,
                              String direccion,
                              Integer rutCliente,
                              int descuento,
                              Integer montoEfectivo,
                              Integer vuelto,
                              String marcaTarjeta,
                              String ultimos4Tarjeta) {

        String sql = """
            INSERT INTO venta
            (Id_Usuario, Id_Sucursales, Precio_Total, Metodo_de_pago, Direccion,
             Rut_Cliente, Descuento, Monto_Efectivo, Vuelto, Marca_Tarjeta, Ultimos_4_Tarjeta)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

            if (montoEfectivo == null)
                stmt.setNull(8, Types.INTEGER);
            else
                stmt.setInt(8, montoEfectivo);

            if (vuelto == null)
                stmt.setNull(9, Types.INTEGER);
            else
                stmt.setInt(9, vuelto);

            if (marcaTarjeta == null || marcaTarjeta.isEmpty())
                stmt.setNull(10, Types.VARCHAR);
            else
                stmt.setString(10, marcaTarjeta);

            if (ultimos4Tarjeta == null || ultimos4Tarjeta.isEmpty())
                stmt.setNull(11, Types.VARCHAR);
            else
                stmt.setString(11, ultimos4Tarjeta);

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
    // BOLETA (CON CLP + DETALLES DE PAGO)
    // ==========================
    private void abrirVentanaBoleta(int idBoleta,
                                    String nombreSucursal,
                                    String metodoPago,
                                    String rut,
                                    String direccion,
                                    int totalOriginal,
                                    int descuento,
                                    int totalFinal,
                                    ObservableList<ItemCarrito> carrito,
                                    Integer montoEfectivo,
                                    Integer vuelto,
                                    String marcaTarjeta,
                                    String ultimos4Tarjeta) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ZonadePagoRealizado.fxml"));
            Parent root = loader.load();

            BoletaController controller = loader.getController();

            controller.setTextoBoleta(
                    construirTextoBoleta(
                            idBoleta,
                            nombreSucursal,
                            metodoPago,
                            rut,
                            direccion,
                            totalOriginal,
                            descuento,
                            totalFinal,
                            carrito,
                            montoEfectivo,
                            vuelto,
                            marcaTarjeta,
                            ultimos4Tarjeta
                    )
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
                                        ObservableList<ItemCarrito> carrito,
                                        Integer montoEfectivo,
                                        Integer vuelto,
                                        String marcaTarjeta,
                                        String ultimos4Tarjeta) {

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        StringBuilder sb = new StringBuilder();

        sb.append("      Boleta N° ").append(idBoleta).append("\n\n");
        sb.append("Fecha: ").append(fecha).append("\n");
        sb.append("Sucursal: ").append(nombreSucursal != null ? nombreSucursal : "-").append("\n");
        sb.append("Método de pago: ").append(metodoPago != null ? metodoPago : "-").append("\n");

        if (rut != null && !rut.isEmpty()) sb.append("Rut cliente: ").append(rut).append("\n");
        if (direccion != null && !direccion.isEmpty()) sb.append("Dirección: ").append(direccion).append("\n");

        // Datos extra según método
        if (metodoPago != null) {
            String lower = metodoPago.toLowerCase();

            if (lower.contains("efectivo")) {
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

            if (lower.contains("visa") || lower.contains("master")) {
                if ((marcaTarjeta != null && !marcaTarjeta.isEmpty())
                        || (ultimos4Tarjeta != null && !ultimos4Tarjeta.isEmpty())) {
                    sb.append("Tarjeta: ");
                    if (marcaTarjeta != null && !marcaTarjeta.isEmpty()) {
                        sb.append(marcaTarjeta).append(" ");
                    }
                    if (ultimos4Tarjeta != null && !ultimos4Tarjeta.isEmpty()) {
                        sb.append("terminada en ").append(ultimos4Tarjeta);
                    }
                    sb.append("\n");
                }
            }
        }

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
