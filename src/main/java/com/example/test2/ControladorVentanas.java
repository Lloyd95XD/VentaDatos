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

    private final String sql = "INSERT INTO Usuario " +
            "(Id_Usuario, Nombre, Apellido, Email, Telefono, Password, Fecha_creacion_de_cuenta, Admin) " +
            "VALUES (?, ?, ?, ?, ?, ?, CURDATE(), ?)";

    // -------------------------------
    // Campos del formulario
    // -------------------------------
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtRut;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;

    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtRepetirPassword;
    @FXML private Hyperlink RegistroMuebleslink;
    @FXML private Text textoerror;
    @FXML private Text textoerrorLogin;

    @FXML private TextField txtnombrecuenta;
    @FXML private TextField txtpassword2;

    @FXML
    private Button registrousuariooo;

    // -------------------------------
    // Inicialización
    // -------------------------------
    @FXML
    private void initialize() {

        // Mostrar botón admin según sesión
        if (registrousuariooo != null) {
            boolean esAdmin = UsuarioSesion.isAdmin();
            registrousuariooo.setDisable(!esAdmin);
            registrousuariooo.setVisible(esAdmin);
            registrousuariooo.setManaged(esAdmin);
        }

        // Filtro RUT (solo números)
        if (txtRut != null) {
            UnaryOperator<TextFormatter.Change> rutFilter = change -> {
                String newText = change.getControlNewText();
                if (newText.matches("\\d{0,15}")) return change;
                return null;
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



    // ===================================================
    //  REGISTRO DE USUARIO (Id_Usuario = VARCHAR(15))
    // ===================================================
    @FXML
    private void RegistroTablaUsuario() {

        textoerror.setFill(Color.web("#ff4444"));

        String idUsuario = txtRut.getText().trim();  // <-- ahora es STRING
        String nombre   = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String correo   = txtCorreo.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String pass1    = txtPassword.getText();
        String pass2    = txtRepetirPassword.getText();

        if (idUsuario.isEmpty() || nombre.isEmpty() || apellido.isEmpty() ||
                correo.isEmpty() || telefono.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {

            textoerror.setText("Faltan datos por completar");
            return;
        }

        if (!idUsuario.matches("\\d{7,15}")) {
            textoerror.setText("El RUT debe ser entre 7 y 15 dígitos (sin puntos ni guion)");
            return;
        }

        if (!telefono.matches("\\+569\\d{8}")) {
            textoerror.setText("Teléfono inválido. Formato: +569XXXXXXXX");
            return;
        }

        if (!pass1.equals(pass2)) {
            textoerror.setText("Las contraseñas no coinciden");
            return;
        }

        String hashedPassword = BCrypt.hashpw(pass1, BCrypt.gensalt());

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idUsuario);     // <-- STRING
            stmt.setString(2, nombre);
            stmt.setString(3, apellido);
            stmt.setString(4, correo);
            stmt.setString(5, telefono);
            stmt.setString(6, hashedPassword);
            stmt.setInt(7, 0); // Admin = false

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




    // ===================================================
    //  LOGIN (Id_Usuario ahora es STRING)
    // ===================================================
    @FXML
    private void iniciarSesion(ActionEvent event) {

        String identificador = txtnombrecuenta.getText().trim();
        String passwordIngresada = txtpassword2.getText();

        if (identificador.isEmpty() || passwordIngresada.isEmpty()) {
            textoerrorLogin.setText("Debe ingresar usuario y contraseña");
            textoerrorLogin.setFill(Color.web("#ff4444"));
            return;
        }

        String sqlLogin = """
                SELECT Id_Usuario, Nombre, Password, Admin
                FROM Usuario
                WHERE Email = ? OR Telefono = ? OR Id_Usuario = ?
                LIMIT 1
                """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlLogin)) {

            stmt.setString(1, identificador);
            stmt.setString(2, identificador);
            stmt.setString(3, identificador);  // <-- ahora STRING

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

            // Guardar sesión (ahora STRING)
            String idUsuario = rs.getString("Id_Usuario");
            String nombre = rs.getString("Nombre");
            boolean esAdmin = rs.getInt("Admin") == 1;

            UsuarioSesion.setSesion(idUsuario, nombre, esAdmin);

            textoerrorLogin.setText("✔ Bienvenido " + nombre + "!");
            textoerrorLogin.setFill(Color.web("#22bc43"));

            menuventa(event);

        } catch (Exception e) {
            textoerrorLogin.setText("Error al iniciar sesión");
            textoerrorLogin.setFill(Color.web("#ff4444"));
            e.printStackTrace();
        }
    }



    // ===================================================
    // Limpieza
    // ===================================================
    private void limpiarCampos() {
        txtNombre.clear();
        txtApellido.clear();
        txtRut.clear();
        txtCorreo.clear();
        txtTelefono.setText("+569");
        txtPassword.clear();
        txtRepetirPassword.clear();
    }


    // ===================================================
    // Cambiar Ventanas
    // ===================================================
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

    // BOTONES → mantienen igual
    @FXML private void PantallaIniciarsesion(ActionEvent event){ cambiarAVentana("iniciarSesion",event); }
    @FXML private void regresarmenuprincipal(ActionEvent event){ cambiarAVentana("VentanaLoginV2",event); }
    @FXML private void Registrarboton(ActionEvent event){ cambiarAVentana("registrarte",event); }
    @FXML public void menuventa(ActionEvent event){ cambiarAVentana("MenuiniciadasesionListoV2",event); }
    @FXML private void compraaaboton(ActionEvent event){ cambiarAVentana("MenuMuebles",event); }
    @FXML private void VerRegistrosUsuarios(ActionEvent event){
        if (UsuarioSesion.isAdmin()) cambiarAVentana("TablaUsuarios", event);
    }
    @FXML private void VentanaRegistroDeMuebles(ActionEvent event){
        if (UsuarioSesion.isAdmin()) cambiarAVentana("VentanaGrafico",event);
    }
    @FXML private void VerBoletas(ActionEvent event){ cambiarAVentana("BoletaTablaV2",event); }
}
