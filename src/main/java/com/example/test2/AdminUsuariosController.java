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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminUsuariosController implements Initializable {

    @FXML private TableView<DatosControlador> tablaUsuarios;

    @FXML private TableColumn<DatosControlador, String> colRut;
    @FXML private TableColumn<DatosControlador, String> colNombre;
    @FXML private TableColumn<DatosControlador, String> colApellido;
    @FXML private TableColumn<DatosControlador, String> colEmail;
    @FXML private TableColumn<DatosControlador, String> colTelefono;
    @FXML private TableColumn<DatosControlador, String> colFechaCreada;
    @FXML private TableColumn<DatosControlador, Integer> colAdmin;
    @FXML private TableColumn<DatosControlador, String> colRol;
    @FXML private TableColumn<DatosControlador, String> colSucursal;

    // 🔹 Nueva columna
    @FXML private TableColumn<DatosControlador, Integer> colSuspendido;

    @FXML private ComboBox<String> cbRol;
    @FXML private ComboBox<String> cbSucursal;

    @FXML private Label lblUsuarioSeleccionado;
    @FXML private Label lblMensaje;

    @FXML private Button salirboton1;

    private final ObservableList<DatosControlador> listaUsuarios =
            FXCollections.observableArrayList();

    private final Map<String, Integer> mapaRoles = new HashMap<>();
    private final Map<String, Integer> mapaSucursales = new HashMap<>();


    // ==============================================
    // INIT
    // ==============================================
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarColumnas();
        configurarEdicionColumnas();
        cargarRoles();
        cargarSucursales();
        cargarUsuarios();

        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener(
                (obs, viejo, nuevo) -> mostrarUsuarioSeleccionado(nuevo)
        );
    }


    private void configurarColumnas() {
        colRut.setCellValueFactory(new PropertyValueFactory<>("idUsuario"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellido.setCellValueFactory(new PropertyValueFactory<>("apellido"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colFechaCreada.setCellValueFactory(new PropertyValueFactory<>("fechaCreacion"));
        colAdmin.setCellValueFactory(new PropertyValueFactory<>("admin"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("nombreRol"));
        colSucursal.setCellValueFactory(new PropertyValueFactory<>("nombreSucursal"));

        // 🔹 NUEVO
        colSuspendido.setCellValueFactory(new PropertyValueFactory<>("suspendido"));
    }


    // ==============================================
    // EDICIÓN
    // ==============================================
    private void configurarEdicionColumnas() {

        tablaUsuarios.setEditable(true);

        colRut.setCellFactory(TextFieldTableCell.forTableColumn());
        colRut.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            actualizarCampoUsuario("Id_Usuario", event.getNewValue(), u.getIdUsuario());
            u.setIdUsuario(event.getNewValue());
        });

        colNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colNombre.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            actualizarCampoUsuario("Nombre", event.getNewValue(), u.getIdUsuario());
            u.setNombre(event.getNewValue());
        });

        colApellido.setCellFactory(TextFieldTableCell.forTableColumn());
        colApellido.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            actualizarCampoUsuario("Apellido", event.getNewValue(), u.getIdUsuario());
            u.setApellido(event.getNewValue());
        });

        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            actualizarCampoUsuario("Email", event.getNewValue(), u.getIdUsuario());
            u.setEmail(event.getNewValue());
        });

        colTelefono.setCellFactory(TextFieldTableCell.forTableColumn());
        colTelefono.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            actualizarCampoUsuario("Telefono", event.getNewValue(), u.getIdUsuario());
            u.setTelefono(event.getNewValue());
        });
    }


    private boolean actualizarCampoUsuario(String columna, String nuevo, String idActual) {
        String sql = "UPDATE Usuario SET " + columna + " = ? WHERE Id_Usuario = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevo);
            stmt.setString(2, idActual);
            stmt.executeUpdate();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error actualizando " + columna);
            return false;
        }
    }


    // ==============================================
    // CARGAR ROLES
    // ==============================================
    private void cargarRoles() {
        cbRol.getItems().clear();
        mapaRoles.clear();

        String sql = "SELECT Id_Rol, Nombre_Rol FROM rol";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cbRol.getItems().add(rs.getString("Nombre_Rol"));
                mapaRoles.put(rs.getString("Nombre_Rol"), rs.getInt("Id_Rol"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ==============================================
    // CARGAR SUCURSALES
    // ==============================================
    private void cargarSucursales() {
        cbSucursal.getItems().clear();
        mapaSucursales.clear();

        String sql = "SELECT Id_Sucursales, localidad FROM sucursales";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cbSucursal.getItems().add(rs.getString("localidad"));
                mapaSucursales.put(rs.getString("localidad"), rs.getInt("Id_Sucursales"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ==============================================
    // CARGAR USUARIOS
    // ==============================================
    private void cargarUsuarios() {

        listaUsuarios.clear();

        String sql = """
            SELECT u.Id_Usuario, u.Nombre, u.Apellido, u.Email, u.Telefono,
                   u.Fecha_creacion_de_cuenta, u.Admin, u.Id_Rol, r.Nombre_Rol,
                   u.Id_Sucursales, s.localidad, u.Suspendido
            FROM Usuario u
            LEFT JOIN rol r ON u.Id_Rol = r.Id_Rol
            LEFT JOIN sucursales s ON u.Id_Sucursales = s.Id_Sucursales
        """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                DatosControlador u = new DatosControlador(
                        rs.getString("Id_Usuario"),
                        rs.getString("Nombre"),
                        rs.getString("Apellido"),
                        rs.getString("Email"),
                        rs.getString("Telefono"),
                        "",
                        rs.getString("Fecha_creacion_de_cuenta"),
                        rs.getInt("Admin"),
                        rs.getInt("Id_Rol"),
                        rs.getInt("Id_Sucursales"),
                        rs.getString("Nombre_Rol"),
                        rs.getString("localidad"),
                        rs.getInt("Suspendido")
                );

                listaUsuarios.add(u);
            }

            tablaUsuarios.setItems(listaUsuarios);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ==============================================
    // SELECCIONAR USUARIO
    // ==============================================
    private void mostrarUsuarioSeleccionado(DatosControlador u) {
        if (u == null) {
            lblUsuarioSeleccionado.setText("(ninguno)");
            return;
        }

        lblUsuarioSeleccionado.setText("Usuario seleccionado: " + u.getIdUsuario());

        cbRol.setValue(u.getNombreRol());
        cbSucursal.setValue(u.getNombreSucursal());
    }


    // ==============================================
    // GUARDAR CAMBIOS
    // ==============================================
    @FXML
    private void guardarCambiosRolSucursal() {

        DatosControlador u = tablaUsuarios.getSelectionModel().getSelectedItem();

        if (u == null) {
            lblMensaje.setText("Selecciona un usuario primero");
            return;
        }

        Integer idRol = mapaRoles.get(cbRol.getValue());
        Integer idSuc = mapaSucursales.get(cbSucursal.getValue());

        String sql = "UPDATE Usuario SET Id_Rol = ?, Id_Sucursales = ? WHERE Id_Usuario = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idRol);
            stmt.setInt(2, idSuc);
            stmt.setString(3, u.getIdUsuario());

            stmt.executeUpdate();

            u.setIdRol(idRol);
            u.setNombreRol(cbRol.getValue());
            u.setIdSucursal(idSuc);
            u.setNombreSucursal(cbSucursal.getValue());

            tablaUsuarios.refresh();
            lblMensaje.setText("Cambios guardados");

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error guardando cambios");
        }
    }


    // ==============================================
    // ELIMINAR CUENTA ✔
    // ==============================================
    @FXML
    private void EliminarCuenta() {

        DatosControlador u = tablaUsuarios.getSelectionModel().getSelectedItem();

        if (u == null) {
            lblMensaje.setText("Selecciona un usuario primero");
            return;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Eliminar cuenta");
        alerta.setHeaderText("¿Eliminar al usuario " + u.getIdUsuario() + "?");
        alerta.setContentText("Esta acción es irreversible.");

        if (alerta.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Usuario WHERE Id_Usuario = ?")) {

            stmt.setString(1, u.getIdUsuario());
            stmt.executeUpdate();

            listaUsuarios.remove(u);
            lblMensaje.setText("Usuario eliminado");

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error al eliminar usuario");
        }
    }


    // ==============================================
    // SUSPENDER / REACTIVAR CUENTA ✔
    // ==============================================
    @FXML
    private void SuspenderCuenta() {

        DatosControlador u = tablaUsuarios.getSelectionModel().getSelectedItem();

        if (u == null) {
            lblMensaje.setText("Selecciona un usuario primero");
            return;
        }

        boolean suspendido = u.getSuspendido() == 1;

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle(suspendido ? "Reactivar cuenta" : "Suspender cuenta");
        alerta.setHeaderText(
                suspendido
                        ? "¿Deseas reactivar la cuenta del usuario " + u.getIdUsuario() + "?"
                        : "¿Deseas suspender al usuario " + u.getIdUsuario() + "?"
        );
        alerta.setContentText(
                suspendido
                        ? "El usuario podrá volver a iniciar sesión."
                        : "El usuario NO podrá iniciar sesión hasta ser reactivado."
        );

        if (alerta.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        int nuevoEstado = suspendido ? 0 : 1;

        String sql = "UPDATE Usuario SET Suspendido = ? WHERE Id_Usuario = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nuevoEstado);
            stmt.setString(2, u.getIdUsuario());
            stmt.executeUpdate();

            u.setSuspendido(nuevoEstado);
            tablaUsuarios.refresh();

            lblMensaje.setText(
                    nuevoEstado == 1 ? "Usuario suspendido" : "Usuario reactivado"
            );

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error actualizando estado");
        }
    }


    // ==============================================
    // VOLVER AL MENÚ
    // ==============================================
    @FXML
    private void VolverMenu2() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("MenuiniciadasesionListoV2.fxml"));
            Stage stage = (Stage) salirboton1.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
