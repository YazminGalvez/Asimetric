package basededatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BdJuegos {
    private static final Logger LOGGER = Logger.getLogger(BdJuegos.class.getName());

    private static final String SQL_ACTUALIZAR_RANKING = "INSERT INTO " + BaseDatos.TABLE_RANKING_GATO + " (id_usuario, puntos, victorias, derrotas, empates) VALUES (?, ?, ?, ?, ?)"
            + "ON CONFLICT(id_usuario) DO UPDATE SET puntos = puntos + excluded.puntos, victorias = victorias + excluded.victorias, derrotas = derrotas + excluded.derrotas, empates = empates + excluded.empates;";



    public static void registrarResultadoGato(String usuario1, String usuario2, String ganador) {
        try {
            Integer id1 = BdUsuario.obtenerIdUsuario(usuario1);
            Integer id2 = BdUsuario.obtenerIdUsuario(usuario2);
            Integer idGanador = (ganador != null) ? BdUsuario.obtenerIdUsuario(ganador) : null;

            if (id1 == null || id2 == null) {
                LOGGER.log(Level.WARNING, "Uno o ambos usuarios no existen.");
                return;
            }
            ejecutarTransaccionResultado(id1, id2, idGanador);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener IDs de usuarios para registro de partida.", e);
        }
    }

    private static void ejecutarTransaccionResultado(Integer id1, Integer id2, Integer idGanador) throws SQLException {
        try (Connection conn = BaseDatos.getConnection()) {
            conn.setAutoCommit(false);
            actualizarPuntuacion(conn, id1, id2, idGanador);
            insertarHistorial(conn, id1, id2, idGanador);
            conn.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar la partida de Gato.", e);
        }
    }

    private static void actualizarPuntuacion(Connection conn, Integer id1, Integer id2, Integer idGanador) throws SQLException {
        if (idGanador != null) {
            actualizarRanking(conn, idGanador, 2, 1, 0, 0);
            Integer idPerdedor = (idGanador.equals(id1)) ? id2 : id1;
            actualizarRanking(conn, idPerdedor, 0, 0, 1, 0);
        } else {
            actualizarRanking(conn, id1, 1, 0, 0, 1);
            actualizarRanking(conn, id2, 1, 0, 0, 1);
        }
    }

    private static void actualizarRanking(Connection conn, Integer idUsuario, int puntos, int victorias, int derrotas, int empates) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_ACTUALIZAR_RANKING)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, puntos);
            pstmt.setInt(3, victorias);
            pstmt.setInt(4, derrotas);
            pstmt.setInt(5, empates);
            pstmt.executeUpdate();
        }
    }

    private static void insertarHistorial(Connection conn, Integer id1, Integer id2, Integer idGanador) throws SQLException {
        String sql = "INSERT INTO " + BaseDatos.TABLE_HISTORIAL_PARTIDAS_GATO + " (id_jugador1, id_jugador2, id_ganador) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id1);
            pstmt.setInt(2, id2);
            if (idGanador != null) {
                pstmt.setInt(3, idGanador);
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
        }
    }



    public static List<Map<String, Object>> obtenerRankingGeneral() throws SQLException {
        String sql = "SELECT u.usuario, r.puntos, r.victorias, r.derrotas, r.empates FROM "
                + BaseDatos.TABLE_RANKING_GATO + " r JOIN " + BaseDatos.TABLE_USUARIOS + " u ON r.id_usuario = u.id "
                + "ORDER BY r.puntos DESC, r.victorias DESC, r.empates DESC, u.usuario ASC";

        try (Connection conn = BaseDatos.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return procesarResultSetRanking(rs);
        }
    }

    private static List<Map<String, Object>> procesarResultSetRanking(ResultSet rs) throws SQLException {
        List<Map<String, Object>> ranking = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("usuario", rs.getString("usuario"));
            fila.put("puntos", rs.getInt("puntos"));
            fila.put("victorias", rs.getInt("victorias"));
            fila.put("derrotas", rs.getInt("derrotas"));
            fila.put("empates", rs.getInt("empates"));
            ranking.add(fila);
        }
        return ranking;
    }



    public static Map<String, Object> obtenerEstadisticasVs(String usuario1, String usuario2) throws SQLException {
        Integer id1 = BdUsuario.obtenerIdUsuario(usuario1);
        Integer id2 = BdUsuario.obtenerIdUsuario(usuario2);

        if (id1 == null || id2 == null) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("error", "Uno o ambos usuarios no existen.");
            return stats;
        }
        return ejecutarQueryEstadisticas(id1, id2, usuario1, usuario2);
    }

    private static Map<String, Object> ejecutarQueryEstadisticas(Integer id1, Integer id2, String u1, String u2) throws SQLException {
        String sql = "SELECT id_ganador FROM " + BaseDatos.TABLE_HISTORIAL_PARTIDAS_GATO +
                " WHERE (id_jugador1 = ? AND id_jugador2 = ?) OR (id_jugador1 = ? AND id_jugador2 = ?)";

        try (Connection conn = BaseDatos.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id1);
            pstmt.setInt(2, id2);
            pstmt.setInt(3, id2);
            pstmt.setInt(4, id1);
            return procesarEstadisticasVs(pstmt.executeQuery(), id1, id2, u1, u2);
        }
    }

    private static Map<String, Object> procesarEstadisticasVs(ResultSet rs, Integer id1, Integer id2, String u1, String u2) throws SQLException {
        int total = 0, victorias1 = 0, victorias2 = 0;
        while (rs.next()) {
            total++;
            Integer idGanador = (Integer) rs.getObject("id_ganador");
            if (idGanador != null && idGanador.equals(id1)) victorias1++;
            else if (idGanador != null && idGanador.equals(id2)) victorias2++;
        }
        int empates = total - victorias1 - victorias2;
        return crearMapaEstadisticasVs(u1, u2, total, victorias1, victorias2, empates);
    }

    private static Map<String, Object> crearMapaEstadisticasVs(String u1, String u2, int total, int v1, int v2, int e) {
        Map<String, Object> stats = new HashMap<>();
        double pct1 = total > 0 ? (double) v1 / total * 100 : 0.0;
        double pct2 = total > 0 ? (double) v2 / total * 100 : 0.0;
        stats.put("usuario1", u1);
        stats.put("usuario2", u2);
        stats.put("total_partidas", total);
        stats.put("victorias1", v1);
        stats.put("victorias2", v2);
        stats.put("empates", e);
        stats.put("porcentaje_victorias1", String.format("%.2f%%", pct1));
        stats.put("porcentaje_victorias2", String.format("%.2f%%", pct2));
        return stats;
    }
}