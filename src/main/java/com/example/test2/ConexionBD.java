package com.example.test2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

    // ==================== USUARIOS: CRUD CAMPOS ====================
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

    // ==================== USUARIOS: LOGIN / REGISTRO ====================
    /** Busca por email, nombre; y si el identificador es numérico, también por teléfono. */
    public static Datos buscarUsuarioParaLogin(String identificador) {
        final String sql =
                "SELECT ID_Usuario, Nombre, Apellido, Email, Telefono, Password " +
                        "FROM usuario WHERE Email = ? OR Nombre = ? OR Telefono = ? LIMIT 1";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            String tel = (identificador != null && identificador.matches("\\d+")) ? identificador : "__no_tel__";
            ps.setString(1, identificador);
            ps.setString(2, identificador);
            ps.setString(3, tel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Datos(
                            rs.getInt("ID_Usuario"),
                            rs.getString("Nombre"),
                            rs.getString("Apellido"),
                            rs.getString("Email"),
                            rs.getString("Telefono"),
                            null, null, null,
                            rs.getString("Password")
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("❌ buscarUsuarioParaLogin: " + e.getMessage());
        }
        return null;
    }

    /** ¿Existe ya el ID_Usuario (PK INT)? */
    public static boolean existeIdUsuario(int idUsuario) {
        final String sql = "SELECT 1 FROM usuario WHERE ID_Usuario = ? LIMIT 1";
        try (var cn = conectar(); var ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            System.out.println("❌ existeIdUsuario: " + e.getMessage());
            return true; // por seguridad evita duplicado si hay error
        }
    }

    /** Inserta usuario con PK ID_Usuario y Telefono. Devuelve filas afectadas. */
    public static int registrarUsuario(int idUsuario, String nombre, String apellido,
                                       String email, String telefono, String hashPassword) {
        final String sql =
                "INSERT INTO usuario (ID_Usuario, Nombre, Apellido, Email, Telefono, Password) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
        try (var cn = conectar(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, nombre);
            ps.setString(3, apellido);
            ps.setString(4, email);
            ps.setString(5, telefono);   // <- AQUÍ se guarda el teléfono
            ps.setString(6, hashPassword);
            int n = ps.executeUpdate();
            System.out.println("✅ INSERT usuario filas=" + n + " (ID=" + idUsuario + ", tel=" + telefono + ")");
            return n;
        } catch (Exception e) {
            System.out.println("❌ registrarUsuario: " + e.getMessage());
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
        final String insert = "INSERT INTO inventario (ID_Producto, Stock, Historial_Movimiento, Editar_Sucursales) VALUES (?, 0, '', '')";
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
