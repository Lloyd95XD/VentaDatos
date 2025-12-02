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
            System.out.println("Conexión fallida: " + e.getMessage());
            return null;
        }
    }

    // ==================== USUARIOS ====================
    // Id_Usuario ahora es VARCHAR(15) → usamos String
    public static int updateCampoUsuario(String idUsuario, String columna, String valor) {
        final String sql = "UPDATE usuario SET " + columna + " = ? WHERE Id_Usuario = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setString(2, idUsuario);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("update usuario: " + e.getMessage());
            return 0;
        }
    }

    // ==================== ADMIN ====================
    // También asumimos que admin.Id_Usuario ahora es VARCHAR(15)
    public static void ensureAdminRow(String idUsuario) {
        final String check  = "SELECT COUNT(*) FROM admin WHERE Id_Usuario = ?";
        final String insert = "INSERT INTO admin (Id_Usuario, Rol, Descripcion, Verificador) VALUES (?, '', '', 0)";
        try (var cn = conectar(); var psC = cn.prepareStatement(check)) {
            psC.setString(1, idUsuario);
            try (var rs = psC.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (var psI = cn.prepareStatement(insert)) {
                        psI.setString(1, idUsuario);
                        psI.executeUpdate();
                        System.out.println("admin creado para ID=" + idUsuario);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("ensureAdminRow: " + e.getMessage());
        }
    }

    public static int updateCampoAdmin(String idUsuario, String columna, String valor) {
        final String sql = "UPDATE admin SET " + columna + " = ? WHERE Id_Usuario = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setString(2, idUsuario);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("update admin (string): " + e.getMessage());
            return 0;
        }
    }

    public static int updateCampoAdmin(String idUsuario, String columna, boolean val) {
        final String sql = "UPDATE admin SET " + columna + " = ? WHERE Id_Usuario = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setInt(1, val ? 1 : 0);
            ps.setString(2, idUsuario);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("update admin (bool): " + e.getMessage());
            return 0;
        }
    }

    // ==================== PRODUCTO ====================
    // Estos siguen usando int porque Id_Producto sigue siendo INT
    public static int updateCampoProducto(int idProducto, String columna, String valor) {
        final String sql = "UPDATE producto SET " + columna + " = ? WHERE Id_Producto = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, idProducto);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("update producto: " + e.getMessage());
            return 0;
        }
    }

    // ==================== INVENTARIO ====================
    public static void ensureInventarioRow(int idProducto) {
        final String check  = "SELECT COUNT(*) FROM inventario WHERE Id_Producto = ?";
        final String insert = "INSERT INTO inventario (Id_Producto, Stock, Historial_Movimiento, Editar_Sucursales) " +
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
            System.out.println("ensureInventarioRow: " + e.getMessage());
        }
    }

    public static int updateCampoInventario(int idProducto, String columna, String valor) {
        final String sql = "UPDATE inventario SET " + columna + " = ? WHERE Id_Producto = ?";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setInt(2, idProducto);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("update inventario: " + e.getMessage());
            return 0;
        }
    }
}
