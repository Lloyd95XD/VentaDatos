package com.example.test2;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

/**
 * Controlador para:
 *  - Tabla de Usuarios (con campos de admin via LEFT JOIN)
 *  - Tabla de Inventario (JOIN producto + inventario)
 *
 * Requisitos de FXML (fx:id):
 *
 *  // USUARIOS
 *  tablaUsuarios, colUsuId, colUsuNombre, colUsuApellido, colUsuEmail, colUsuTelefono,
 *  colUsuRol, colUsuDescripcion, colUsuVerificador
 *
 *  // INVENTARIO
 *  tablaInventario, colProdId, colProdNombre, colProdDescripcion, colProdStock,
 *  colProdHistorial, colProdSucursales
 */
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

    private final ObservableList<Datos> listaUsuarios = FXCollections.observableArrayList();

    // -------- TABLA INVENTARIO (PRODUCTO + INVENTARIO) --------
    @FXML private TableView<ProdInvRow> tablaInventario;

    @FXML private TableColumn<ProdInvRow, Integer> colProdId;
    @FXML private TableColumn<ProdInvRow, String>  colProdNombre;
    @FXML private TableColumn<ProdInvRow, String>  colProdDescripcion;
    @FXML private TableColumn<ProdInvRow, Integer> colProdStock;
    @FXML private TableColumn<ProdInvRow, String>  colProdHistorial;
    @FXML private TableColumn<ProdInvRow, String>  colProdSucursales;

    private final ObservableList<ProdInvRow> listaInventario = FXCollections.observableArrayList();

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

        // Verificador como property que persiste al cambiar
        colUsuVerificador.setCellValueFactory(cd -> {
            Datos fila = cd.getValue();
            var prop = new SimpleBooleanProperty(Boolean.TRUE.equals(fila.getVerificador()));
            prop.addListener((obs, oldV, newV) -> {
                fila.setVerificador(newV);
                ConexionBD.ensureAdminRow(fila.getIdUsuario());
                int n = ConexionBD.updateCampoAdmin(fila.getIdUsuario(), "Verificador", newV);
                System.out.println("Verificador guardado? filas=" + n + " id=" + fila.getIdUsuario() + " -> " + (newV?1:0));
            });
            return prop.asObject();
        });
        colUsuVerificador.setCellFactory(CheckBoxTableCell.forTableColumn(colUsuVerificador));
        colUsuVerificador.setEditable(true);

        tablaUsuarios.setItems(listaUsuarios);
        tablaUsuarios.setEditable(true);

        // Editables de texto (usuario)
        colUsuNombre.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuApellido.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuTelefono.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuRol.setCellFactory(TextFieldTableCell.forTableColumn());
        colUsuDescripcion.setCellFactory(TextFieldTableCell.forTableColumn());

        // Persistencia USUARIO
        colUsuNombre.setOnEditCommit(e -> editarCampoUsuario(e.getRowValue(), "Nombre",    e.getNewValue(), Datos::setNombre));
        colUsuApellido.setOnEditCommit(e -> editarCampoUsuario(e.getRowValue(), "Apellido", e.getNewValue(), Datos::setApellido));
        colUsuEmail.setOnEditCommit(e -> editarCampoUsuario(e.getRowValue(), "Email",      e.getNewValue(), Datos::setEmail));
        colUsuTelefono.setOnEditCommit(e -> editarCampoUsuario(e.getRowValue(), "Telefono", e.getNewValue(), Datos::setTelefono));

        // Persistencia ADMIN
        colUsuRol.setOnEditCommit(e -> {
            Datos r = e.getRowValue();
            r.setRol(e.getNewValue());
            ConexionBD.ensureAdminRow(r.getIdUsuario());
            ConexionBD.updateCampoAdmin(r.getIdUsuario(), "Rol", e.getNewValue());
        });
        colUsuDescripcion.setOnEditCommit(e -> {
            Datos r = e.getRowValue();
            r.setDescripcion(e.getNewValue());
            ConexionBD.ensureAdminRow(r.getIdUsuario());
            ConexionBD.updateCampoAdmin(r.getIdUsuario(), "Descripcion", e.getNewValue());
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

        // Editables
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

        // Cargar datos iniciales
        cargarUsuarios();
        cargarInventario();
    }

    // -------- CARGA USUARIOS + ADMIN --------
    private void cargarUsuarios() {
        listaUsuarios.clear();
        final String sql =
                "SELECT u.ID_Usuario AS idUsuario, u.Nombre AS nombre, u.Apellido AS apellido, " +
                        "u.Email AS email, u.Telefono AS telefono, " +
                        "a.Rol AS rol, a.Descripcion AS descripcion, a.Verificador AS verificador " +
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
                        (rs.getObject("verificador") == null) ? null : rs.getInt("verificador") == 1
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
                listaInventario.add(new ProdInvRow(
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
