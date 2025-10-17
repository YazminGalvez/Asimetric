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

    private static final Logger LOGGER = Logger.getLogger(Usuarios.class.getName());
    static {
        initializeDatabase();
    }
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                + " id integer PRIMARY KEY,\n"
                + " usuario text NOT NULL UNIQUE,\n"
                + " contrasena text NOT NULL\n"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("Base de datos SQLite inicializada. Tabla 'usuarios' lista.");
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
}