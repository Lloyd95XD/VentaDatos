package com.example.test2;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminUsuariosController implements Initializable {

    // ==========================
    //  TABLA USUARIOS
    // ==========================
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

    private final ObservableList<DatosControlador> listaUsuarios =
            FXCollections.observableArrayList();

    // ==========================
    //  CONTROLES PARA EDITAR ROL/SUCURSAL
    // ==========================
    @FXML private ComboBox<String> cbRol;
    @FXML private ComboBox<String> cbSucursal;
    @FXML private Label lblUsuarioSeleccionado;
    @FXML private Label lblMensaje;

    // nombreRol → Id_Rol
    private final Map<String, Integer> mapaRoles = new HashMap<>();
    // nombreSucursal → Id_Sucursales
    private final Map<String, Integer> mapaSucursales = new HashMap<>();

    // ==========================
    //  INIT
    // ==========================
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarColumnas();
        configurarEdicionColumnas();   // 👈 para editar rut, nombre, etc.
        cargarRoles();
        cargarSucursales();
        cargarUsuarios();

        // Cuando seleccionas un usuario en la tabla
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
    }

    // ==========================
    //  EDICIÓN DIRECTA EN LA TABLA
    // ==========================
    private void configurarEdicionColumnas() {
        tablaUsuarios.setEditable(true);

        // RUT (Id_Usuario)
        colRut.setCellFactory(TextFieldTableCell.forTableColumn());
        colRut.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            String antiguoRut = u.getIdUsuario();
            String nuevoRut = event.getNewValue();

            if (nuevoRut == null || nuevoRut.isBlank()) {
                tablaUsuarios.refresh();
                return;
            }

            boolean ok = actualizarCampoUsuario("Id_Usuario", nuevoRut, antiguoRut);
            if (ok) {
                u.setIdUsuario(nuevoRut);
                if (lblMensaje != null) lblMensaje.setText("Rut actualizado");
            } else {
                tablaUsuarios.refresh();
            }
        });

        // Nombre
        colNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colNombre.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            String nuevo = event.getNewValue();
            if (nuevo == null) nuevo = "";
            boolean ok = actualizarCampoUsuario("Nombre", nuevo, u.getIdUsuario());
            if (ok) u.setNombre(nuevo);
        });

        // Apellido
        colApellido.setCellFactory(TextFieldTableCell.forTableColumn());
        colApellido.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            String nuevo = event.getNewValue();
            if (nuevo == null) nuevo = "";
            boolean ok = actualizarCampoUsuario("Apellido", nuevo, u.getIdUsuario());
            if (ok) u.setApellido(nuevo);
        });

        // Email
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            String nuevo = event.getNewValue();
            if (nuevo == null) nuevo = "";
            boolean ok = actualizarCampoUsuario("Email", nuevo, u.getIdUsuario());
            if (ok) u.setEmail(nuevo);
        });

        // Teléfono
        colTelefono.setCellFactory(TextFieldTableCell.forTableColumn());
        colTelefono.setOnEditCommit(event -> {
            DatosControlador u = event.getRowValue();
            String nuevo = event.getNewValue();
            if (nuevo == null) nuevo = "";
            boolean ok = actualizarCampoUsuario("Telefono", nuevo, u.getIdUsuario());
            if (ok) u.setTelefono(nuevo);
        });
    }

    /**
     * Actualiza un solo campo de la tabla Usuario en la BD.
     * @param columna nombre de la columna (Id_Usuario, Nombre, Email, ...)
     * @param nuevoValor valor que quieres guardar
     * @param idUsuarioActual ID actual (para el WHERE)
     * @return true si el UPDATE fue bien, false si hubo error
     */
    private boolean actualizarCampoUsuario(String columna, String nuevoValor, String idUsuarioActual) {
        String sql = "UPDATE Usuario SET " + columna + " = ? WHERE Id_Usuario = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Si estamos cambiando el RUT (PK entero)
            if ("Id_Usuario".equalsIgnoreCase(columna)) {
                int nuevoId = Integer.parseInt(nuevoValor);
                int viejoId = Integer.parseInt(idUsuarioActual);
                stmt.setInt(1, nuevoId);
                stmt.setInt(2, viejoId);
            } else {
                // Campos tipo VARCHAR
                stmt.setString(1, nuevoValor);
                stmt.setInt(2, Integer.parseInt(idUsuarioActual));
            }

            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (lblMensaje != null)
                lblMensaje.setText("Error actualizando " + columna + ": " + e.getMessage());
            return false;
        }
    }

    // ==========================
    //  CARGA DE DATOS DESDE BD
    // ==========================

    private void cargarRoles() {
        cbRol.getItems().clear();
        mapaRoles.clear();

        String sql = "SELECT Id_Rol, Nombre_Rol FROM rol";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("Id_Rol");
                String nombre = rs.getString("Nombre_Rol");

                cbRol.getItems().add(nombre);
                mapaRoles.put(nombre, id);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (lblMensaje != null) lblMensaje.setText("Error cargando roles");
        }
    }

    private void cargarSucursales() {
        cbSucursal.getItems().clear();
        mapaSucursales.clear();

        String sql = "SELECT Id_Sucursales, localidad FROM sucursales";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("Id_Sucursales");
                String nombre = rs.getString("localidad");

                cbSucursal.getItems().add(nombre);
                mapaSucursales.put(nombre, id);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (lblMensaje != null) lblMensaje.setText("Error cargando sucursales");
        }
    }

    private void cargarUsuarios() {
        listaUsuarios.clear();

        String sql = """
            SELECT u.Id_Usuario,
                   u.Nombre,
                   u.Apellido,
                   u.Email,
                   u.Telefono,
                   u.Fecha_creacion_de_cuenta,
                   u.Admin,
                   u.Id_Rol,
                   r.Nombre_Rol,
                   u.Id_Sucursales,
                   s.localidad
            FROM Usuario u
            LEFT JOIN rol r ON u.Id_Rol = r.Id_Rol
            LEFT JOIN sucursales s ON u.Id_Sucursales = s.Id_Sucursales
            """;

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                String idUsuario   = rs.getString("Id_Usuario");
                String nombre      = rs.getString("Nombre");
                String apellido    = rs.getString("Apellido");
                String email       = rs.getString("Email");
                String telefono    = rs.getString("Telefono");
                String fecha       = rs.getString("Fecha_creacion_de_cuenta");
                int admin          = rs.getInt("Admin");
                int idRol          = rs.getInt("Id_Rol");
                String nombreRol   = rs.getString("Nombre_Rol");
                int idSucursal     = rs.getInt("Id_Sucursales");
                String sucursal    = rs.getString("localidad");

                // Password lo dejamos vacío (no se muestra)
                DatosControlador user = new DatosControlador(
                        idUsuario, nombre, apellido,
                        email, telefono, "",
                        fecha, admin, idRol, idSucursal,
                        nombreRol, sucursal
                );

                listaUsuarios.add(user);
            }

            tablaUsuarios.setItems(listaUsuarios);

        } catch (Exception e) {
            e.printStackTrace();
            if (lblMensaje != null) lblMensaje.setText("Error cargando usuarios");
        }
    }

    // ==========================
    //  SELECCIÓN DE USUARIO
    // ==========================
    private void mostrarUsuarioSeleccionado(DatosControlador u) {
        if (lblUsuarioSeleccionado == null) return;

        if (u == null) {
            lblUsuarioSeleccionado.setText("usuario seleccionado");
            if (cbRol != null) cbRol.getSelectionModel().clearSelection();
            if (cbSucursal != null) cbSucursal.getSelectionModel().clearSelection();
            return;
        }

        lblUsuarioSeleccionado.setText("usuario seleccionado: " + u.getIdUsuario());

        if (cbRol != null) cbRol.setValue(u.getNombreRol());
        if (cbSucursal != null) cbSucursal.setValue(u.getNombreSucursal());
    }

    // ==========================
    //  GUARDAR CAMBIOS ROL/SUCURSAL (BOTÓN APLICAR CAMBIOS)
    // ==========================
    @FXML
    private void guardarCambiosRolSucursal() {
        if (lblMensaje != null) lblMensaje.setText("");

        DatosControlador u = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (u == null) {
            if (lblMensaje != null) lblMensaje.setText("Selecciona un usuario primero");
            return;
        }

        if (cbRol == null || cbSucursal == null) return;

        String rolNombre = cbRol.getValue();
        String sucNombre = cbSucursal.getValue();

        if (rolNombre == null || sucNombre == null) {
            if (lblMensaje != null) lblMensaje.setText("Elige Rol y Sucursal");
            return;
        }

        Integer nuevoIdRol = mapaRoles.get(rolNombre);
        Integer nuevoIdSuc = mapaSucursales.get(sucNombre);

        if (nuevoIdRol == null || nuevoIdSuc == null) {
            if (lblMensaje != null) lblMensaje.setText("Error con IDs de rol/sucursal");
            return;
        }

        String sql = "UPDATE Usuario SET Id_Rol = ?, Id_Sucursales = ? WHERE Id_Usuario = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nuevoIdRol);
            stmt.setInt(2, nuevoIdSuc);
            stmt.setInt(3, Integer.parseInt(u.getIdUsuario()));

            stmt.executeUpdate();

            u.setIdRol(nuevoIdRol);
            u.setNombreRol(rolNombre);
            u.setIdSucursal(nuevoIdSuc);
            u.setNombreSucursal(sucNombre);
            tablaUsuarios.refresh();

            if (lblMensaje != null) lblMensaje.setText("✅ Cambios guardados");

        } catch (Exception e) {
            e.printStackTrace();
            if (lblMensaje != null) lblMensaje.setText("Error al guardar cambios");
        }
    }

    // ==========================
    //  ELIMINAR CUENTA (BOTÓN)
    // ==========================
    @FXML
    private void EliminarCuenta() {
        if (lblMensaje != null) lblMensaje.setText("");

        DatosControlador u = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (u == null) {
            if (lblMensaje != null) lblMensaje.setText("Selecciona un usuario para eliminar");
            return;
        }

        String sql = "DELETE FROM Usuario WHERE Id_Usuario = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(u.getIdUsuario()));
            stmt.executeUpdate();

            listaUsuarios.remove(u);
            if (lblMensaje != null) lblMensaje.setText("✅ Usuario eliminado");

        } catch (Exception e) {
            e.printStackTrace();
            if (lblMensaje != null)
                lblMensaje.setText("Error al eliminar (puede tener ventas asociadas)");
        }
    }
}
