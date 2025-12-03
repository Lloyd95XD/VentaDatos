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
        final String sql = "{ CALL sp_actualizar_campo_usuario(?, ?, ?) }";
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
        final String sql = "{ CALL sp_ensure_admin_row(?) }";

        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, idUsuario);
            ps.execute();
            System.out.println("admin creado/verificado para ID=" + idUsuario);
        } catch (Exception e) {
            System.out.println("ensureAdminRow: " + e.getMessage());
        }
    }

    public static int updateCampoAdmin(String idUsuario, String columna, String valor) {
        final String sql = "{ CALL sp_actualizar_campo_admin(?, ?, ?) }";

        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setString(1, idUsuario);
            ps.setString(2, columna);
            ps.setString(3, valor);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("update admin (string): " + e.getMessage());
            return 0;
        }
    }


    // ==================== PRODUCTO ====================
    // Estos siguen usando int porque Id_Producto sigue siendo INT
    public static int updateCampoProducto(int idProducto, String columna, String valor) {
        final String sql = "{ CALL sp_actualizar_campo_producto(?, ?, ?) }";

        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setString(2, columna);
            ps.setString(3, valor);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("update producto: " + e.getMessage());
            return 0;
        }
    }


    // ==================== INVENTARIO ====================
    public static void ensureInventarioRow(int idProducto) {
        final String sql = "{ CALL sp_ensure_inventario_row(?) }";

        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.execute();
            System.out.println("inventario creado/verificado para producto ID=" + idProducto);
        } catch (Exception e) {
            System.out.println("ensureInventarioRow: " + e.getMessage());
        }
    }


    public static int updateCampoInventario(int idProducto, String columna, String valor) {
        final String sql = "{ CALL sp_actualizar_campo_inventario(?, ?, ?) }";

        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setString(2, columna);
            ps.setString(3, valor);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("update inventario: " + e.getMessage());
            return 0;
        }
    }

}
