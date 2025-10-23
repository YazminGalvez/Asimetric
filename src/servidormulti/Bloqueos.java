package servidormulti;
import basededatos.BaseDatos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Bloqueos {

    private static final Logger LOGGER = Logger.getLogger(Bloqueos.class.getName());

    public static boolean bloquearUsuario(String bloqueador, String bloqueado) {
        if (bloqueador.equalsIgnoreCase(bloqueado)) {
            return false;
        }
        try (Connection conn = BaseDatos.getConnection()) {
            Integer idBloqueador = BaseDatos.obtenerIdUsuario(bloqueador);
            Integer idBloqueado = BaseDatos.obtenerIdUsuario(bloqueado);

            if (idBloqueador == null || idBloqueado == null) {
                return false;
            }
            String sql = "INSERT INTO " + BaseDatos.TABLE_BLOQUEOS + "(id_bloqueador, id_bloqueado) VALUES(?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idBloqueador);
                pstmt.setInt(2, idBloqueado);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                return false;
            }
            LOGGER.log(Level.SEVERE, "Error al bloquear el usuario: " + bloqueador + " -> " + bloqueado, e);
            return false;
        }
    }

    public static boolean desbloquearUsuario(String bloqueador, String bloqueado) {
        try (Connection conn = BaseDatos.getConnection()) {
            Integer idBloqueador = BaseDatos.obtenerIdUsuario(bloqueador);
            Integer idBloqueado = BaseDatos.obtenerIdUsuario(bloqueado);

            if (idBloqueador == null || idBloqueado == null) {
                return false;
            }

            String sql = "DELETE FROM " + BaseDatos.TABLE_BLOQUEOS + " WHERE id_bloqueador = ? AND id_bloqueado = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idBloqueador);
                pstmt.setInt(2, idBloqueado);
                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al desbloquear el usuario: " + bloqueador + " -> " + bloqueado, e);
            return false;
        }
    }

    public static boolean estaBloqueadoPor(String emisor, String receptor) {
        try (Connection conn = BaseDatos.getConnection()) {
            Integer idEmisor = BaseDatos.obtenerIdUsuario(emisor);
            Integer idReceptor = BaseDatos.obtenerIdUsuario(receptor);

            if (idEmisor == null || idReceptor == null) {
                return false;
            }

            String sql = "SELECT COUNT(*) FROM " + BaseDatos.TABLE_BLOQUEOS
                    + " WHERE id_bloqueador = ? AND id_bloqueado = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idReceptor);
                pstmt.setInt(2, idEmisor);
                ResultSet rs = pstmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar bloqueo: " + emisor + " por " + receptor, e);
            return false;
        }
    }
}