package com.example.test2;

import at.favre.lib.crypto.bcrypt.BCrypt;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.text.Text;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class Controlador {

    // -------- TABLA USUARIOS (+ ADMIN) --------
    @FXML private TableView<Datos> tablaUsuarios;

    @FXML private TableColumn<Datos, Integer>  colUsuId;
    @FXML private TableColumn<Datos, String>   colUsuNombre;
    @FXML private TableColumn<Datos, String>   colUsuApellido;
    @FXML private TableColumn<Datos, String>   colUsuEmail;
    @FXML private TableColumn<Datos, String>   colUsuTelefono;

    @FXML private TableColumn<Datos, String>   colUsuRol;          // admin.Rol
    @FXML private TableColumn<Datos, String>   colUsuDescripcion;  // admin.Descripcion
    @FXML private TableColumn<Datos, Boolean>  colUsuVerificador;  // admin.Verificador
    @FXML private TableColumn<Datos, String>   colUsuPassword;     // usuario.Password (hash)

    private final ObservableList<Datos> listaUsuarios = FXCollections.observableArrayList();

    // -------- TABLA INVENTARIO (PRODUCTO + INVENTARIO) --------
    @FXML private TableView<Datos.ProdInvRow> tablaInventario;

    @FXML private TableColumn<Datos.ProdInvRow, Integer> colProdId;
    @FXML private TableColumn<Datos.ProdInvRow, String>  colProdNombre;
    @FXML private TableColumn<Datos.ProdInvRow, String>  colProdDescripcion;
    @FXML private TableColumn<Datos.ProdInvRow, Integer> colProdStock;
    @FXML private TableColumn<Datos.ProdInvRow, String>  colProdHistorial;
    @FXML private TableColumn<Datos.ProdInvRow, String>  colProdSucursales;

    private final ObservableList<Datos.ProdInvRow> listaInventario = FXCollections.observableArrayList();

    // ===== LOGIN / REGISTRO =====
    @FXML private javafx.scene.control.TextField txtLoginIdentificador; // nombre/email/teléfono
    @FXML private javafx.scene.control.PasswordField pfLoginPassword;   // password login
    @FXML private Text lblUsuarioConectado;                             // "Usuario Conectado: ..."

    @FXML private javafx.scene.control.TextField txtRut;
    @FXML private javafx.scene.control.TextField txtNom;
    @FXML private javafx.scene.control.TextField txtApe;
    @FXML private javafx.scene.control.TextField txtEmail;
    @FXML private javafx.scene.control.TextField txtFono;               // <- Teléfono (fx:id debe ser txtFono)
    @FXML private javafx.scene.control.PasswordField pfRegPassword;     // password registro (separado)

    private Datos usuarioActual; // guardamos quién inició sesión

    @FXML
    public void initialize() {
        // ====== USUARIOS ======
        colUsuId.setCellValueFactory(new PropertyValueFactory<>("idUsuario"));
        colUsuNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUsuApellido.setCellValueFactory(new PropertyValueFactory<>("apellido"));
        colUsuEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUsuTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colUsuRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colUsuDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        // Verificador con persistencia
        colUsuVerificador.setCellValueFactory(cd -> {
            Datos fila = cd.getValue();
            var prop = new SimpleBooleanProperty(Boolean.TRUE.equals(fila.getVerificador()));
            prop.addListener((obs, oldV, newV) -> {
                fila.setVerificador(newV);
                ConexionBD.ensureAdminRow(fila.getIdUsuario());
                int n = ConexionBD.updateCampoAdmin(fila.getIdUsuario(), "Verificador", newV);
                System.out.println("Verificador guardado? filas=" + n + " id=" + fila.getIdUsuario());
            });
            return prop.asObject();
        });
        colUsuVerificador.setCellFactory(CheckBoxTableCell.forTableColumn(colUsuVerificador));
        colUsuVerificador.setEditable(true);

        // Password: value factory + enmascarado visual
        colUsuPassword.setCellValueFactory(new PropertyValueFactory<>("password")); // viene el HASH
        colUsuPassword.setCellFactory(col -> new TextFieldTableCell<>(new DefaultStringConverter()) {
            @Override public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                if (isEditing()) { setText(item == null ? "" : item); return; } // durante edición
                if (item == null || item.isEmpty()) { setText(""); return; }
                setText("•".repeat(Math.min(item.length(), 12))); // enmascarado
            }
        });

        tablaUsuarios.setItems(listaUsuarios);
        tablaUsuarios.setEditable(true);

        // Editables de texto (usuario)
        colUsuNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuApellido.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuTelefono.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuRol.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuDescripcion.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuPassword.setCellFactory(TextFieldTableCell.forTableColumn()); // escribir nueva clave -> se hashea abajo

        // Persistencia USUARIO
        colUsuNombre.setOnEditCommit(e -> editarCampoUsuario(e.getRowValue(), "Nombre",    e.getNewValue(), Datos::setNombre));
        colUsuApellido.setOnEditCommit(e -> editarCampoUsuario(e.getRowValue(), "Apellido", e.getNewValue(), Datos::setApellido));
        colUsuEmail.setOnEditCommit(e -> editarCampoUsuario(e.getRowValue(), "Email",      e.getNewValue(), Datos::setEmail));
        colUsuTelefono.setOnEditCommit(e -> editarCampoUsuario(e.getRowValue(), "Telefono", e.getNewValue(), Datos::setTelefono));

        // Persistencia PASSWORD (hash con BCrypt)
        colUsuPassword.setOnEditCommit(e -> {
            Datos r = e.getRowValue();
            String plano = e.getNewValue();
            if (plano != null && plano.length() > 255) {
                System.out.println("⚠️ Password muy largo (máx 255).");
                tablaUsuarios.refresh();
                return;
            }
            // vacío -> NULL
            String hash = (plano == null || plano.isBlank())
                    ? null
                    : BCrypt.withDefaults().hashToString(12, plano.toCharArray());

            int updated = ConexionBD.updateCampoUsuario(r.getIdUsuario(), "Password", hash);
            if (updated == 1) {
                r.setPassword(hash);
                System.out.println("✅ Password (hash) actualizado para ID=" + r.getIdUsuario());
            } else {
                System.out.println("⚠️ No se pudo actualizar Password");
            }
            tablaUsuarios.refresh();
        });

        // ====== INVENTARIO ======
        colProdId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colProdNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colProdDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colProdStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colProdHistorial.setCellValueFactory(new PropertyValueFactory<>("historialMovimiento"));
        colProdSucursales.setCellValueFactory(new PropertyValueFactory<>("editarSucursales"));

        tablaInventario.setItems(listaInventario);
        tablaInventario.setEditable(true);

        // Editables inventario
        colProdNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colProdDescripcion.setCellFactory(TextFieldTableCell.forTableColumn());
        colProdHistorial.setCellFactory(TextFieldTableCell.forTableColumn());
        colProdSucursales.setCellFactory(TextFieldTableCell.forTableColumn());
        colProdStock.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        // Persistencia PRODUCTO
        colProdNombre.setOnEditCommit(e -> {
            var r = e.getRowValue();
            r.setNombre(e.getNewValue());
            ConexionBD.updateCampoProducto(r.getIdProducto(), "Nombre", e.getNewValue());
        });
        colProdDescripcion.setOnEditCommit(e -> {
            var r = e.getRowValue();
            r.setDescripcion(e.getNewValue());
            ConexionBD.updateCampoProducto(r.getIdProducto(), "DESCRIPCION", e.getNewValue());
        });

        // Persistencia INVENTARIO
        colProdStock.setOnEditCommit(e -> {
            var r = e.getRowValue();
            Integer nuevo = e.getNewValue() == null ? 0 : e.getNewValue();
            r.setStock(nuevo);
            ConexionBD.ensureInventarioRow(r.getIdProducto());
            ConexionBD.updateCampoInventario(r.getIdProducto(), "Stock", String.valueOf(nuevo));
        });
        colProdHistorial.setOnEditCommit(e -> {
            var r = e.getRowValue();
            r.setHistorialMovimiento(e.getNewValue());
            ConexionBD.ensureInventarioRow(r.getIdProducto());
            ConexionBD.updateCampoInventario(r.getIdProducto(), "Historial_Movimiento", e.getNewValue());
        });
        colProdSucursales.setOnEditCommit(e -> {
            var r = e.getRowValue();
            r.setEditarSucursales(e.getNewValue());
            ConexionBD.ensureInventarioRow(r.getIdProducto());
            ConexionBD.updateCampoInventario(r.getIdProducto(), "Editar_Sucursales", e.getNewValue());
        });

        // Cargar datos
        cargarUsuarios();
        cargarInventario();
    }

    // -------- CARGA USUARIOS + ADMIN --------
    private void cargarUsuarios() {
        listaUsuarios.clear();
        final String sql =
                "SELECT u.ID_Usuario AS idUsuario, u.Nombre AS nombre, u.Apellido AS apellido, " +
                        "       u.Email AS email, u.Telefono AS telefono, u.Password AS password, " +
                        "       a.Rol AS rol, a.Descripcion AS descripcion, a.Verificador AS verificador " +
                        "FROM usuario u LEFT JOIN admin a ON a.ID_Usuario = u.ID_Usuario";

        try (var cn = ConexionBD.conectar(); var st = cn.createStatement(); var rs = st.executeQuery(sql)) {
            while (rs.next()) {
                listaUsuarios.add(new Datos(
                        rs.getInt("idUsuario"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("email"),
                        rs.getString("telefono"),
                        rs.getString("rol"),
                        rs.getString("descripcion"),
                        (rs.getObject("verificador") == null) ? null : rs.getInt("verificador") == 1,
                        rs.getString("password") // hash existente
                ));
            }
        } catch (Exception e) {
            System.out.println("❌ Error al cargar usuarios: " + e.getMessage());
        }
    }

    // -------- CARGA INVENTARIO (JOIN) --------
    private void cargarInventario() {
        listaInventario.clear();
        final String sql =
                "SELECT p.ID_Producto, p.Nombre, p.DESCRIPCION, " +
                        "       i.Stock, i.Historial_Movimiento, i.Editar_Sucursales " +
                        "FROM producto p " +
                        "LEFT JOIN inventario i ON i.ID_Producto = p.ID_Producto " +
                        "ORDER BY p.ID_Producto";

        try (var cn = ConexionBD.conectar(); var st = cn.createStatement(); var rs = st.executeQuery(sql)) {
            while (rs.next()) {
                listaInventario.add(new Datos.ProdInvRow(
                        rs.getInt("ID_Producto"),
                        rs.getString("Nombre"),
                        rs.getString("DESCRIPCION"),
                        rs.getObject("Stock") == null ? 0 : rs.getInt("Stock"),
                        rs.getString("Historial_Movimiento"),
                        rs.getString("Editar_Sucursales")
                ));
            }
        } catch (Exception e) {
            System.out.println("❌ Error al cargar inventario: " + e.getMessage());
        }
    }

    // ====== ACCIONES LOGIN / REGISTRO ======
    @FXML
    private void iniciarSesion() {
        String id = (txtLoginIdentificador == null) ? null : txtLoginIdentificador.getText().trim();
        String passPlano = (pfLoginPassword == null) ? null : pfLoginPassword.getText();

        if (id == null || id.isBlank() || passPlano == null || passPlano.isBlank()) {
            System.out.println("⚠️ Ingresa usuario/correo/teléfono y contraseña");
            return;
        }

        Datos u = ConexionBD.buscarUsuarioParaLogin(id);
        if (u == null || u.getPassword() == null || u.getPassword().isBlank()) {
            System.out.println("❌ Usuario no encontrado o sin password");
            return;
        }

        var res = BCrypt.verifyer().verify(passPlano.toCharArray(), u.getPassword());
        if (!res.verified) {
            System.out.println("❌ Contraseña incorrecta");
            return;
        }

        usuarioActual = u;
        String nombreMostrar = (u.getNombre() != null && !u.getNombre().isBlank())
                ? u.getNombre()
                : (u.getEmail() != null ? u.getEmail() : "¿?");

        if (lblUsuarioConectado != null) {
            lblUsuarioConectado.setText("Usuario Conectado: " + nombreMostrar);
        }
        System.out.println("✅ Sesión iniciada como " + nombreMostrar);
    }

    @FXML
    private void registrarUsuario() {
        String idUsuario = txtRut != null ? txtRut.getText().trim() : null;        // ID_Usuario (Rut)
        String nom       = txtNom != null ? txtNom.getText().trim() : null;
        String ape       = txtApe != null ? txtApe.getText().trim() : null;
        String email     = txtEmail != null ? txtEmail.getText().trim() : null;

        // Null-guard explícito para teléfono y log
        String fono = null;
        if (txtFono != null) {
            fono = txtFono.getText() == null ? null : txtFono.getText().trim();
        } else {
            System.out.println("⚠️ txtFono es null: revisa el fx:id del TextField Teléfono en el FXML");
        }

        String pass      = pfRegPassword != null ? pfRegPassword.getText() : null;

        if (idUsuario == null || idUsuario.isBlank()) {
            System.out.println("⚠️ El RUT/ID_Usuario es obligatorio para registrarse");
            return;
        }
        if (!idUsuario.matches("\\d+")) {
            System.out.println("⚠️ El RUT/ID_Usuario debe ser numérico (int)");
            return;
        }
        if (ConexionBD.existeIdUsuario(Integer.parseInt(idUsuario))) {
            System.out.println("❌ Ese ID_Usuario ya está registrado");
            return;
        }
        if (nom == null || nom.isBlank() || pass == null || pass.isBlank()) {
            System.out.println("⚠️ Nombre y contraseña son obligatorios");
            return;
        }
        if (fono != null && fono.length() > 15) {
            System.out.println("⚠️ Teléfono demasiado largo (máx 15)");
            return;
        }

        // Log de depuración — verifica que 'fono' venga con valor
        System.out.println("[DBG] registrar -> id=" + idUsuario +
                " nom=" + nom +
                " ape=" + ape +
                " email=" + email +
                " fono=" + fono);

        String hash = BCrypt.withDefaults().hashToString(12, pass.toCharArray());
        int filas = ConexionBD.registrarUsuario(
                Integer.parseInt(idUsuario), nom, ape, email, fono, hash
        );
        if (filas == 1) {
            System.out.println("✅ Usuario registrado");
            if (pfRegPassword != null) pfRegPassword.clear();
            cargarUsuarios();
        } else {
            System.out.println("❌ No se pudo registrar");
        }
    }

    // -------- UTILIDAD: guardar edición en USUARIO --------
    @FunctionalInterface
    private interface Setter<T> { void apply(Datos u, T val); }

    private void editarCampoUsuario(Datos datos, String columnaBD, String nuevoValor, Setter<String> setter) {
        if (datos == null) return;

        if ("Email".equals(columnaBD) && (nuevoValor == null || !nuevoValor.matches(".+@.+\\..+"))) {
            System.out.println("⚠️ Email inválido");
            tablaUsuarios.refresh();
            return;
        }
        if ("Telefono".equals(columnaBD) && nuevoValor != null && nuevoValor.length() > 15) {
            System.out.println("⚠️ Teléfono demasiado largo");
            tablaUsuarios.refresh();
            return;
        }

        try {
            int updated = ConexionBD.updateCampoUsuario(datos.getIdUsuario(), columnaBD, nuevoValor);
            if (updated == 1) {
                setter.apply(datos, nuevoValor);
                tablaUsuarios.refresh();
                System.out.println("✅ Guardado " + columnaBD + " para ID=" + datos.getIdUsuario());
            } else {
                System.out.println("⚠️ No se actualizó la fila");
            }
        } catch (Exception ex) {
            System.out.println("❌ Error al guardar: " + ex.getMessage());
            tablaUsuarios.refresh();
        }
    }
}
