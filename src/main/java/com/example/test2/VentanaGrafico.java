package com.example.test2;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class VentanaGrafico implements Initializable {

    // ====== Formato CLP ======
    private static final NumberFormat formatoCL =
            NumberFormat.getInstance(new Locale("es", "CL"));

    // ====== Gráfico ======
    @FXML private LineChart<String, Number> lineChartVentas;
    @FXML private CategoryAxis ejeX;
    @FXML private NumberAxis ejeY;

    // ====== Resumen inferior ======
    @FXML private Label lblVentasHoy;          // CLP hoy
    @FXML private Label lblVentasMes;          // CLP mes

    @FXML private Label lblTotalRegistros;     // total boletas
    @FXML private Label TextoERROR;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (ejeX != null) ejeX.setLabel("Fecha");
        if (ejeY != null) ejeY.setLabel("Monto (CLP)");

        cargarGraficoPorDia();      // Ventas por día
        cargarUsuariosPorDia();     // Usuarios registrados por día
        cargarResumen();            // Labels de abajo
    }

    // =====================================================
    //   VENTAS AGRUPADAS POR DÍA
    // =====================================================
    private void cargarGraficoPorDia() {
        if (lineChartVentas == null) return;

        lineChartVentas.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Ventas");

        String sql =
                "SELECT DATE(Hora_de_venta) AS dia, " +
                        "       SUM(Precio_Total) AS total_dia " +
                        "FROM venta " +
                        "GROUP BY DATE(Hora_de_venta) " +
                        "ORDER BY dia";

        Connection cn = ConexionBD.conectar();
        if (cn == null) {
            TextoERROR.setText("No se pudo conectar para cargar ventas por día.");

            return;
        }

        try (cn;
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String fecha = rs.getDate("dia").toString();
                double total = rs.getDouble("total_dia");
                serie.getData().add(new XYChart.Data<>(fecha, total));
            }

        } catch (Exception e) {

            TextoERROR.setText("Error cargando gráfico de ventas por día: " + e.getMessage());
        }

        lineChartVentas.getData().add(serie);
    }

    // =====================================================
    //   USUARIOS REGISTRADOS POR DÍA (cantidad)
    // =====================================================
    private void cargarUsuariosPorDia() {
        if (lineChartVentas == null) return;

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Usuarios registrados");

        String sql =
                "SELECT Fecha_creacion_de_cuenta AS dia, " +
                        "       COUNT(*) AS total " +
                        "FROM usuario " +
                        "GROUP BY Fecha_creacion_de_cuenta " +
                        "ORDER BY dia";

        Connection cn = ConexionBD.conectar();
        if (cn == null) {

            TextoERROR.setText("No se pudo conectar para cargar usuarios por día.");
            return;
        }

        try (cn;
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String fecha = rs.getDate("dia").toString();
                int total = rs.getInt("total");
                serie.getData().add(new XYChart.Data<>(fecha, total));
            }

        } catch (Exception e) {

            TextoERROR.setText("Error cargando gráfico de usuarios por día: " + e.getMessage());
        }

        // Se agrega la serie encima de la de ventas
        lineChartVentas.getData().add(serie);
    }

    // =====================================================
    //   RESUMEN: ventas hoy, este mes, usuarios hoy, total ventas
    // =====================================================
    private void cargarResumen() {

        String sqlVentasHoy =
                "SELECT IFNULL(SUM(Precio_Total), 0) AS total " +
                        "FROM venta " +
                        "WHERE DATE(Hora_de_venta) = CURDATE()";

        String sqlVentasMes =
                "SELECT IFNULL(SUM(Precio_Total), 0) AS total " +
                        "FROM venta " +
                        "WHERE YEAR(Hora_de_venta) = YEAR(CURDATE()) " +
                        "  AND MONTH(Hora_de_venta) = MONTH(CURDATE())";

        String sqlTotalRegistros =
                "SELECT COUNT(*) AS total FROM venta";

        // 🔹 Usuarios registrados HOY en la tabla usuario

        Connection cn = ConexionBD.conectar();
        if (cn == null) {

            TextoERROR.setText("No se pudo conectar para cargar resumen.");
            return;
        }

        try (cn) {
            // Ventas hoy
            try (PreparedStatement ps = cn.prepareStatement(sqlVentasHoy);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && lblVentasHoy != null) {
                    lblVentasHoy.setText("CLP: " + formatoCL.format(rs.getInt("total")));
                }
            }

            // Ventas este mes
            try (PreparedStatement ps = cn.prepareStatement(sqlVentasMes);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && lblVentasMes != null) {
                    lblVentasMes.setText("CLP: " + formatoCL.format(rs.getInt("total")));
                }
            }

            // Usuarios registrados HOY


            // Total registros (boletas)
            try (PreparedStatement ps = cn.prepareStatement(sqlTotalRegistros);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && lblTotalRegistros != null) {
                    lblTotalRegistros.setText(
                            String.valueOf(rs.getInt("total"))
                    );
                }
            }

        } catch (Exception e) {

            TextoERROR.setText("Error cargando resumen: " + e.getMessage());
        }
    }

    // =====================================================
    //   (Opcional) Botones si quieres refrescar o cambiar vista
    // =====================================================
    @FXML
    private void verVentasPorDia() {
        cargarGraficoPorDia();
        cargarUsuariosPorDia(); // volvemos a añadir la serie de usuarios
    }

    @FXML
    private void refrescarPanel() {
        cargarGraficoPorDia();
        cargarUsuariosPorDia();
        cargarResumen();
    }
    @FXML
    private Button btnCambiarVentana;

    @FXML

    private void volvermenuprincipal() {
        try {
            // Cargar el archivo FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MenuIniciadaSesionListoV2.fxml"));
            Parent root = loader.load();

            // Obtener el Stage actual
            Stage stage = (Stage) btnCambiarVentana.getScene().getWindow(); // Puedes usar cualquier nodo

            // Cambiar la escena
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
