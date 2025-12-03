package com.example.test2;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.UnaryOperator;

public class ControladorVentanas {

    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtRut;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;

    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtRepetirPassword;

    @FXML private Text textoerror;
    @FXML private Text textoerrorLogin;

    @FXML private TextField txtnombrecuenta;
    @FXML private TextField txtpassword2;

    @FXML private Button registrousuariooo;

    @FXML private Hyperlink linkRegistroMuebles;
    @FXML private Hyperlink linkRegistroUsuarios;
    @FXML private Hyperlink linkVerTablasMuebles;

    @FXML
    private void initialize() {

        boolean esAdmin = UsuarioSesion.isAdmin();

        if (registrousuariooo != null) {
            registrousuariooo.setDisable(!esAdmin);
            registrousuariooo.setVisible(esAdmin);
            registrousuariooo.setManaged(esAdmin);
        }

        if (!esAdmin) {
            if (linkRegistroMuebles != null) {
                linkRegistroMuebles.setVisible(false);
                linkRegistroMuebles.setManaged(false);
            }
            if (linkRegistroUsuarios != null) {
                linkRegistroUsuarios.setVisible(false);
                linkRegistroUsuarios.setManaged(false);
            }
            if (linkVerTablasMuebles != null) {
                linkVerTablasMuebles.setVisible(false);
                linkVerTablasMuebles.setManaged(false);
            }
        }

        // Formato RUT con TextFormatter
        if (txtRut != null) {
            UnaryOperator<TextFormatter.Change> rutFilter = change -> {
                if (!change.isContentChange()) {
                    return change;
                }

                String newText = change.getControlNewText().toUpperCase();

                String limpio = newText
                        .replace(".", "")
                        .replace("-", "")
                        .replaceAll("[^0-9K]", "");

                if (limpio.length() > 9) {
                    limpio = limpio.substring(0, 9);
                }

                if (limpio.isEmpty()) {
                    change.setText("");
                    change.setRange(0, change.getControlText().length());
                    change.setCaretPosition(0);
                    change.setAnchor(0);
                    return change;
                }

                String formateado = formatearRut(limpio);

                int oldLength = change.getControlText().length();

                change.setRange(0, oldLength);
                change.setText(formateado);

                change.setCaretPosition(formateado.length());
                change.setAnchor(formateado.length());

                return change;
            };

            txtRut.setTextFormatter(new TextFormatter<>(rutFilter));
        }

        // Teléfono +569 obligatorio
        if (txtTelefono != null) {

            if (txtTelefono.getText() == null || txtTelefono.getText().isEmpty()) {
                txtTelefono.setText("+569");
            }

            UnaryOperator<TextFormatter.Change> telFilter = change -> {
                String newText = change.getControlNewText();
                if (!newText.startsWith("+569")) return null;
                if (!newText.matches("\\+569\\d{0,8}")) return null;
                return change;
            };

            txtTelefono.setTextFormatter(new TextFormatter<>(telFilter));
            txtTelefono.positionCaret(txtTelefono.getText().length());
        }
    }

    private String formatearRut(String rutLimpio) {
        if (rutLimpio == null || rutLimpio.isEmpty()) return "";
        if (rutLimpio.length() == 1) return rutLimpio;

        String cuerpo = rutLimpio.substring(0, rutLimpio.length() - 1);
        String dv = rutLimpio.substring(rutLimpio.length() - 1);

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

    @FXML
    private void RegistroTablaUsuario() {

        textoerror.setFill(Color.web("#ff4444"));

        String rutFormateado = txtRut.getText().trim().toUpperCase();
        String nombre   = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String correo   = txtCorreo.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String pass1    = txtPassword.getText();
        String pass2    = txtRepetirPassword.getText();

        if (rutFormateado.isEmpty() || nombre.isEmpty() || apellido.isEmpty() ||
                correo.isEmpty() || telefono.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {

            textoerror.setText("Faltan datos por completar");
            return;
        }

        String rutLimpio = rutFormateado.replace(".", "").replace("-", "");
        if (rutLimpio.length() < 2) {
            textoerror.setText("RUT inválido");
            return;
        }

        String cuerpo = rutLimpio.substring(0, rutLimpio.length() - 1);
        String dv = rutLimpio.substring(rutLimpio.length() - 1);

        if (!cuerpo.matches("\\d{7,8}")) {
            textoerror.setText("El RUT debe tener entre 7 y 8 dígitos en el cuerpo");
            return;
        }

        if (!validarRutChileno(cuerpo, dv)) {
            textoerror.setText("RUT inválido");
            return;
        }

        String idUsuario = cuerpo;

        if (!telefono.matches("\\+569\\d{8}")) {
            textoerror.setText("Teléfono inválido. Formato: +569XXXXXXXX");
            return;
        }

        if (!pass1.equals(pass2)) {
            textoerror.setText("Las contraseñas no coinciden");
            return;
        }

        String hashedPassword = BCrypt.hashpw(pass1, BCrypt.gensalt());

        String sqlRegistrar = "{ CALL sp_registrar_usuario(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlRegistrar)) {

            stmt.setString(1, idUsuario);
            stmt.setString(2, nombre);
            stmt.setString(3, apellido);
            stmt.setString(4, correo);
            stmt.setString(5, telefono);
            stmt.setString(6, hashedPassword);
            stmt.setInt(7, 0); // Admin = 0 por defecto

            stmt.executeUpdate();

            textoerror.setText("Usuario registrado correctamente");
            textoerror.setFill(Color.web("#22bc43"));

            limpiarCampos();

        } catch (Exception e) {
            textoerror.setText("Error al registrar usuario");
            textoerror.setFill(Color.web("#ff4444"));
            e.printStackTrace();
        }
    }

    @FXML
    private void iniciarSesion(ActionEvent event) {

        String identificador = txtnombrecuenta.getText().trim();
        String identificadorSinFormato = identificador.replaceAll("\\D", "");
        String passwordIngresada = txtpassword2.getText();

        if (identificador.isEmpty() || passwordIngresada.isEmpty()) {
            textoerrorLogin.setText("Debe ingresar usuario y contraseña");
            textoerrorLogin.setFill(Color.web("#ff4444"));
            return;
        }

        String sqlLogin = "{ CALL sp_login_usuario(?, ?) }";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlLogin)) {

            stmt.setString(1, identificador);
            stmt.setString(2, identificadorSinFormato);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                textoerrorLogin.setText("Usuario no encontrado");
                textoerrorLogin.setFill(Color.web("#ff4444"));
                return;
            }

