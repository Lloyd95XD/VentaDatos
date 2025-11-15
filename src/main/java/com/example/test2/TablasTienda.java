package com.example.test2;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
//
public class TablasTienda implements Initializable {

    // ==========================
    //  TABLA DE PRODUCTOS
    // ==========================
    @FXML private TableView<MueblesControlador> tablaProductos;
    @FXML private TableColumn<MueblesControlador, Integer> colId;
    @FXML private TableColumn<MueblesControlador, String>  colNombre;
    @FXML private TableColumn<MueblesControlador, String>  colCategoria;
    @FXML private TableColumn<MueblesControlador, Integer> colCantidad;
    @FXML private TableColumn<MueblesControlador, Integer> colPrecio;

    @FXML private TextArea txtDescripcion;

    private final ObservableList<MueblesControlador> listaProductos =
            FXCollections.observableArrayList();

    // ==========================
    //  TABLA DEL CARRITO
    // ==========================
    @FXML private TableView<ItemCarrito> tablaCarrito;
    @FXML private TableColumn<ItemCarrito, String>  colCarritoNombre;
    @FXML private TableColumn<ItemCarrito, Integer> colCarritoPrecio;
    @FXML private TableColumn<ItemCarrito, Integer> colCarritoCantidad;

    @FXML private Text lblMontoTotal;

    private final ObservableList<ItemCarrito> listaCarrito =
            FXCollections.observableArrayList();

    // ==========================
    //  INITIALIZE
    // ==========================
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        if (tablaProductos != null) {
            configurarColumnasProductos();
            cargarProductos();
            configurarDescripcion();
        }

        if (tablaCarrito != null) {
            configurarColumnasCarrito();
            tablaCarrito.setItems(listaCarrito);
            actualizarTotal();
        }
    }

    // ==========================
    //  CARRITO - ACCIONES
    // ==========================

    @FXML
    private void agregarAlCarrito() {
        MueblesControlador seleccionado =
                tablaProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            System.out.println("⚠ Selecciona un mueble primero");
            return;
        }

        // Si el producto ya está en el carrito, solo aumentamos la cantidad
        for (ItemCarrito item : listaCarrito) {
            if (item.getIdProducto() == seleccionado.getIdProducto()) {
                item.setCantidad(item.getCantidad() + 1);
                tablaCarrito.refresh();  // actualiza la columna Cantidad
                actualizarTotal();
                return;
            }
        }

        // Si no estaba, lo agregamos con cantidad 1
        ItemCarrito nuevo = new ItemCarrito(
                seleccionado.getIdProducto(),
                seleccionado.getNombre(),
                seleccionado.getPrecio(),
                1
        );
        listaCarrito.add(nuevo);
        actualizarTotal();
    }

    @FXML
    private void eliminarDelCarrito() {
        ItemCarrito seleccionado =
                tablaCarrito.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            System.out.println("⚠ Selecciona algo del carrito para eliminar");
            return;
        }

        // Si hay más de 1, restamos uno; si no, eliminamos la fila
        if (seleccionado.getCantidad() > 1) {
            seleccionado.setCantidad(seleccionado.getCantidad() - 1);
            tablaCarrito.refresh();
        } else {
            listaCarrito.remove(seleccionado);
        }

        actualizarTotal();
    }

    @FXML
    private void vaciarCarrito() {
        listaCarrito.clear();
        actualizarTotal();
    }

    private void actualizarTotal() {
        int total = 0;
        for (ItemCarrito item : listaCarrito) {
            total += item.getPrecio() * item.getCantidad();
        }

        if (lblMontoTotal != null) {
            lblMontoTotal.setText("Monto Total $ " + total);
        }
    }

    private void configurarColumnasCarrito() {
        colCarritoNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colCarritoPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precio"));
        colCarritoCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));
    }

    // ==========================
    //  PRODUCTOS
    // ==========================

    private void configurarColumnasProductos() {
        colId.setCellValueFactory(
                new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(
                new PropertyValueFactory<>("categoria"));
        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("stock"));
        colPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precio"));
    }

    private void cargarProductos() {
        listaProductos.clear();

        String sql = "SELECT Id_Producto, Categoria, Nombre, " +
                "Descripcion, Stock, Precio FROM Producto";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                MueblesControlador p = new MueblesControlador(
                        rs.getInt("Id_Producto"),
                        rs.getString("Categoria"),
                        rs.getString("Nombre"),
                        rs.getString("Descripcion"),
                        rs.getInt("Stock"),
                        rs.getInt("Precio")
                );
                listaProductos.add(p);
            }

            tablaProductos.setItems(listaProductos);

        } catch (Exception e) {
            System.out.println("❌ Error cargando productos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarDescripcion() {
        if (tablaProductos == null || txtDescripcion == null) return;

        tablaProductos.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, viejo, nuevo) -> {
                    if (nuevo != null) {
                        txtDescripcion.setText(nuevo.getDescripcion());
                    } else {
                        txtDescripcion.clear();
                    }
                });
    }
}

