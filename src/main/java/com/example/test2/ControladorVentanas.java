package com.example.test2;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
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
    // 🔹 Campos del formulario (FXML)
    // -------------------------------
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

    // 🔹 BOTONES QUE DEPENDEN DE ADMIN
    @FXML
    private Button registrousuariooo;

    // ===================================================
    // 🔥 initialize: admin + filtros RUT/Teléfono
    // ===================================================
    @FXML
    private void initialize() {

        // 1) Mostrar / ocultar botón de admin
        if (registrousuariooo != null) {
            boolean esAdmin = UsuarioSesion.isAdmin();
            registrousuariooo.setDisable(!esAdmin);
            registrousuariooo.setVisible(esAdmin);
            registrousuariooo.setManaged(esAdmin);
        }

        // 2) Filtro para RUT: solo dígitos, máximo 9
        if (txtRut != null) {
            UnaryOperator<TextFormatter.Change> rutFilter = change -> {
                String newText = change.getControlNewText();

                // Permitir vacío mientras se escribe
                if (newText.isEmpty()) {
                    return change;
                }

                // Solo permitir dígitos
                if (!newText.matches("\\d*")) {
                    return null;
                }

                // Máximo 9 dígitos (ej: 210113592)
                if (newText.length() > 9) {
                    return null;
                }

                return change;
            };

            txtRut.setTextFormatter(new TextFormatter<>(rutFilter));
        }

        // 3) Filtro para Teléfono: siempre "+569" + hasta 8 dígitos
        if (txtTelefono != null) {

            // Si está vacío al cargar, prellenar con +569
            if (txtTelefono.getText() == null || txtTelefono.getText().isEmpty()) {
                txtTelefono.setText("+569");
            }

            UnaryOperator<TextFormatter.Change> telFilter = change -> {
                String newText = change.getControlNewText();

                // Siempre debe empezar con +569
                if (!newText.startsWith("+569")) {
                    return null;
                }

                // Solo permitir +569 seguido de dígitos
                if (!newText.matches("\\+569\\d*")) {
                    return null;
                }

                // Máximo: +569 + 8 dígitos = 12 caracteres
                if (newText.length() > 12) {
                    return null;
                }

                return change;
            };

            txtTelefono.setTextFormatter(new TextFormatter<>(telFilter));

            // Que el cursor quede al final al abrir
            txtTelefono.positionCaret(txtTelefono.getText().length());
        }
    }


    // ===================================================
    // 🔥 MÉTODO REGISTRAR
    // ===================================================
    @FXML
    private void RegistroTablaUsuario() {
        // Registrara los datos de los usuarios a la tabla
        textoerror.setFill(Color.web("#ff4444"));

        String rutStr   = txtRut.getText().trim();
        String nombre   = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String correo   = txtCorreo.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String pass1    = txtPassword.getText();
        String pass2    = txtRepetirPassword.getText();

        // 1) Validar campos vacíos
        if (rutStr.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty()
                || telefono.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {

            textoerror.setText("❌ Faltan datos por completar");
            return;
        }

        // Validar longitud del RUT (sin guion): entre 7 y 9 dígitos
        if (!rutStr.matches("\\d{7,9}")) {
            textoerror.setText("❌ RUT inválido. Debe tener entre 7 y 9 dígitos (solo números, sin guion)");
            return;
        }

        // Parsear RUT a int (Id_Usuario)
        int idUsuario;
        try {
            idUsuario = Integer.parseInt(rutStr);
        } catch (NumberFormatException e) {
            textoerror.setText("❌ El RUT debe contener solo números");
            return;
        }

        // Validar teléfono: +569 + 8 dígitos
        if (!telefono.matches("\\+569\\d{8}")) {
            textoerror.setText("❌ Teléfono inválido. Use formato +569XXXXXXXX");
            return;
        }

        // 5) Validar que las contraseñas coinciden
        if (!pass1.equals(pass2)) {
            textoerror.setText("❌ Las contraseñas no coinciden");
            return;
        }

        // Hasheamos la contraseña
        String hashedPassword = BCrypt.hashpw(pass1, BCrypt.gensalt());

        // Guardar en BD
        try (Connection conn = ConexionBD.conectar()) {

            if (conn == null) {
                textoerror.setText("❌ Error de conexión a la base de datos");
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, idUsuario);       // Id_Usuario = RUT numérico sin guion
                stmt.setString(2, nombre);
                stmt.setString(3, apellido);
                stmt.setString(4, correo);
                stmt.setString(5, telefono);
                stmt.setString(6, hashedPassword);
                stmt.setInt(7, 0);              // Admin por defecto es 0

                stmt.executeUpdate();

                textoerror.setText("✔ Usuario registrado correctamente");
                textoerror.setFill(Color.web("#22bc43"));

                limpiarCampos();

            }

        } catch (Exception e) {
            textoerror.setText("❌ Error al registrar usuario");
            textoerror.setFill(Color.web("#ff4444"));
            System.out.println("❌ Error al registrar: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // ===================================================
    // 🔥 INICIAR SESIÓN
    // ===================================================
    @FXML
    private void iniciarSesion(ActionEvent event) {

        String identificador = txtnombrecuenta.getText().trim();
        String passwordIngresada = txtpassword2.getText();

        if (identificador.isEmpty() || passwordIngresada.isEmpty()) {
            textoerrorLogin.setText("❌ Debe ingresar usuario y contraseña");
            textoerrorLogin.setFill(Color.web("#ff4444"));
            return;
        }

        int idUsuarioBuscado = -1;
        try { idUsuarioBuscado = Integer.parseInt(identificador); } catch (Exception ignored) {}

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
            stmt.setInt(3, idUsuarioBuscado);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                textoerrorLogin.setText("❌ Usuario no encontrado");
                textoerrorLogin.setFill(Color.web("#ff4444"));
                return;
            }

            String passHash = rs.getString("Password");
            if (!BCrypt.checkpw(passwordIngresada, passHash)) {
                textoerrorLogin.setText("❌ Contraseña incorrecta");
                textoerrorLogin.setFill(Color.web("#ff4444"));
                return;
            }

            // Guardamos sesión global
            int idUsuario = rs.getInt("Id_Usuario");
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
    //  LIMPIAR CAMPOS
    // ===================================================
    private void limpiarCampos() {
        txtNombre.clear();
        txtApellido.clear();
        txtRut.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtPassword.clear();
        txtRepetirPassword.clear();
    }


    // ===================================================
    //  SISTEMA DE VENTANAS
    // ===================================================
    public void cambiarAVentana(String ventanaFxml, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(ventanaFxml + ".fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            System.out.println("❌ Error cargando " + ventanaFxml + ".fxml");
            e.printStackTrace();
        }
    }


    // ===================================================
    //  BOTONES DE NAVEGACIÓN
    // ===================================================
    @FXML
    private void PantallaIniciarsesion(ActionEvent event) {
        cambiarAVentana("iniciarSesion", event);
    }

    @FXML
    private void regresarmenuprincipal(ActionEvent event) {
        cambiarAVentana("pantalla_iniciarsesion",event);
    }

    @FXML
    private void Registrarboton(ActionEvent event) {
        cambiarAVentana("registrarte",event);
    }

    @FXML
    public void menuventa(ActionEvent event) {
        cambiarAVentana("VentanaIniciadalista",event);
    }

    @FXML
    private void compraaa(ActionEvent event){
        cambiarAVentana("MenuMuebles",event);
    }

    @FXML
    private void VerRegistrosUsuarios(ActionEvent event){
        if (UsuarioSesion.isAdmin()) {
            cambiarAVentana("TablaUsuarios", event);
        } else {
            System.out.println("No eres admin");
        }
    }

    @FXML
    private void VerBoletas(ActionEvent event){
        cambiarAVentana("VerBoletas",event);
    }
}