            String passHash = rs.getString("Password");
            if (!BCrypt.checkpw(passwordIngresada, passHash)) {
                textoerrorLogin.setText("Contraseña incorrecta");
                textoerrorLogin.setFill(Color.web("#ff4444"));
                return;
            }

            if (rs.getInt("Suspendido") == 1) {
                textoerrorLogin.setText("Cuenta suspendida.");
                textoerrorLogin.setFill(Color.web("#ff4444"));
                return;
            }

            String idUsuario = rs.getString("Id_Usuario");
            String nombre = rs.getString("Nombre");
            boolean esAdmin = rs.getInt("Admin") == 1;

            UsuarioSesion.setSesion(idUsuario, nombre, esAdmin);

            textoerrorLogin.setText("Bienvenido " + nombre);
            textoerrorLogin.setFill(Color.web("#22bc43"));

            menuventa(event);

        } catch (Exception e) {
            textoerrorLogin.setText("Error al iniciar sesión");
            textoerrorLogin.setFill(Color.web("#ff4444"));
            e.printStackTrace();
        }
    }

    private void limpiarCampos() {
        if (txtNombre != null) txtNombre.clear();
        if (txtApellido != null) txtApellido.clear();
        if (txtRut != null) txtRut.clear();
        if (txtCorreo != null) txtCorreo.clear();
        if (txtTelefono != null) txtTelefono.setText("+569");
        if (txtPassword != null) txtPassword.clear();
        if (txtRepetirPassword != null) txtRepetirPassword.clear();
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

    public void cambiarAVentana(String ventanaFxml, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(ventanaFxml + ".fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            System.out.println("Error cargando " + ventanaFxml + ".fxml");
            e.printStackTrace();
        }
    }

    @FXML private void PantallaIniciarsesion(ActionEvent event){ cambiarAVentana("iniciarSesion",event); }
    @FXML private void regresarmenuprincipal(ActionEvent event){ cambiarAVentana("VentanaLoginV2",event); }
    @FXML private void Registrarboton(ActionEvent event){ cambiarAVentana("registrarte",event); }
    @FXML public  void menuventa(ActionEvent event){ cambiarAVentana("MenuiniciadasesionListoV2",event); }
    @FXML private void compraaaboton(ActionEvent event){ cambiarAVentana("MenuMuebles",event); }
    @FXML private void VerBoletas(ActionEvent event){ cambiarAVentana("BoletaTablaV2",event); }

    @FXML
    private void VerRegistrosUsuarios(ActionEvent event){
        if (!UsuarioSesion.isAdmin()) {
            mostrarAlertaNoAdmin();
            return;
        }
        cambiarAVentana("TablaUsuarios", event);
    }

    @FXML
    private void VentanaRegistroDeMuebles(ActionEvent event){
        if (!UsuarioSesion.isAdmin()) {
            mostrarAlertaNoAdmin();
            return;
        }
        cambiarAVentana("VentanaGrafico",event);
    }

    @FXML
    private void VerTablasMuebles(ActionEvent event){
        if (!UsuarioSesion.isAdmin()) {
            mostrarAlertaNoAdmin();
            return;
        }
        cambiarAVentana("TablaMueblesVentana", event);
    }

    private void mostrarAlertaNoAdmin() {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Acceso restringido");
        alerta.setHeaderText("Solo administradores");
        alerta.setContentText("No tienes permisos para acceder a esta sección.");
        alerta.showAndWait();
    }
}

