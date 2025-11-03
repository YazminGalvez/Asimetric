package servidormulti;
import basededatos.BdJuegos;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServicioRanking {

    private static final Logger LOGGER = Logger.getLogger(ServicioRanking.class.getName());

    public void mostrarRankingGeneral(UnCliente cliente) throws IOException {
        try {
            List<Map<String, Object>> ranking = BdJuegos.obtenerRankingGeneral();
            procesarMostrarRanking(cliente, ranking);
        } catch (SQLException e) {
            cliente.enviarMensaje("Sistema Ranking: Error al consultar el ranking. Intenta de nuevo mas tarde.");
            LOGGER.log(Level.SEVERE, "Error al obtener ranking", e);
        }
    }

    private void procesarMostrarRanking(UnCliente cliente, List<Map<String, Object>> ranking) throws IOException {
        if (ranking.isEmpty()) {
            cliente.enviarMensaje("Sistema Ranking: No hay datos de ranking disponibles.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        construirEncabezadoRanking(sb);
        construirFilasRanking(sb, ranking);
        construirPieRanking(sb);

        cliente.enviarMensaje(sb.toString());
    }

    private void construirEncabezadoRanking(StringBuilder sb) {
        sb.append("\n===========================================\n");
        sb.append("         RANKING GENERAL GATO \n");
        sb.append("============================================\n");
        sb.append(String.format("%-4s | %-15s | %-6s | %-3s | %-3s | %-3s\n", "Pos.", "Usuario", "Puntos", "V", "D", "E"));
        sb.append("--------------------------------------------\n");
    }

    private void construirFilasRanking(StringBuilder sb, List<Map<String, Object>> ranking) {
        int pos = 1;
        for (Map<String, Object> fila : ranking) {
            sb.append(String.format("%-4d | %-15s | %-6d | %-3d | %-3d | %-3d\n",
                    pos++,
                    fila.get("usuario"),
                    fila.get("puntos"),
                    fila.get("victorias"),
                    fila.get("derrotas"),
                    fila.get("empates")));
        }
    }

    private void construirPieRanking(StringBuilder sb) {
        sb.append("============================================\n");
        sb.append("Sistema Ranking: (V: Victorias (2 Pts), D: Derrotas (0 Pts), E: Empates (1 Pt))\n");
    }

    public void mostrarEstadisticasVsArbitrarias(UnCliente cliente, String usuario1, String usuario2) throws IOException {
        if (!validarFormatoVs(cliente, usuario1, usuario2)) return;

        try {
            Map<String, Object> stats = BdJuegos.obtenerEstadisticasVs(usuario1, usuario2);
            procesarMostrarEstadisticas(cliente, stats, usuario1, usuario2);
        } catch (SQLException e) {
            cliente.enviarMensaje("Sistema VS: Error al consultar estadísticas Head-to-Head. Intenta de nuevo más tarde.");
            LOGGER.log(Level.SEVERE, "Error al obtener estadísticas VS", e);
        }
    }

    private boolean validarFormatoVs(UnCliente cliente, String u1, String u2) throws IOException {
        if (u1.length() < 4 || !u1.matches("^[a-zA-Z0-9]+$") ||
                u2.length() < 4 || !u2.matches("^[a-zA-Z0-9]+$")) {
            cliente.enviarMensaje("Sistema VS: Los nombres de usuario no tienen un formato valido (mínimo 4 caracteres alfanuméricos).");
            return false;
        }
        return true;
    }

    private void procesarMostrarEstadisticas(UnCliente cliente, Map<String, Object> stats, String u1, String u2) throws IOException {
        if (stats.containsKey("error")) {
            cliente.enviarMensaje("Sistema VS: Error. " + stats.get("error"));
            return;
        }

        int total = (int) stats.get("total_partidas");
        if (total == 0) {
            cliente.enviarMensaje(String.format("Sistema VS: %s y %s no han jugado partidas entre ellos.", u1, u2));
            return;
        }

        StringBuilder sb = new StringBuilder();
        construirEstadisticas(sb, stats, u1, u2, total);
        cliente.enviarMensaje(sb.toString());
    }

    private void construirEstadisticas(StringBuilder sb, Map<String, Object> stats, String u1, String u2, int total) {
        int v1 = (int) stats.get("victorias1");
        int v2 = (int) stats.get("victorias2");
        int e = (int) stats.get("empates");
        String pct1 = (String) stats.get("porcentaje_victorias1");
        String pct2 = (String) stats.get("porcentaje_victorias2");

        sb.append("\n======================================================\n");
        sb.append(String.format("     ESTADISTICAS : %s vs %s \n", u1, u2));
        sb.append("======================================================\n");
        sb.append(String.format("Partidas jugadas entre ellos: %d\n", total));
        sb.append("------------------------------------------------------\n");
        sb.append(String.format("» %-15s: Victorias: %-3d (%s)\n", u1, v1, pct1));
        sb.append(String.format("» %-15s: Victorias: %-3d (%s)\n", u2, v2, pct2));
        sb.append(String.format("» Empates: %d\n", e));
        sb.append("======================================================\n");
    }
}