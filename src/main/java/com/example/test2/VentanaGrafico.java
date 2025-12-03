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
    @FXML private Label lblVentasHoy;
    @FXML private Label lblVentasMes;
    @FXML private Label lblTotalRegistros;
    @FXML private Label TextoERROR;

    @FXML private Button btnCambiarVentana;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        if (ejeX != null) ejeX.setLabel("Fecha");
        if (ejeY != null) ejeY.setLabel("Monto (CLP)");

        // Cargar gráfico y resumen usando procedimientos almacenados
        cargarGraficoPorDia();
        cargarResumen();
    }

    // =====================================================
    //   VENTAS AGRUPADAS POR DÍA (ÚNICA LÍNEA) - SP
    //   Usa: sp_ventas_por_dia()
    // =====================================================
    private void cargarGraficoPorDia() {
        if (lineChartVentas == null) return;

        lineChartVentas.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Ventas por día");

        String sql = "{ CALL sp_ventas_por_dia() }";

        Connection cn = ConexionBD.conectar();
        if (cn == null) {
            if (TextoERROR != null) {
                TextoERROR.setText("No se pudo conectar para cargar ventas por día.");
            }
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

            lineChartVentas.getData().add(serie);

        } catch (Exception e) {
            if (TextoERROR != null) {
                TextoERROR.setText("Error cargando gráfico de ventas: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    // =====================================================
    //   RESUMEN: ventas hoy, ventas mes, total registros
    //   Usa: sp_resumen_ventas()
    // =====================================================
    private void cargarResumen() {

        String sqlResumen = "{ CALL sp_resumen_ventas() }";

        Connection cn = ConexionBD.conectar();
        if (cn == null) {
            if (TextoERROR != null) {
                TextoERROR.setText("No se pudo conectar para cargar resumen.");
            }
            return;
        }

        try (cn;
             PreparedStatement ps = cn.prepareStatement(sqlResumen);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int totalHoy        = rs.getInt("total_hoy");
                int totalMes        = rs.getInt("total_mes");
                int totalRegistros  = rs.getInt("total_registros");

                if (lblVentasHoy != null) {
                    lblVentasHoy.setText("CLP: " + formatoCL.format(totalHoy));
                }
                if (lblVentasMes != null) {
                    lblVentasMes.setText("CLP: " + formatoCL.format(totalMes));
                }
                if (lblTotalRegistros != null) {
                    lblTotalRegistros.setText(String.valueOf(totalRegistros));
                }
            }

        } catch (Exception e) {
            if (TextoERROR != null) {
                TextoERROR.setText("Error cargando resumen: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    // =====================================================
    //   BOTONES
    // =====================================================
    @FXML
    private void verVentasPorDia() {
        cargarGraficoPorDia();
    }

    @FXML
    private void refrescarPanel() {
        cargarGraficoPorDia();
        cargarResumen();
    }

    @FXML
    private void volvermenuprincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("MenuiniciadasesionListoV2.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) btnCambiarVentana.getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
