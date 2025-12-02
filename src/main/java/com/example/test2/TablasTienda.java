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
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class TablasTienda implements Initializable {

    // ==========================
    //  PRODUCTOS
    // ==========================
    @FXML private TableView<MueblesControlador> tablaProductos;
    @FXML private TableColumn<MueblesControlador, Integer> colId;
    @FXML private TableColumn<MueblesControlador, String> colNombre;
    @FXML private TableColumn<MueblesControlador, String> colCategoria;
    @FXML private TableColumn<MueblesControlador, Integer> colCantidad;
    @FXML private TableColumn<MueblesControlador, Integer> colPrecio;

    @FXML private TextArea txtDescripcion;

    private final ObservableList<MueblesControlador> listaProductos =
            FXCollections.observableArrayList();

    // ==========================
    //  CARRITO
    // ==========================
    @FXML private TableView<ItemCarrito> tablaCarrito;
    @FXML private TableColumn<ItemCarrito, String>  colCarritoNombre;
    @FXML private TableColumn<ItemCarrito, Integer> colCarritoPrecio;
    @FXML private TableColumn<ItemCarrito, Integer> colCarritoCantidad;

    @FXML private Text lblMontoTotal;

    @FXML private TextField colCantidadAdd;

    private final ObservableList<ItemCarrito> listaCarrito =
            FXCollections.observableArrayList();

    // ==========================
    //  INITIALIZE
    // ==========================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // --- Restringir la cantidad solo a números ---
        if (colCantidadAdd != null) {
            colCantidadAdd.textProperty().addListener((obs, old, nuevo) -> {
                if (!nuevo.matches("\\d*")) {
                    colCantidadAdd.setText(nuevo.replaceAll("[^\\d]", ""));
                }
            });
        }

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
    //  AGREGAR AL CARRITO
    // ==========================
    @FXML
    private void agregarAlCarrito() {

        MueblesControlador seleccionado =
                tablaProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Selecciona un producto primero.");
            return;
        }

        String txt = colCantidadAdd.getText().trim();
        if (txt.isEmpty()) {
            mostrarAlerta("Ingresa una cantidad válida.");
            return;
        }

        int cantidadSolicitada = Integer.parseInt(txt);
        if (cantidadSolicitada <= 0) {
            mostrarAlerta("La cantidad debe ser mayor a 0.");
            return;
        }

        int stock = seleccionado.getStock();

        ItemCarrito itemExistente = null;
        int cantidadEnCarrito = 0;

        for (ItemCarrito item : listaCarrito) {
            if (item.getIdProducto() == seleccionado.getIdProducto()) {
                itemExistente = item;
                cantidadEnCarrito = item.getCantidad();
                break;
            }
        }

        if (cantidadSolicitada + cantidadEnCarrito > stock) {
            mostrarAlerta("Stock insuficiente. Máximo disponible: " +
                    (stock - cantidadEnCarrito));
            return;
        }

        if (itemExistente != null) {
            itemExistente.setCantidad(itemExistente.getCantidad() + cantidadSolicitada);
            tablaCarrito.refresh();
        } else {
            ItemCarrito nuevo = new ItemCarrito(
                    seleccionado.getIdProducto(),
                    seleccionado.getNombre(),
                    seleccionado.getPrecio(),
                    cantidadSolicitada
            );
            listaCarrito.add(nuevo);
        }

        actualizarTotal();
        colCantidadAdd.clear();
    }

    // ==========================
    //  ELIMINAR / VACIAR
    // ==========================
    @FXML
    private void eliminarDelCarrito() {
        ItemCarrito seleccionado = tablaCarrito.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

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

    // ==========================
    //  TOTAL
    // ==========================
    private void actualizarTotal() {
        int total = 0;
        for (ItemCarrito item : listaCarrito) {
            total += item.getPrecio() * item.getCantidad();
        }
        lblMontoTotal.setText("Monto Total $ " + formatearCLP(total));
    }

    // ==========================
    //  COLUMNAS CARRITO
    // ==========================
    private void configurarColumnasCarrito() {
        colCarritoNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCarritoPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colCarritoCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        // Formato CLP en la columna de precio del carrito
        colCarritoPrecio.setCellFactory(col -> new TableCell<ItemCarrito, Integer>() {
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
    }

    // ==========================
    //  PRODUCTOS
    // ==========================
    private void configurarColumnasProductos() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));

        // Formato CLP en la columna de precio de productos
        colPrecio.setCellFactory(col -> new TableCell<MueblesControlador, Integer>() {
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
    }

    private void cargarProductos() {
        listaProductos.clear();

        String sql = "SELECT Id_Producto, Categoria, Nombre, Descripcion, Stock, Precio FROM Producto";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                listaProductos.add(new MueblesControlador(
                        rs.getInt("Id_Producto"),
                        rs.getString("Categoria"),
                        rs.getString("Nombre"),
                        rs.getString("Descripcion"),
                        rs.getInt("Stock"),
                        rs.getInt("Precio")
                ));
            }

            tablaProductos.setItems(listaProductos);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configurarDescripcion() {
        tablaProductos.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, nuevo) -> {
                    if (nuevo != null)
                        txtDescripcion.setText(nuevo.getDescripcion());
                    else
                        txtDescripcion.clear();
                });
    }

    // ==========================
    //  FORMATO CLP
    // ==========================
    private String formatearCLP(int valor) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CL"));
        nf.setMaximumFractionDigits(0); // sin decimales
        nf.setMinimumFractionDigits(0);
        return nf.format(valor);
    }

    // ==========================
    //  METODO ALERTA
    // ==========================
    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ==========================
    //  IR A PAGAR
    // ==========================
    @FXML
    private void irAPagar() {
        if (listaCarrito.isEmpty()) {
            mostrarAlerta("El carrito está vacío.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ZonaDePago.fxml"));
            Parent root = loader.load();

            ZonaPagoController controller = loader.getController();
            controller.setCarritoYTotal(
                    FXCollections.observableArrayList(listaCarrito),
                    calcularTotal()
            );

            Stage stage = (Stage) tablaProductos.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int calcularTotal() {
        int total = 0;
        for (ItemCarrito item : listaCarrito)
            total += item.getPrecio() * item.getCantidad();
        return total;
    }

    // ==========================
    //  VOLVER
    // ==========================
    @FXML private Button btnVolver;

    @FXML
    private void volverMenu1() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("MenuIniciadasesionListoV2.fxml"));
            Stage stage = (Stage) btnVolver.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
