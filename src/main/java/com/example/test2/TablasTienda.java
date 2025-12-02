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

        // Cantidad solo números
        if (colCantidadAdd != null) {
            colCantidadAdd.textProperty().addListener((obs, old, nuevo) -> {
                if (!nuevo.matches("\\d*")) {
                    colCantidadAdd.setText(nuevo.replaceAll("[^\\d]", ""));
                }
            });
            colCantidadAdd.setText("1");
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

            // Cuando selecciono algo en el carrito, cargo su cantidad en el TextField
            tablaCarrito.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldSel, nuevoSel) -> {
                        if (nuevoSel != null && colCantidadAdd != null) {
                            colCantidadAdd.setText(String.valueOf(nuevoSel.getCantidad()));
                        }
                    });
        }
    }

    // ==========================
    //  AGREGAR / EDITAR CARRITO
    // ==========================
    @FXML
    private void agregarAlCarrito() {

        String txtCant = colCantidadAdd.getText().trim();
        if (txtCant.isEmpty()) {
            mostrarAlerta("Ingresa una cantidad válida.");
            return;
        }

        int cantidadIngresada;
        try {
            cantidadIngresada = Integer.parseInt(txtCant);
        } catch (NumberFormatException e) {
            mostrarAlerta("La cantidad debe ser numérica.");
            return;
        }

        if (cantidadIngresada <= 0) {
            mostrarAlerta("La cantidad debe ser mayor a 0.");

            return;
        }

        // 1) Si hay un producto seleccionado en el carrito -> EDITAR cantidad
        ItemCarrito itemCarritoSel = tablaCarrito.getSelectionModel().getSelectedItem();
        if (itemCarritoSel != null) {

            // Buscar el producto original para conocer el stock
            MueblesControlador producto = null;
            for (MueblesControlador m : listaProductos) {
                if (m.getIdProducto() == itemCarritoSel.getIdProducto()) {
                    producto = m;
                    break;
                }
            }

            if (producto == null) {
                mostrarAlerta("No se encontró el producto asociado.");
                return;
            }

            int stock = producto.getStock();
            if (cantidadIngresada > stock) {
                mostrarAlerta("Stock insuficiente. Máximo disponible: " + stock);
                return;
            }

            // Editar cantidad exacta
            itemCarritoSel.setCantidad(cantidadIngresada);
            tablaCarrito.refresh();
            actualizarTotal();
            colCantidadAdd.setText("1");
            return;
        }

        // 2) Si no hay selección en el carrito -> AÑADIR desde la tabla de productos
        MueblesControlador seleccionado =
                tablaProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Selecciona un producto en la lista de productos.");
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

        if (cantidadIngresada + cantidadEnCarrito > stock) {
            mostrarAlerta("Stock insuficiente. Máximo disponible: " +
                    (stock - cantidadEnCarrito));
            return;
        }

        if (itemExistente != null) {
            itemExistente.setCantidad(itemExistente.getCantidad() + cantidadIngresada);
            tablaCarrito.refresh();
        } else {
            ItemCarrito nuevo = new ItemCarrito(
                    seleccionado.getIdProducto(),
                    seleccionado.getNombre(),
                    seleccionado.getPrecio(),
                    cantidadIngresada
            );
            listaCarrito.add(nuevo);
        }

        actualizarTotal();
<<<<<<< HEAD
        //colCantidadAdd.clear();//
=======
>>>>>>> 905c37f (MegaCambioooo)
        colCantidadAdd.setText("1");
    }

    // ==========================
    //  ELIMINAR / VACIAR
    // ==========================
    @FXML
    private void eliminarDelCarrito() {
        ItemCarrito seleccionado = tablaCarrito.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        // Ahora elimina el producto completo del carrito
        listaCarrito.remove(seleccionado);
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
        nf.setMaximumFractionDigits(0);
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
