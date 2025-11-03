package basededatos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseDatos {
    private static final String DB_FILE = "usuarios.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static final Logger LOGGER = Logger.getLogger(BaseDatos.class.getName());

    public static final String TABLE_USUARIOS = "usuarios";
    public static final String TABLE_BLOQUEOS = "bloqueos";
    public static final String TABLE_RANKING_GATO = "ranking_gato";
    public static final String TABLE_HISTORIAL_PARTIDAS_GATO = "historial_partidas_gato";
    public static final String TABLE_GRUPOS = "grupos";
    public static final String TABLE_MIEMBROS_GRUPO = "miembros_grupo";
    public static final String TABLE_MENSAJES_GRUPO = "mensajes_grupo";
    public static final String TABLE_ULTIMO_VISTO = "ultimo_visto";

    static {
        initializeDatabase();
        BdGrupos.inicializarGrupos();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            createAllTables(stmt);
            System.out.println("Base de datos SQLite inicializada. Tablas listas.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la base de datos.", e);
            System.err.println("¡ERROR FATAL! No se pudo inicializar la base de datos: " + e.getMessage());
        }
    }

    private static void createAllTables(Statement stmt) throws SQLException {
        stmt.execute(getSqlUsuarios());
        stmt.execute(getSqlBloqueos());
        stmt.execute(getSqlRankingGato());
        stmt.execute(getSqlHistorialGato());
        stmt.execute(getSqlGrupos());
        stmt.execute(getSqlMiembros());
        stmt.execute(getSqlMensajes());
        stmt.execute(getSqlUltimoVisto());
    }

    private static String getSqlUsuarios() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_USUARIOS + " (\n"
                + " id integer PRIMARY KEY,\n"
                + " usuario text NOT NULL UNIQUE,\n"
                + " contrasena text NOT NULL\n"
                + ");";
    }

    private static String getSqlBloqueos() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_BLOQUEOS + " (\n"
                + " id_bloqueador integer NOT NULL,\n"
                + " id_bloqueado integer NOT NULL,\n"
                + " PRIMARY KEY (id_bloqueador, id_bloqueado),\n"
                + " FOREIGN KEY (id_bloqueador) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_bloqueado) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE\n"
                + ");";
    }

    private static String getSqlRankingGato() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_RANKING_GATO + " (\n"
                + " id_usuario integer PRIMARY KEY,\n"
                + " puntos integer NOT NULL DEFAULT 0,\n"
                + " victorias integer NOT NULL DEFAULT 0,\n"
                + " derrotas integer NOT NULL DEFAULT 0,\n"
                + " empates integer NOT NULL DEFAULT 0,\n"
                + " FOREIGN KEY (id_usuario) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE\n"
                + ");";
    }

    private static String getSqlHistorialGato() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_HISTORIAL_PARTIDAS_GATO + " (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " id_jugador1 integer NOT NULL,\n"
                + " id_jugador2 integer NOT NULL,\n"
                + " id_ganador integer,\n"
                + " FOREIGN KEY (id_jugador1) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_jugador2) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE\n"
                + ");";
    }

    private static String getSqlGrupos() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_GRUPOS + " (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " nombre text NOT NULL UNIQUE,\n"
                + " id_creador integer,\n"
                + " FOREIGN KEY (id_creador) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE SET NULL\n"
                + ");";
    }

    private static String getSqlMiembros() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_MIEMBROS_GRUPO + " (\n"
                + " id_usuario integer NOT NULL,\n"
                + " id_grupo integer NOT NULL,\n"
                + " PRIMARY KEY (id_usuario, id_grupo),\n"
                + " FOREIGN KEY (id_usuario) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_grupo) REFERENCES " + TABLE_GRUPOS + "(id) ON DELETE CASCADE\n"
                + ");";
    }

    private static String getSqlMensajes() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_MENSAJES_GRUPO + " (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " id_grupo integer NOT NULL,\n"
                + " id_remitente integer,\n"
                + " contenido text NOT NULL,\n"
                + " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,\n"
                + " FOREIGN KEY (id_grupo) REFERENCES " + TABLE_GRUPOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_remitente) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE SET NULL\n"
                + ");";
    }

    private static String getSqlUltimoVisto() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_ULTIMO_VISTO + " (\n"
                + " id_usuario integer NOT NULL,\n"
                + " id_grupo integer NOT NULL,\n"
                + " id_ultimo_mensaje_visto integer NOT NULL DEFAULT 0,\n"
                + " PRIMARY KEY (id_usuario, id_grupo),\n"
                + " FOREIGN KEY (id_usuario) REFERENCES " + TABLE_USUARIOS + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_grupo) REFERENCES " + TABLE_GRUPOS + "(id) ON DELETE CASCADE\n"
                + ");";
    }
}