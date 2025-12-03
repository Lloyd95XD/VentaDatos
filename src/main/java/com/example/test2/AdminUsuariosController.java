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

    /// Inicializa el controlador y carga los datos necesarios
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

    /// Configura las columnas de la tabla de usuarios
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
        colSuspendido.setCellValueFactory(new PropertyValueFactory<>("suspendido"));

        colAdmin.setCellFactory(col -> new TableCell<DatosControlador, Integer>() {
            @Override
            protected void updateItem(Integer valor, boolean empty) {
                super.updateItem(valor, empty);

                if (empty || valor == null) {
                    setText(null);
                    return;
                }
                setText(valor == 1 ? "Sí" : "No");
                setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            }
        });
        colAdmin.setEditable(false);

        colSuspendido.setCellFactory(col -> new TableCell<DatosControlador, Integer>() {
            @Override
            protected void updateItem(Integer valor, boolean empty) {
                super.updateItem(valor, empty);

                if (empty || valor == null) {
                    setText(null);
                    return;
                }
                if (valor == 1) {
                    setText("Suspendido");
                    setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                } else {
                    setText("Activo");
                    setStyle("-fx-text-fill: #66ff99; -fx-font-weight: bold;");
                }
            }
        });
        colSuspendido.setEditable(false);
    }

    /// Configura la capacidad de edicion de las columnas
    private void configurarEdicionColumnas() {

        tablaUsuarios.setEditable(true);

        /// ADVERTENCIA: Cambiar Id_Usuario puede romper las FK
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

    /// Actualiza un campo especifico de un usuario en la base de datos
    private boolean actualizarCampoUsuario(String columna, String nuevo, String idActual) {
        String sql = "{ CALL sp_actualizar_campo_usuario_perfil(?, ?, ?) }";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, columna);
            stmt.setString(2, nuevo);
            stmt.setString(3, idActual);

            stmt.execute();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error actualizando " + columna);
            return false;
        }
    }


    /// Carga los roles disponibles desde la base de datos
    private void cargarRoles() {
        cbRol.getItems().clear();
        mapaRoles.clear();

        String sql = "{ CALL sp_listar_roles() }";

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


    /// Carga las sucursales disponibles desde la base de datos
    private void cargarSucursales() {
        cbSucursal.getItems().clear();
        mapaSucursales.clear();

        String sql = "{ CALL sp_listar_sucursales() }";

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


    /// Carga la lista completa de usuarios desde la base de datos
    private void cargarUsuarios() {

        listaUsuarios.clear();

        String sql = "{ CALL sp_listar_usuarios_completos() }";

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


    /// Muestra los detalles del usuario seleccionado en los campos correspondientes
    private void mostrarUsuarioSeleccionado(DatosControlador u) {
        if (u == null) {
            lblUsuarioSeleccionado.setText("(ninguno)");
            return;
        }

        lblUsuarioSeleccionado.setText("Usuario seleccionado: " + u.getIdUsuario());

        cbRol.setValue(u.getNombreRol());
        cbSucursal.setValue(u.getNombreSucursal());
    }

    /// Guarda los cambios de rol y sucursal del usuario seleccionado
    @FXML
    private void guardarCambiosRolSucursal() {

        DatosControlador u = tablaUsuarios.getSelectionModel().getSelectedItem();

        if (u == null) {
            lblMensaje.setText("Selecciona un usuario primero");
            return;
        }

        Integer idRol = mapaRoles.get(cbRol.getValue());
        Integer idSuc = mapaSucursales.get(cbSucursal.getValue());

        String sql = "{ CALL sp_actualizar_rol_sucursal_usuario(?, ?, ?) }";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // ORDEN CORRECTO
            stmt.setString(1, u.getIdUsuario());
            stmt.setInt(2, idRol);
            stmt.setInt(3, idSuc);

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


    /// Suspende o reactiva la cuenta del usuario seleccionado
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
                        ? "Reactivar al usuario " + u.getIdUsuario()
                        : "Suspender al usuario " + u.getIdUsuario()
        );
        alerta.setContentText(
                suspendido
                        ? "El usuario podrá iniciar sesión nuevamente."
                        : "El usuario no podrá iniciar sesión."
        );

        if (alerta.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        int nuevoEstado = suspendido ? 0 : 1;

        String sql = "{ CALL sp_actualizar_suspension_usuario(?, ?) }";


        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // ORDEN CORRECTO (ARREGLADO)
            stmt.setString(1, u.getIdUsuario()); // p_id_usuario
            stmt.setInt(2, nuevoEstado);         // p_suspendido

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

    /// Regresa a la ventana del menu principal
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