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
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarTabla();

        // Cargar categorías desde la lista editable
        comboCategoria.setItems(categorias);

        cargarProductos();

        // Detectar selección en tabla
        tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            productoSeleccionado = newSel;
            if (newSel != null) llenarFormularioDesdeProducto(newSel);
        });

        btnNuevoProducto.setOnAction(e -> limpiarFormulario());
        btnGuardarProducto.setOnAction(e -> guardarProducto());
        btnEliminarProducto.setOnAction(e -> eliminarProducto());
    }

    // ===================== CONFIG TABLA =====================
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
    private Connection getConnection() throws SQLException {
        Connection cn = ConexionBD.conectar();
        if (cn == null) throw new SQLException("No se pudo conectar a la BD.");
        return cn;
    }

    // ===================== CARGAR DATOS =====================
    private void cargarProductos() {
        listaProductos.clear();
        String sql = "SELECT Id_Producto, Categoria, Nombre, Descripcion, Stock, Precio FROM producto";

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
    private void llenarFormularioDesdeProducto(Producto p) {
        comboCategoria.setValue(p.getCategoria());
        txtNombreProducto.setText(p.getNombre());
        txtDescripcionProducto.setText(p.getDescripcion());
        txtStockProducto.setText(String.valueOf(p.getStock()));
        txtPrecioProducto.setText(String.valueOf(p.getPrecio())); // sin formato, para editar
    }

    private void limpiarFormulario() {
        tablaProductos.getSelectionModel().clearSelection();
        productoSeleccionado = null;

        comboCategoria.setValue(null);
        txtNombreProducto.clear();
        txtDescripcionProducto.clear();
        txtStockProducto.clear();
        txtPrecioProducto.clear();
    }

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
    private void guardarProducto() {
        if (!validarFormulario()) return;

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

    private void insertarProducto(String categoria, String nombre, String descripcion, int stock, int precio) {
        String sql = "INSERT INTO producto (Categoria, Nombre, Descripcion, Stock, Precio) VALUES (?,?,?,?,?)";

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

    private void actualizarProducto(int id, String categoria, String nombre, String descripcion, int stock, int precio) {
        String sql = "UPDATE producto SET Categoria=?, Nombre=?, Descripcion=?, Stock=?, Precio=? WHERE Id_Producto=?";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, categoria);
            ps.setString(2, nombre);
            ps.setString(3, descripcion);
            ps.setInt(4, stock);
            ps.setInt(5, precio);
            ps.setInt(6, id);

            ps.executeUpdate();

        } catch (SQLException e) {
            mostrarError("Error al actualizar producto", e.getMessage());
        }
    }

    // ===================== ELIMINAR =====================
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

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        String sql = "DELETE FROM producto WHERE Id_Producto=?";

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
    private String formatearCLP(int valor) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CL"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(valor);
    }

    // ===================== ALERTAS =====================
    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

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

    public Producto(int id, String cat, String nom, String desc, int stock, int precio) {
        this.idProducto.set(id);
        this.categoria.set(cat);
        this.nombre.set(nom);
        this.descripcion.set(desc);
        this.stock.set(stock);
        this.precio.set(precio);
    }

    public int getIdProducto() { return idProducto.get(); }
    public String getCategoria() { return categoria.get(); }
    public String getNombre() { return nombre.get(); }
    public String getDescripcion() { return descripcion.get(); }
    public int getStock() { return stock.get(); }
    public int getPrecio() { return precio.get(); }

    public IntegerProperty idProductoProperty() { return idProducto; }
    public StringProperty categoriaProperty() { return categoria; }
    public StringProperty nombreProperty() { return nombre; }
    public StringProperty descripcionProperty() { return descripcion; }
    public IntegerProperty stockProperty() { return stock; }
    public IntegerProperty precioProperty() { return precio; }
}
