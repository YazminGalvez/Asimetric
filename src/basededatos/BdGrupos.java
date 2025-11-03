package basededatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BdGrupos {
    private static final Logger LOGGER = Logger.getLogger(BdGrupos.class.getName());


    public static void inicializarGrupos() {
        try {
            Integer idTodos = obtenerIdGrupo("Todos");
            if (idTodos == null) {
                crearGrupo("Todos", null);
                idTodos = obtenerIdGrupo("Todos");
            }
            if (idTodos != null) {
                actualizarMiembrosGrupoTodos(idTodos);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar grupos.", e);
        }
    }

    private static void actualizarMiembrosGrupoTodos(Integer idGrupo) throws SQLException {
        String sql = "SELECT id FROM " + BaseDatos.TABLE_USUARIOS;
        try (Connection conn = BaseDatos.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                unirseAGrupo(rs.getInt("id"), idGrupo);
            }
        }
    }


    public static Integer obtenerIdGrupo(String nombre) throws SQLException {
        String sql = "SELECT id FROM " + BaseDatos.TABLE_GRUPOS + " WHERE nombre = ?";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
            return null;
        }
    }

    public static String obtenerCreadorGrupo(String nombreGrupo) throws SQLException {
        String sql = "SELECT u.usuario FROM " + BaseDatos.TABLE_GRUPOS + " g JOIN " + BaseDatos.TABLE_USUARIOS + " u ON g.id_creador = u.id WHERE g.nombre = ?";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("usuario");
            return null;
        }
    }


    public static boolean crearGrupo(String nombre, String usuarioCreador) {
        String sql = "INSERT INTO " + BaseDatos.TABLE_GRUPOS + " (nombre, id_creador) VALUES (?, ?)";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Integer idCreador = BdUsuario.obtenerIdUsuario(usuarioCreador);
            pstmt.setString(1, nombre);
            pstmt.setObject(2, idCreador);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) return false;
            LOGGER.log(Level.SEVERE, "Error al crear el grupo: " + nombre, e);
            return false;
        }
    }

    public static boolean unirseAGrupo(String usuario, String grupo) {
        try {
            Integer idUsuario = BdUsuario.obtenerIdUsuario(usuario);
            Integer idGrupo = obtenerIdGrupo(grupo);
            if (idUsuario == null || idGrupo == null) return false;
            return unirseAGrupo(idUsuario, idGrupo);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al unir usuario al grupo.", e);
            return false;
        }
    }

    public static boolean unirseAGrupo(Integer idUsuario, Integer idGrupo) throws SQLException {
        String sql = "INSERT OR IGNORE INTO " + BaseDatos.TABLE_MIEMBROS_GRUPO + " (id_usuario, id_grupo) VALUES (?, ?)";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, idGrupo);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) actualizarUltimoVisto(idUsuario, idGrupo, 0);
            return rowsAffected > 0;
        }
    }

    public static boolean salirDeGrupo(String usuario, String grupo) {
        try {
            Integer idUsuario = BdUsuario.obtenerIdUsuario(usuario);
            Integer idGrupo = obtenerIdGrupo(grupo);
            if (idUsuario == null || idGrupo == null) return false;
            salirDeGrupo(idUsuario, idGrupo);
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al salir usuario del grupo.", e);
            return false;
        }
    }

    private static void salirDeGrupo(Integer idUsuario, Integer idGrupo) throws SQLException {
        String sql = "DELETE FROM " + BaseDatos.TABLE_MIEMBROS_GRUPO + " WHERE id_usuario = ? AND id_grupo = ?";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, idGrupo);
            pstmt.executeUpdate();
        }
    }

    public static boolean eliminarGrupo(String nombre, String usuario) {
        try {
            Integer idGrupo = obtenerIdGrupo(nombre);
            Integer idUsuario = BdUsuario.obtenerIdUsuario(usuario);
            if (idGrupo == null || idUsuario == null) return false;
            return validarCreadorYEliminar(idGrupo, idUsuario);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar grupo.", e);
            return false;
        }
    }

    private static boolean validarCreadorYEliminar(Integer idGrupo, Integer idCreador) throws SQLException {
        String sql = "DELETE FROM " + BaseDatos.TABLE_GRUPOS + " WHERE id = ? AND id_creador = ?";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idGrupo);
            pstmt.setInt(2, idCreador);
            return pstmt.executeUpdate() > 0;
        }
    }


    public static boolean esMiembroDeGrupo(String usuario, String grupo) {
        String sql = "SELECT 1 FROM " + BaseDatos.TABLE_MIEMBROS_GRUPO + " m JOIN " + BaseDatos.TABLE_USUARIOS + " u ON m.id_usuario = u.id JOIN " + BaseDatos.TABLE_GRUPOS + " g ON m.id_grupo = g.id WHERE u.usuario = ? AND g.nombre = ?";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            pstmt.setString(2, grupo);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar membresía de grupo.", e);
            return false;
        }
    }

    public static List<String> obtenerMiembrosDeGrupo(String grupo) {
        List<String> miembros = new ArrayList<>();
        String sql = "SELECT u.usuario FROM " + BaseDatos.TABLE_MIEMBROS_GRUPO + " m JOIN " + BaseDatos.TABLE_USUARIOS + " u ON m.id_usuario = u.id JOIN " + BaseDatos.TABLE_GRUPOS + " g ON m.id_grupo = g.id WHERE g.nombre = ?";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, grupo);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) miembros.add(rs.getString("usuario"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener miembros de grupo.", e);
        }
        return miembros;
    }

    public static long guardarMensajeGrupo(String grupo, String remitente, String mensaje) {
        String sql = "INSERT INTO " + BaseDatos.TABLE_MENSAJES_GRUPO + " (id_grupo, id_remitente, contenido) VALUES (?, ?, ?)";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Integer idGrupo = obtenerIdGrupo(grupo);
            Integer idRemitente = BdUsuario.obtenerIdUsuario(remitente);
            if (idGrupo == null) return -1L;
            pstmt.setInt(1, idGrupo);
            pstmt.setObject(2, idRemitente);
            pstmt.setString(3, mensaje);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
            return -1L;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar mensaje de grupo.", e);
            return -1L;
        }
    }



    public static List<String> obtenerMensajesNoVistos(String usuario, String grupo) {
        List<String> mensajes = new ArrayList<>();
        try {
            Integer idUsuario = BdUsuario.obtenerIdUsuario(usuario);
            Integer idGrupo = obtenerIdGrupo(grupo);
            if (idUsuario == null || idGrupo == null) return mensajes;

            long ultimoVisto = obtenerUltimoMensajeVisto(idUsuario, idGrupo);
            long maxId = ejecutarQueryMensajesNoVistos(idGrupo, ultimoVisto, mensajes);

            if (maxId > ultimoVisto) actualizarUltimoVisto(idUsuario, idGrupo, maxId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener mensajes no vistos.", e);
        }
        return mensajes;
    }

    private static long ejecutarQueryMensajesNoVistos(Integer idGrupo, long ultimoVisto, List<String> mensajes) throws SQLException {
        String sql = "SELECT m.id, m.contenido, u.usuario FROM " + BaseDatos.TABLE_MENSAJES_GRUPO + " m LEFT JOIN " + BaseDatos.TABLE_USUARIOS + " u ON m.id_remitente = u.id WHERE m.id_grupo = ? AND m.id > ? ORDER BY m.id ASC";
        long maxId = ultimoVisto;

        try (Connection conn = BaseDatos.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idGrupo);
            pstmt.setLong(2, ultimoVisto);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                mensajes.add(formatearMensaje(rs));
                maxId = rs.getLong("id");
            }
        }
        return maxId;
    }

    private static String formatearMensaje(ResultSet rs) throws SQLException {
        String remitente = rs.getString("usuario");
        String contenido = rs.getString("contenido");
        return (remitente == null ? "[Sistema]" : remitente) + ": " + contenido;
    }

    private static long obtenerUltimoMensajeVisto(Integer idUsuario, Integer idGrupo) throws SQLException {
        String sql = "SELECT id_ultimo_mensaje_visto FROM " + BaseDatos.TABLE_ULTIMO_VISTO + " WHERE id_usuario = ? AND id_grupo = ?";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, idGrupo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getLong(1);
            return 0;
        }
    }

    private static void actualizarUltimoVisto(Integer idUsuario, Integer idGrupo, long idMensaje) throws SQLException {
        String sql = "INSERT OR REPLACE INTO " + BaseDatos.TABLE_ULTIMO_VISTO + " (id_usuario, id_grupo, id_ultimo_mensaje_visto) VALUES (?, ?, ?)";
        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, idGrupo);
            pstmt.setLong(3, idMensaje);
            pstmt.executeUpdate();
        }
    }
}