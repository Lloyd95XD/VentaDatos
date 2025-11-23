package com.example.test2;// VentanaGrafico.java
import javax.swing.*;
import java.awt.*;

public class VentanaGrafico extends JFrame {

    private final String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
    private final int[] ventas = {45, 68, 58, 82, 95, 110, 125, 105, 98, 115, 130, 145};

    public VentanaGrafico() {
        setTitle("Gráficos de Ventas 2025");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new PanelGrafico(), BorderLayout.CENTER);
    }

    private class PanelGrafico extends JPanel {
        public PanelGrafico() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            dibujarGraficoBarras(g);
        }

        private void dibujarGraficoBarras(Graphics g) {
            int ancho = getWidth();
            int alto = getHeight();

            int margenIzq = 80;
            int margenInf = 100;
            int margenDer = 50;
            int margenSup = 60;

            int anchoUtil = ancho - margenIzq - margenDer;
            int altoUtil = alto - margenSup - margenInf;

            // Valor máximo para escalar
            int maximo = 150; // lo pongo fijo para que sea más bonito

            // Ejes
            g.setColor(Color.BLACK);
            g.drawLine(margenIzq, margenSup, margenIzq, alto - margenInf);                    // Y
            g.drawLine(margenIzq, alto - margenInf, ancho - margenDer, alto - margenInf);    // X

            // Marcas y etiquetas eje Y
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (int i = 0; i <= 10; i++) {
                int y = alto - margenInf - (altoUtil * i / 10);
                g.drawLine(margenIzq - 8, y, margenIzq, y);
                g.drawString(String.valueOf(i * 15), margenIzq - 45, y + 5);
            }

            // Dibujar las barras
            int anchoBarra = anchoUtil / meses.length;
            for (int i = 0; i < meses.length; i++) {
                int x = margenIzq + i * anchoBarra;
                int altura = (ventas[i] * altoUtil) / maximo;
                int y = alto - margenInf - altura;

                // Barra con gradiente bonito
                Color colorInicio = new Color(0, 123, 255);
                Color colorFin    = new Color(0, 180, 255);
                GradientPaint gradiente = new GradientPaint(x, y, colorInicio, x, y + altura, colorFin);
                ((Graphics2D) g).setPaint(gradiente);
                g.fillRect(x + 15, y, anchoBarra - 30, altura);

                // Borde
                g.setColor(Color.BLACK);
                g.drawRect(x + 15, y, anchoBarra - 30, altura);

                // Valor encima de la barra
                g.drawString(ventas[i] + "k", x + anchoBarra/2 - 15, y - 10);

                // Etiqueta del mes
                g.drawString(meses[i], x + anchoBarra/2 - 15, alto - margenInf + 25);
            }

            // Título
            g.setColor(new Color(0, 102, 204));
            g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            g.drawString("VENTAS MENSUALES 2025", ancho/2 - 150, 40);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaGrafico().setVisible(true));
    }
}