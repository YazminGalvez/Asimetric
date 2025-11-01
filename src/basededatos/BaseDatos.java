package basededatos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class BaseDatos {
    private static final String DB_FILE = "usuarios.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    public static final String TABLE_USUARIOS = "usuarios";
    public static final String TABLE_BLOQUEOS = "bloqueos";
    public static final String TABLE_RANKING_GATO = "ranking_gato";
    public static final String TABLE_HISTORIAL_PARTIDAS_GATO = "historial_partidas_gato";

    public static final String TABLE_GRUPOS = "grupos";
    public static final String TABLE_MIEMBROS_GRUPO = "miembros_grupo";
    public static final String TABLE_MENSAJES_GRUPO = "mensajes_grupo";
    public static final String TABLE_ULTIMO_VISTO = "ultimo_visto";


    private static final Logger LOGGER = Logger.getLogger(BaseDatos.class.getName());

    static {
        initializeDatabase();
        inicializarGrupos();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void initializeDatabase() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS " + TABLE_USUARIOS + " (\n"
                + " id integer PRIMARY KEY,\n"
                + " usuario text NOT NULL UNIQUE,\n"
                + " contrasena text NOT NULL\n"
                + ");";
        String sqlBloqueos = "CREATE TABLE IF NOT EXISTS " + TABLE_BLOQUEOS + " (\n"
                + " id_bloqueador integer NOT NULL,\n"
                + " id_bloqueado integer NOT NULL,\n"
                + " PRIMARY KEY (id_bloqueador, id_bloqueado),\n"
                + " FOREIGN KEY (id_bloqueador) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_bloqueado) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE\n"
                + ");";

        String sqlRankingGato = "CREATE TABLE IF NOT EXISTS " + TABLE_RANKING_GATO + " (\n"
                + " id_usuario integer PRIMARY KEY,\n"
                + " puntos integer NOT NULL DEFAULT 0,\n"
                + " victorias integer NOT NULL DEFAULT 0,\n"
                + " derrotas integer NOT NULL DEFAULT 0,\n"
                + " empates integer NOT NULL DEFAULT 0,\n"
                + " FOREIGN KEY (id_usuario) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE\n"
                + ");";

        String sqlHistorialGato = "CREATE TABLE IF NOT EXISTS " + TABLE_HISTORIAL_PARTIDAS_GATO + " (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " id_jugador1 integer NOT NULL,\n"
                + " id_jugador2 integer NOT NULL,\n"
                + " id_ganador integer,\n"
                + " FOREIGN KEY (id_jugador1) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_jugador2) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE\n"
                + ");";

        String sqlGrupos = "CREATE TABLE IF NOT EXISTS " + TABLE_GRUPOS + " (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " nombre text NOT NULL UNIQUE,\n"
                + " id_creador integer,\n"
                + " FOREIGN KEY (id_creador) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE SET NULL\n"
                + ");";
        String sqlMiembros = "CREATE TABLE IF NOT EXISTS " + TABLE_MIEMBROS_GRUPO + " (\n"
                + " id_usuario integer NOT NULL,\n"
                + " id_grupo integer NOT NULL,\n"
                + " PRIMARY KEY (id_usuario, id_grupo),\n"
                + " FOREIGN KEY (id_usuario) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_grupo) REFERENCES " + TABLE_GRUPOS + "(id) ON DELETE CASCADE\n"
                + ");";
        String sqlMensajes = "CREATE TABLE IF NOT EXISTS " + TABLE_MENSAJES_GRUPO + " (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " id_grupo integer NOT NULL,\n"
                + " id_remitente integer,\n"
                + " contenido text NOT NULL,\n"
                + " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,\n"
                + " FOREIGN KEY (id_grupo) REFERENCES " + TABLE_GRUPOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_remitente) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE SET NULL\n"
                + ");";
        String sqlUltimoVisto = "CREATE TABLE IF NOT EXISTS " + TABLE_ULTIMO_VISTO + " (\n"
                + " id_usuario integer NOT NULL,\n"
                + " id_grupo integer NOT NULL,\n"
                + " id_ultimo_mensaje_visto integer NOT NULL DEFAULT 0,\n"
                + " PRIMARY KEY (id_usuario, id_grupo),\n"
                + " FOREIGN KEY (id_usuario) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_grupo) REFERENCES " + TABLE_GRUPOS + "(id) ON DELETE CASCADE\n"
                + ");";


        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueos);
            stmt.execute(sqlRankingGato);
            stmt.execute(sqlHistorialGato);

            stmt.execute(sqlGrupos);
            stmt.execute(sqlMiembros);
            stmt.execute(sqlMensajes);
            stmt.execute(sqlUltimoVisto);


            System.out.println("Base de datos SQLite inicializada. Tablas 'usuarios', 'bloqueos', 'ranking_gato', 'historial_partidas_gato' y tablas de grupos listas.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la base de datos.", e);
            System.err.println("¡ERROR FATAL! No se pudo inicializar la base de datos: " + e.getMessage());
        }
    }

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
        String sqlUsuarios = "SELECT id FROM " + TABLE_USUARIOS;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlUsuarios)) {
            while (rs.next()) {
                unirseAGrupo(rs.getInt("id"), idGrupo);
            }
        }
    }

    public static Integer obtenerIdUsuario(String usuario) throws SQLException {
        String sql = "SELECT id FROM " + TABLE_USUARIOS + " WHERE usuario = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;
        }
    }

    private static Integer obtenerIdGrupo(String nombre) throws SQLException {
        String sql = "SELECT id FROM " + TABLE_GRUPOS + " WHERE nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;
        }
    }

    public static String obtenerCreadorGrupo(String nombreGrupo) throws SQLException {
        String sql = "SELECT u.usuario FROM " + TABLE_GRUPOS + " g JOIN " + TABLE_USUARIOS + " u ON g.id_creador = u.id WHERE g.nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("usuario");
            }
            return null;
        }
    }

    public static boolean crearGrupo(String nombre, String usuarioCreador) {
        String sql = "INSERT INTO " + TABLE_GRUPOS + " (nombre, id_creador) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Integer idCreador = obtenerIdUsuario(usuarioCreador);
            pstmt.setString(1, nombre);
            pstmt.setObject(2, idCreador);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                return false;
            }
            LOGGER.log(Level.SEVERE, "Error al crear el grupo: " + nombre, e);
            return false;
        }
    }

    private static boolean unirseAGrupo(Integer idUsuario, Integer idGrupo) throws SQLException {
        String sql = "INSERT OR IGNORE INTO " + TABLE_MIEMBROS_GRUPO + " (id_usuario, id_grupo) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, idGrupo);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                actualizarUltimoVisto(idUsuario, idGrupo, 0);
                return true;
            }
            return false;
        }
    }

    public static boolean unirseAGrupo(String usuario, String grupo) {
        try {
            Integer idUsuario = obtenerIdUsuario(usuario);
            Integer idGrupo = obtenerIdGrupo(grupo);
            if (idUsuario == null || idGrupo == null) {
                return false;
            }
            return unirseAGrupo(idUsuario, idGrupo);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al unir usuario al grupo.", e);
            return false;
        }
    }

    private static void salirDeGrupo(Integer idUsuario, Integer idGrupo) throws SQLException {
        String sql = "DELETE FROM " + TABLE_MIEMBROS_GRUPO + " WHERE id_usuario = ? AND id_grupo = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, idGrupo);
            pstmt.executeUpdate();
        }
    }

    public static boolean salirDeGrupo(String usuario, String grupo) {
        try {
            Integer idUsuario = obtenerIdUsuario(usuario);
            Integer idGrupo = obtenerIdGrupo(grupo);
            if (idUsuario == null || idGrupo == null) {
                return false;
            }
            salirDeGrupo(idUsuario, idGrupo);
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al salir usuario del grupo.", e);
            return false;
        }
    }

    private static boolean validarCreadorYEliminar(Integer idGrupo, Integer idCreador) throws SQLException {
        String sql = "DELETE FROM " + TABLE_GRUPOS + " WHERE id = ? AND id_creador = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idGrupo);
            pstmt.setInt(2, idCreador);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean eliminarGrupo(String nombre, String usuario) {
        try {
            Integer idGrupo = obtenerIdGrupo(nombre);
            Integer idUsuario = obtenerIdUsuario(usuario);
            if (idGrupo == null || idUsuario == null) {
                return false;
            }
            return validarCreadorYEliminar(idGrupo, idUsuario);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar grupo.", e);
            return false;
        }
    }

    public static boolean esMiembroDeGrupo(String usuario, String grupo) {
        String sql = "SELECT 1 FROM " + TABLE_MIEMBROS_GRUPO + " m JOIN " + TABLE_USUARIOS + " u ON m.id_usuario = u.id JOIN " + TABLE_GRUPOS + " g ON m.id_grupo = g.id WHERE u.usuario = ? AND g.nombre = ?";
        try (Connection conn = getConnection();
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

    public static long guardarMensajeGrupo(String grupo, String remitente, String mensaje) {
        String sql = "INSERT INTO " + TABLE_MENSAJES_GRUPO + " (id_grupo, id_remitente, contenido) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Integer idGrupo = obtenerIdGrupo(grupo);
            Integer idRemitente = obtenerIdUsuario(remitente);

            if (idGrupo == null) {
                return -1L;
            }

            pstmt.setInt(1, idGrupo);
            pstmt.setObject(2, idRemitente);
            pstmt.setString(3, mensaje);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return -1L;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar mensaje de grupo.", e);
            return -1L;
        }
    }

    public static List<String> obtenerMiembrosDeGrupo(String grupo) {
        List<String> miembros = new ArrayList<>();
        String sql = "SELECT u.usuario FROM " + TABLE_MIEMBROS_GRUPO + " m JOIN " + TABLE_USUARIOS + " u ON m.id_usuario = u.id JOIN " + TABLE_GRUPOS + " g ON m.id_grupo = g.id WHERE g.nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, grupo);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                miembros.add(rs.getString("usuario"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener miembros de grupo.", e);
            return miembros;
        }
        return miembros;
    }

    public static List<String> obtenerMensajesNoVistos(String usuario, String grupo) {
        List<String> mensajes = new ArrayList<>();
        try {
            Integer idUsuario = obtenerIdUsuario(usuario);
            Integer idGrupo = obtenerIdGrupo(grupo);
            if (idUsuario == null || idGrupo == null) {
                return mensajes;
            }

            long ultimoVisto = obtenerUltimoMensajeVisto(idUsuario, idGrupo);

            String sql = "SELECT m.id, m.contenido, u.usuario FROM " + TABLE_MENSAJES_GRUPO + " m LEFT JOIN " + TABLE_USUARIOS + " u ON m.id_remitente = u.id WHERE m.id_grupo = ? AND m.id > ? ORDER BY m.id ASC";
            long maxId = ultimoVisto;

            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idGrupo);
                pstmt.setLong(2, ultimoVisto);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    mensajes.add(formatearMensaje(rs));
                    maxId = rs.getLong("id");
                }
            }

            if (maxId > ultimoVisto) {
                actualizarUltimoVisto(idUsuario, idGrupo, maxId);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener mensajes no vistos.", e);
            return mensajes;
        }
        return mensajes;
    }

    private static String formatearMensaje(ResultSet rs) throws SQLException {
        String remitente = rs.getString("usuario");
        String contenido = rs.getString("contenido");
        return (remitente == null ? "[Sistema]" : remitente) + ": " + contenido;
    }

    private static long obtenerUltimoMensajeVisto(Integer idUsuario, Integer idGrupo) throws SQLException {
        String sql = "SELECT id_ultimo_mensaje_visto FROM " + TABLE_ULTIMO_VISTO + " WHERE id_usuario = ? AND id_grupo = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, idGrupo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    private static void actualizarUltimoVisto(Integer idUsuario, Integer idGrupo, long idMensaje) throws SQLException {
        String sql = "INSERT OR REPLACE INTO " + TABLE_ULTIMO_VISTO + " (id_usuario, id_grupo, id_ultimo_mensaje_visto) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, idGrupo);
            pstmt.setLong(3, idMensaje);
            pstmt.executeUpdate();
        }
    }
    private static void actualizarRanking(Integer idUsuario, int puntos, int victorias, int derrotas, int empates) throws SQLException {
        String sql = "INSERT INTO " + TABLE_RANKING_GATO + " (id_usuario, puntos, victorias, derrotas, empates) VALUES (?, ?, ?, ?, ?)\n"
                + "ON CONFLICT(id_usuario) DO UPDATE SET\n"
                + " puntos = puntos + excluded.puntos,\n"
                + " victorias = victorias + excluded.victorias,\n"
                + " derrotas = derrotas + excluded.derrotas,\n"
                + " empates = empates + excluded.empates;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idUsuario);
            pstmt.setInt(2, puntos);
            pstmt.setInt(3, victorias);
            pstmt.setInt(4, derrotas);
            pstmt.setInt(5, empates);

            pstmt.executeUpdate();
        }
    }
    public static void registrarResultadoGato(String usuario1, String usuario2, String ganador) {
        Integer id1 = null;
        Integer id2 = null;
        Integer idGanador = null;

        try {
            id1 = obtenerIdUsuario(usuario1);
            id2 = obtenerIdUsuario(usuario2);
            if (ganador != null) {
                idGanador = obtenerIdUsuario(ganador);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener IDs de usuarios para registro de partida.", e);
            return;
        }

        if (id1 == null || id2 == null) {
            LOGGER.log(Level.WARNING, "Uno o ambos usuarios de la partida no existen en la BD (o no están autenticados).");
            return;
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            if (ganador != null) {
                actualizarRanking(idGanador, 2, 1, 0, 0);
                Integer idPerdedor = (idGanador.equals(id1)) ? id2 : id1;
                actualizarRanking(idPerdedor, 0, 0, 1, 0);
            } else {
                actualizarRanking(id1, 1, 0, 0, 1);
                actualizarRanking(id2, 1, 0, 0, 1);
            }

            String sqlHistorial = "INSERT INTO " + TABLE_HISTORIAL_PARTIDAS_GATO + " (id_jugador1, id_jugador2, id_ganador) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlHistorial)) {
                pstmt.setInt(1, id1);
                pstmt.setInt(2, id2);
                if (idGanador != null) {
                    pstmt.setInt(3, idGanador);
                } else {
                    pstmt.setNull(3, java.sql.Types.INTEGER);
                }
                pstmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar la partida de Gato.", e);
        }
    }
    public static List<Map<String, Object>> obtenerRankingGeneral() throws SQLException {
        List<Map<String, Object>> ranking = new ArrayList<>();
        String sql = "SELECT u.usuario, r.puntos, r.victorias, r.derrotas, r.empates "
                + "FROM " + TABLE_RANKING_GATO + " r "
                + "JOIN " + TABLE_USUARIOS + " u ON r.id_usuario = u.id "
                + "ORDER BY r.puntos DESC, r.victorias DESC, r.empates DESC, u.usuario ASC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("usuario", rs.getString("usuario"));
                fila.put("puntos", rs.getInt("puntos"));
                fila.put("victorias", rs.getInt("victorias"));
                fila.put("derrotas", rs.getInt("derrotas"));
                fila.put("empates", rs.getInt("empates"));
                ranking.add(fila);
            }
        }
        return ranking;
    }
    public static Map<String, Object> obtenerEstadisticasVs(String usuario1, String usuario2) throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        Integer id1 = obtenerIdUsuario(usuario1);
        Integer id2 = obtenerIdUsuario(usuario2);
        if (id1 == null || id2 == null) {
            stats.put("error", "Uno o ambos usuarios no existen.");
            return stats;
        }
        String sqlPartidas = "SELECT id_jugador1, id_jugador2, id_ganador FROM " + TABLE_HISTORIAL_PARTIDAS_GATO +
                " WHERE (id_jugador1 = ? AND id_jugador2 = ?) OR (id_jugador1 = ? AND id_jugador2 = ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmtPartidas = conn.prepareStatement(sqlPartidas)) {
            pstmtPartidas.setInt(1, id1);
            pstmtPartidas.setInt(2, id2);
            pstmtPartidas.setInt(3, id2);
            pstmtPartidas.setInt(4, id1);

            int total = 0;
            int victorias1 = 0;
            int victorias2 = 0;

            try (ResultSet rs = pstmtPartidas.executeQuery()) {
                while (rs.next()) {
                    total++;
                    Integer idGanador = (Integer) rs.getObject("id_ganador");
                    if (idGanador != null && idGanador.equals(id1)) {
                        victorias1++;
                    } else if (idGanador != null && idGanador.equals(id2)) {
                        victorias2++;
                    }
                }
            }
            int empates = total - victorias1 - victorias2;

            stats.put("usuario1", usuario1);
            stats.put("usuario2", usuario2);
            stats.put("total_partidas", total);
            stats.put("victorias1", victorias1);
            stats.put("victorias2", victorias2);
            stats.put("empates", empates);

            double pct1 = total > 0 ? (double) victorias1 / total * 100 : 0.0;
            double pct2 = total > 0 ? (double) victorias2 / total * 100 : 0.0;

            stats.put("porcentaje_victorias1", String.format("%.2f%%", pct1));
            stats.put("porcentaje_victorias2", String.format("%.2f%%", pct2));
        }
        return stats;
    }
    public static boolean registrarUsuario(String usuario, String contrasena) {
        String sql = "INSERT INTO " + TABLE_USUARIOS + "(usuario, contrasena) VALUES(?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.setString(2, contrasena);

            pstmt.executeUpdate();

            unirseAGrupo(usuario, "Todos");

            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                return false;
            }
            LOGGER.log(Level.SEVERE, "Error al registrar el usuario: " + usuario, e);
            return false;
        }
    }

    public static boolean verificarCredenciales(String usuario, String contrasena) {
        String sql = "SELECT contrasena FROM " + TABLE_USUARIOS + " WHERE usuario = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String contrasenaAlmacenada = rs.getString("contrasena");
                return contrasena.equals(contrasenaAlmacenada);
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar credenciales para: " + usuario, e);
            return false;
        }
    }
}