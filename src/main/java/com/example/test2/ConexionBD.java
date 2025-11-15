package com.example.test2;

import java.sql.Connection;
import java.sql.DriverManager;
//
public class ConexionBD {
    private static final String URL  = "jdbc:mysql://localhost:3306/gestor_de_ventas?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = ""; // sin contraseña (XAMPP por defecto)

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            System.out.println("❌ Conexión fallida: " + e.getMessage());
            return null;
        }
    }

    // ==================== USUARIOS ====================
    public static int updateCampoUsuario(int idUsuario, String columna, String valor) {
        final String sql = "UPDATE usuario SET " + columna + " = ? WHERE ID_Usuario = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("❌ update usuario: " + e.getMessage());
            return 0;
        }
    }

    // ==================== ADMIN ====================
    public static void ensureAdminRow(int idUsuario) {
        final String check  = "SELECT COUNT(*) FROM admin WHERE ID_Usuario = ?";
        final String insert = "INSERT INTO admin (ID_Usuario, Rol, Descripcion, Verificador) VALUES (?, '', '', 0)";
        try (var cn = conectar(); var psC = cn.prepareStatement(check)) {
            psC.setInt(1, idUsuario);
            try (var rs = psC.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (var psI = cn.prepareStatement(insert)) {
                        psI.setInt(1, idUsuario);
                        psI.executeUpdate();
                        System.out.println("✅ admin creado para ID=" + idUsuario);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ ensureAdminRow: " + e.getMessage());
        }
    }

    public static int updateCampoAdmin(int idUsuario, String columna, String valor) {
        final String sql = "UPDATE admin SET " + columna + " = ? WHERE ID_Usuario = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("❌ update admin (string): " + e.getMessage());
            return 0;
        }
    }

    public static int updateCampoAdmin(int idUsuario, String columna, boolean val) {
        final String sql = "UPDATE admin SET " + columna + " = ? WHERE ID_Usuario = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setInt(1, val ? 1 : 0);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("❌ update admin (bool): " + e.getMessage());
            return 0;
        }
    }

    // ==================== PRODUCTO ====================
    public static int updateCampoProducto(int idProducto, String columna, String valor) {
        final String sql = "UPDATE producto SET " + columna + " = ? WHERE ID_Producto = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, idProducto);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("❌ update producto: " + e.getMessage());
            return 0;
        }
    }

    // ==================== INVENTARIO ====================
    public static void ensureInventarioRow(int idProducto) {
        final String check  = "SELECT COUNT(*) FROM inventario WHERE ID_Producto = ?";
        final String insert = "INSERT INTO inventario (ID_Producto, Stock, Historial_Movimiento, Editar_Sucursales) " +
                "VALUES (?, 0, '', '')";
        try (var cn = conectar(); var psC = cn.prepareStatement(check)) {
            psC.setInt(1, idProducto);
            try (var rs = psC.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (var psI = cn.prepareStatement(insert)) {
                        psI.setInt(1, idProducto);
                        psI.executeUpdate();
                        System.out.println("✅ inventario creado para producto ID=" + idProducto);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ ensureInventarioRow: " + e.getMessage());
        }
    }

    public static int updateCampoInventario(int idProducto, String columna, String valor) {
        final String sql = "UPDATE inventario SET " + columna + " = ? WHERE ID_Producto = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, idProducto);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("❌ update inventario: " + e.getMessage());
            return 0;
        }
    }
}
