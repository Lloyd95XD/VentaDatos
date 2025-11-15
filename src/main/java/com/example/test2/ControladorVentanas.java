    package com.example.test2;


    import javafx.event.ActionEvent;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Node;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.scene.control.PasswordField;
    import javafx.scene.control.TableColumn;
    import javafx.scene.control.TableView;
    import javafx.scene.control.TextField;
    import javafx.scene.paint.Color;
    import javafx.scene.text.Text;
    import javafx.stage.Stage;
    import org.mindrot.jbcrypt.BCrypt;



    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;

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

        // -------------------------------
// tabla
//

        //

        // =============================
        // 🔸 MÉTODO DEL BOTÓN REGISTRAR
        // =============================
        @FXML

            private void RegistroTablaUsuario() {
                String Id_Usuario = txtRut.getText().trim();
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
            String hashedPassword = BCrypt.hashpw(pass1, BCrypt.gensalt());

                try (Connection conn = ConexionBD.conectar();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    // OJO: ahora empiezas desde Nombre, ya no envías Id_Usuario
                    stmt.setInt(1, Integer.parseInt(Id_Usuario));  // Id_Usuario (RUT)
                    stmt.setString(2, nombre);
                    stmt.setString(3, apellido);
                    stmt.setString(4, correo);
                    stmt.setString(5, telefono);
                    stmt.setString(6, hashedPassword);
                    stmt.setInt(7, 0); // Admin = 0
                    stmt.executeUpdate();


                    textoerror.setText("✔ Usuario registrado correctamente");
                    textoerror.setFill(Color.web("#22bc43"));

                    limpiarCampos();

                } catch (Exception e) {
                    System.out.println("❌ Error al registrar: " + e.getMessage());
                    e.printStackTrace();
                }
            }
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
        SELECT Id_Usuario, Nombre, Password
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

                textoerrorLogin.setText("✔ Sesión iniciada. Bienvenido!");
                textoerrorLogin.setFill(Color.web("#22bc43"));

                // ⭐ Cambiar a la ventana del menú
                menuventa(event);

            } catch (Exception e) {
                textoerrorLogin.setText("❌ Error al iniciar sesión");
                textoerrorLogin.setFill(Color.web("#ff4444"));
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
        private void menuventa(ActionEvent event) {
            cambiarAVentana("MenuMuebles",event);
        }
    }