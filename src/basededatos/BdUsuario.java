package basededatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BdUsuario {
    private static final Logger LOGGER = Logger.getLogger(BdUsuario.class.getName());

    public static boolean registrarUsuario(String usuario, String contrasena) {
        String sql = "INSERT INTO " + BaseDatos.TABLE_USUARIOS + "(usuario, contrasena) VALUES(?, ?)";

        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            pstmt.setString(2, contrasena);
            pstmt.executeUpdate();
            BdGrupos.unirseAGrupo(usuario, "Todos");
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) return false;
            LOGGER.log(Level.SEVERE, "Error al registrar el usuario: " + usuario, e);
            return false;
        }
    }

    public static boolean verificarCredenciales(String usuario, String contrasena) {
        String sql = "SELECT contrasena FROM " + BaseDatos.TABLE_USUARIOS + " WHERE usuario = ?";

        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPass = rs.getString("contrasena");
                return contrasena.equals(storedPass);
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar credenciales para: " + usuario, e);
            return false;
        }
    }

    public static Integer obtenerIdUsuario(String usuario) throws SQLException {
        String sql = "SELECT id FROM " + BaseDatos.TABLE_USUARIOS + " WHERE usuario = ?";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
            return null;
        }
    }
}