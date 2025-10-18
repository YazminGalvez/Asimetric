package servidormulti;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Usuarios {
    private static final String DB_FILE = "usuarios.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static final String TABLE_NAME = "usuarios";
    private static final String TABLE_BLOQUEOS = "bloqueos";

    private static final Logger LOGGER = Logger.getLogger(Usuarios.class.getName());
    static {
        initializeDatabase();
    }
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void initializeDatabase() {
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                + " id integer PRIMARY KEY,\n"
                + " usuario text NOT NULL UNIQUE,\n"
                + " contrasena text NOT NULL\n"
                + ");";
        String sqlBloqueos = "CREATE TABLE IF NOT EXISTS " + TABLE_BLOQUEOS + " (\n"
                + " id_bloqueador integer NOT NULL,\n"
                + " id_bloqueado integer NOT NULL,\n"
                + " PRIMARY KEY (id_bloqueador, id_bloqueado),\n"
                + " FOREIGN KEY (id_bloqueador) REFERENCES " + TABLE_NAME + "(id) ON DELETE CASCADE,\n"
                + " FOREIGN KEY (id_bloqueado) REFERENCES " + TABLE_NAME + "(id) ON DELETE CASCADE\n"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueos);
            System.out.println("Base de datos SQLite inicializada. Tablas 'usuarios' y 'bloqueos' listas.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la base de datos.", e);
            System.err.println("¡ERROR FATAL! No se pudo inicializar la base de datos: " + e.getMessage());
        }
    }

    public static boolean registrarUsuario(String usuario, String contrasena) {
        String sql = "INSERT INTO " + TABLE_NAME + "(usuario, contrasena) VALUES(?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.setString(2, contrasena);

            pstmt.executeUpdate();
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
        String sql = "SELECT contrasena FROM " + TABLE_NAME + " WHERE usuario = ?";

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

    private static Integer obtenerIdUsuario(String usuario) throws SQLException {
        String sql = "SELECT id FROM " + TABLE_NAME + " WHERE usuario = ?";
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
    public static boolean bloquearUsuario(String bloqueador, String bloqueado) {
        if (bloqueador.equalsIgnoreCase(bloqueado)) {
            return false;
        }
        try (Connection conn = getConnection()) {
            Integer idBloqueador = obtenerIdUsuario(bloqueador);
            Integer idBloqueado = obtenerIdUsuario(bloqueado);

            if (idBloqueador == null || idBloqueado == null) {
                return false;
            }
            String sql = "INSERT INTO " + TABLE_BLOQUEOS + "(id_bloqueador, id_bloqueado) VALUES(?, ?)";
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
        try (Connection conn = getConnection()) {
            Integer idBloqueador = obtenerIdUsuario(bloqueador);
            Integer idBloqueado = obtenerIdUsuario(bloqueado);

            if (idBloqueador == null || idBloqueado == null) {
                return false;
            }

            String sql = "DELETE FROM " + TABLE_BLOQUEOS + " WHERE id_bloqueador = ? AND id_bloqueado = ?";
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
        try (Connection conn = getConnection()) {
            Integer idEmisor = obtenerIdUsuario(emisor);
            Integer idReceptor = obtenerIdUsuario(receptor);

            if (idEmisor == null || idReceptor == null) {
                return false;
            }

            String sql = "SELECT COUNT(*) FROM " + TABLE_BLOQUEOS
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