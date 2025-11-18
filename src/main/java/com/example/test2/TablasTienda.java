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

        int stockDisponible = seleccionado.getStock();

        // Cuántas unidades de este producto YA hay en el carrito
        int cantidadEnCarrito = 0;
        ItemCarrito itemExistente = null;

        for (ItemCarrito item : listaCarrito) {
            if (item.getIdProducto() == seleccionado.getIdProducto()) {
                cantidadEnCarrito = item.getCantidad();
                itemExistente = item;
                break;
            }
        }

        // Si ya llegamos al stock máximo, no dejamos agregar más
        if (cantidadEnCarrito >= stockDisponible) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Stock máximo alcanzado");
            alert.setHeaderText(null);
            alert.setContentText("Llegaste al stock máximo de la tienda para \""
                    + seleccionado.getNombre() + "\" (" + stockDisponible + " unidades).");
            alert.showAndWait();
            return;
        }

        // Si ya estaba, solo aumentamos la cantidad
        if (itemExistente != null) {
            itemExistente.setCantidad(itemExistente.getCantidad() + 1);
            tablaCarrito.refresh();  // actualiza la columna Cantidad
        } else {
            // Si no estaba, lo agregamos con cantidad 1
            ItemCarrito nuevo = new ItemCarrito(
                    seleccionado.getIdProducto(),
                    seleccionado.getNombre(),
                    seleccionado.getPrecio(),
                    1
            );
            listaCarrito.add(nuevo);
        }

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
        int total = calcularTotalCarrito();
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

    // ==========================
    //  IR A PAGAR
    // ==========================
    @FXML
    private void irAPagar() {
        // No dejar pasar si el carrito está vacío
        if (listaCarrito.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Carrito vacío");
            alert.setHeaderText(null);
            alert.setContentText("Debes agregar al menos un producto al carrito antes de pagar.");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ZonaDePago.fxml"));
            Parent root = loader.load();

            // Obtener el controlador de la Zona de Pago
            ZonaPagoController controller = loader.getController();

            // Pasar carrito y total
            int total = calcularTotalCarrito();
            controller.setCarritoYTotal(
                    FXCollections.observableArrayList(listaCarrito), // copia
                    total
            );

            // Si quisieras también podrías pasar el id de usuario así:
            // controller.setIdUsuario(UsuarioSesion.getIdUsuario());

            // 🔁 REEMPLAZAR LA VENTANA ACTUAL
            // Usa cualquier nodo de la escena actual, por ejemplo tablaProductos
            Stage stage = (Stage) tablaProductos.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            System.out.println("❌ Error al abrir Zona de Pago: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private int calcularTotalCarrito() {
        int total = 0;
        for (ItemCarrito item : listaCarrito) {
            total += item.getPrecio() * item.getCantidad();
        }
        return total;
    }
    @FXML
    private Button btnVolver; // ← usa el botón real que tengas en tu FXML
    @FXML
    private void volverMenu1() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("ventanaIniciadalista.fxml"));

            // Obtener la ventana actual desde el botón
            Stage stage = (Stage) btnVolver.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
