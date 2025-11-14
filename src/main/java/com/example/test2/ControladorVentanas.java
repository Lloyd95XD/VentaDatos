    package com.example.test2;

    import javafx.event.ActionEvent;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Node;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.scene.control.PasswordField;
    import javafx.scene.control.TextField;
    import javafx.scene.paint.Color;
    import javafx.scene.text.Text;
    import javafx.stage.Stage;

    import java.sql.Connection;
    import java.sql.PreparedStatement;

    public class ControladorVentanas {

        private final String sql = "INSERT INTO Usuario " +
                "(Nombre, Apellido, Email, Telefono, Password, Fecha_creacion_de_cuenta, Admin) " +
                "VALUES (?, ?, ?, ?, ?, CURDATE(), ?)";

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
        // -------------------------------


        // =============================
        // 🔸 MÉTODO DEL BOTÓN REGISTRAR
        // =============================
        @FXML

            private void RegistroTablaUsuario() {

                String nombre = txtNombre.getText().trim();
                String apellido = txtApellido.getText().trim();
                String correo = txtCorreo.getText().trim();
                String telefono = txtTelefono.getText().trim();
                String pass1 = txtPassword.getText();
                String pass2 = txtRepetirPassword.getText();

                // Validaciones...
                if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty()
                        || telefono.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {

                    textoerror.setText("❌ Faltan datos por completar");
                    return;
                }
                if (!pass1.equals(pass2)) {

                    textoerror.setText("❌ Las contraseñas no coinciden");
                    return;
                }

                try (Connection conn = ConexionBD.conectar();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    // OJO: ahora empiezas desde Nombre, ya no envías Id_Usuario
                    stmt.setString(1, nombre);
                    stmt.setString(2, apellido);
                    stmt.setString(3, correo);
                    stmt.setString(4, telefono);
                    stmt.setString(5, pass1);
                    stmt.setInt(6, 0);   // Admin = 0

                    stmt.executeUpdate();


                    textoerror.setText("✔ Usuario registrado correctamente");
                    textoerror.setFill(Color.web("#22bc43"));

                    limpiarCampos();

                } catch (Exception e) {
                    System.out.println("❌ Error al registrar: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        // =============================
        // 🔹 LIMPIAR CAMPOS
        // =============================
        private void limpiarCampos() {
            txtNombre.clear();
            txtApellido.clear();
            txtRut.clear();
            txtCorreo.clear();
            txtTelefono.clear();
            txtPassword.clear();
            txtRepetirPassword.clear();
        }

        // =================================================
        // Sistema de ventanas
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















        // Este SÍ va conectado al botón con onAction
        @FXML
        private void PantallaIniciarsesion(ActionEvent event) {
            System.out.println("exito");
    cambiarAVentana("iniciarSesion",event);
        }
        @FXML
        private void regresarmenuprincipal(ActionEvent event) {
            cambiarAVentana("pantalla_iniciarsesion",event);
        }
        @FXML
        private  void Registrarboton(ActionEvent event) {
            cambiarAVentana("registrarte",event);
        }
    }