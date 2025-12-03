package com.example.test2;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class MueblesTablaController implements Initializable {

    // ===================== LISTA DE CATEGORÍAS EDITABLE =====================
    private final ObservableList<String> categorias = FXCollections.observableArrayList(
            "Sofá",
            "Mesa",
            "Silla",
            "Cama",
            "Velador",
            "Escritorio",
            "Estante",
            "Mueble TV",
            "Ropero",
            "Otro"
    );

    // ===================== ELEMENTOS FXML =====================
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Integer> colIdProducto;
    @FXML private TableColumn<Producto, String>  colCategoria;
    @FXML private TableColumn<Producto, String>  colNombre;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, Integer> colPrecio;

    @FXML private ComboBox<String> comboCategoria;
    @FXML private TextField txtNombreProducto;
    @FXML private TextArea txtDescripcionProducto;
    @FXML private TextField txtStockProducto;
    @FXML private TextField txtPrecioProducto;

    @FXML private Button btnNuevoProducto;
    @FXML private Button btnGuardarProducto;
    @FXML private Button btnEliminarProducto;

    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();
    private Producto productoSeleccionado = null;

    // ===================== INICIALIZACIÓN =====================
    /// Inicializa el controlador, configura la tabla y carga los datos
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarTabla();

        // Cargar categorías desde la lista editable
        comboCategoria.setItems(categorias);

        cargarProductos();

        // Detectar selección en tabla
        tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            productoSeleccionado = newSel;
            if (newSel != null) {
                llenarFormularioDesdeProducto(newSel);
            }
        });

        btnNuevoProducto.setOnAction(e -> limpiarFormulario());
        btnGuardarProducto.setOnAction(e -> guardarProducto());
        btnEliminarProducto.setOnAction(e -> eliminarProducto());
    }

    // ===================== CONFIG TABLA =====================
    /// Configura las columnas de la tabla de productos
    private void configurarTabla() {
        colIdProducto.setCellValueFactory(data -> data.getValue().idProductoProperty().asObject());
        colCategoria.setCellValueFactory(data -> data.getValue().categoriaProperty());
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colStock.setCellValueFactory(data -> data.getValue().stockProperty().asObject());
        colPrecio.setCellValueFactory(data -> data.getValue().precioProperty().asObject());

        // Formatear la columna de precio como CLP (con puntos)
        colPrecio.setCellFactory(col -> new TableCell<Producto, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatearCLP(item));
                }
            }
        });

        tablaProductos.setItems(listaProductos);
    }

    // ===================== CONEXIÓN =====================
    /// Obtiene una conexion a la base de datos
    private Connection getConnection() throws SQLException {
        Connection cn = ConexionBD.conectar();
        if (cn == null) {
            throw new SQLException("No se pudo conectar a la BD.");
        }
        return cn;
    }

    // ===================== CARGAR DATOS =====================
    /// Carga la lista de productos desde la base de datos
    private void cargarProductos() {
        listaProductos.clear();

        final String sql = "{ CALL sp_listar_productos() }";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaProductos.add(new Producto(
                        rs.getInt("Id_Producto"),
                        rs.getString("Categoria"),
                        rs.getString("Nombre"),
                        rs.getString("Descripcion"),
                        rs.getInt("Stock"),
                        rs.getInt("Precio")
                ));
            }

        } catch (SQLException e) {
            mostrarError("Error al cargar productos", e.getMessage());
        }
    }

    // ===================== FORMULARIO =====================
    /// Rellena el formulario con los datos del producto seleccionado
    private void llenarFormularioDesdeProducto(Producto p) {
        comboCategoria.setValue(p.getCategoria());
        txtNombreProducto.setText(p.getNombre());
        txtDescripcionProducto.setText(p.getDescripcion());
        txtStockProducto.setText(String.valueOf(p.getStock()));
        txtPrecioProducto.setText(String.valueOf(p.getPrecio())); // sin formato, para editar
    }

    /// Limpia los campos del formulario de producto
    private void limpiarFormulario() {
        tablaProductos.getSelectionModel().clearSelection();
        productoSeleccionado = null;

        comboCategoria.setValue(null);
        txtNombreProducto.clear();
        txtDescripcionProducto.clear();
        txtStockProducto.clear();
        txtPrecioProducto.clear();
    }

    /// Valida que los datos ingresados en el formulario sean correctos
    private boolean validarFormulario() {
        if (comboCategoria.getValue() == null) {
            mostrarAlerta("Selecciona una categoría.");
            return false;
        }
        if (txtNombreProducto.getText().isBlank()) {
            mostrarAlerta("El nombre no puede estar vacío.");
            return false;
        }
        if (!txtStockProducto.getText().matches("\\d+")) {
            mostrarAlerta("Stock debe ser un número entero.");
            return false;
        }
        if (!txtPrecioProducto.getText().matches("\\d+")) {
            mostrarAlerta("Precio debe ser un número entero.");
            return false;
        }
        return true;
    }

    // ===================== GUARDAR =====================
    /// Guarda un producto nuevo o actualiza uno existente
    private void guardarProducto() {
        if (!validarFormulario()) {
            return;
        }

        String categoria = comboCategoria.getValue();
        String nombre = txtNombreProducto.getText().trim();
        String descripcion = txtDescripcionProducto.getText().trim();
        int stock = Integer.parseInt(txtStockProducto.getText());
        int precio = Integer.parseInt(txtPrecioProducto.getText());

        if (productoSeleccionado == null) {
            insertarProducto(categoria, nombre, descripcion, stock, precio);
        } else {
            actualizarProducto(productoSeleccionado.getIdProducto(), categoria, nombre, descripcion, stock, precio);
        }

        cargarProductos();
        limpiarFormulario();
    }

    /// Inserta un nuevo producto en la base de datos
    private void insertarProducto(String categoria, String nombre, String descripcion, int stock, int precio) {
        final String sql = "{ CALL sp_insertar_producto(?, ?, ?, ?, ?) }";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, categoria);
            ps.setString(2, nombre);
            ps.setString(3, descripcion);
            ps.setInt(4, stock);
            ps.setInt(5, precio);

            ps.executeUpdate();

        } catch (SQLException e) {
            mostrarError("Error al insertar producto", e.getMessage());
        }
    }

    /// Actualiza los datos de un producto existente
    private void actualizarProducto(int id, String categoria, String nombre,
                                    String descripcion, int stock, int precio) {

        final String sql = "{ CALL sp_actualizar_producto(?, ?, ?, ?, ?, ?) }";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setString(2, categoria);
            ps.setString(3, nombre);
            ps.setString(4, descripcion);
            ps.setInt(5, stock);
            ps.setInt(6, precio);

            ps.executeUpdate();

        } catch (SQLException e) {
            mostrarError("Error al actualizar producto", e.getMessage());
        }
    }

    // ===================== ELIMINAR =====================
    /// Elimina el producto seleccionado de la base de datos
    private void eliminarProducto() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selecciona un producto para eliminar.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar el producto \"" + seleccionado.getNombre() + "\"?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        final String sql = "{ CALL sp_eliminar_producto(?) }";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, seleccionado.getIdProducto());
            ps.executeUpdate();

        } catch (SQLException e) {
            mostrarError("Error al eliminar producto", e.getMessage());
        }

        cargarProductos();
        limpiarFormulario();
    }

    // ===================== VOLVER AL MENÚ =====================
    /// Regresa a la ventana del menu principal
    @FXML
    private void volverAlMenu() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("MenuIniciadasesionListoV2.fxml")
            );
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage)
                    tablaProductos.getScene().getWindow();

            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

        } catch (Exception e) {
            mostrarError("Error al volver al menú", e.getMessage());
        }
    }

    // ===================== FORMATO CLP =====================
    /// Formatea el valor a moneda chilena
    private String formatearCLP(int valor) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CL"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(valor);
    }

    // ===================== ALERTAS =====================
    /// Muestra una alerta de advertencia
    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /// Muestra una alerta de error
    private void mostrarError(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

// ===================== CLASE PRODUCTO =====================
class Producto {
    private final IntegerProperty idProducto = new SimpleIntegerProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final StringProperty nombre = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final IntegerProperty stock = new SimpleIntegerProperty();
    private final IntegerProperty precio = new SimpleIntegerProperty();

    /// Constructor de la clase Producto
    public Producto(int id, String cat, String nom, String desc, int stock, int precio) {
        this.idProducto.set(id);
        this.categoria.set(cat);
        this.nombre.set(nom);
        this.descripcion.set(desc);
        this.stock.set(stock);
        this.precio.set(precio);
    }

    /// Obtiene:
    public int getIdProducto() { return idProducto.get(); }
    public String getCategoria() { return categoria.get(); }
    public String getNombre() { return nombre.get(); }
    public String getDescripcion() { return descripcion.get(); }
    public int getStock() { return stock.get(); }
    public int getPrecio() { return precio.get(); }

    /// Propiedad de:
    public IntegerProperty idProductoProperty() { return idProducto; }
    public StringProperty categoriaProperty() { return categoria; }
    public StringProperty nombreProperty() { return nombre; }
    public StringProperty descripcionProperty() { return descripcion; }
    public IntegerProperty stockProperty() { return stock; }
    public IntegerProperty precioProperty() { return precio; }
}
