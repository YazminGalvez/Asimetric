package servidormulti;

import basededatos.BaseDatos;
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
            List<Map<String, Object>> ranking = BaseDatos.obtenerRankingGeneral();

            if (ranking.isEmpty()) {
                cliente.enviarMensaje("Sistema Ranking: No hay datos de ranking disponibles.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\n===========================================\n");
            sb.append("         RANKING GENERAL GATO \n");
            sb.append("============================================\n");
            sb.append(String.format("%-4s | %-15s | %-6s | %-3s | %-3s | %-3s\n", "Pos.", "Usuario", "Puntos", "V", "D", "E"));
            sb.append("--------------------------------------------\n");

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
            sb.append("============================================\n");
            sb.append("Sistema Ranking: (V: Victorias (2 Pts), D: Derrotas (0 Pts), E: Empates (1 Pt))\n");

            cliente.enviarMensaje(sb.toString());

        } catch (SQLException e) {
            cliente.enviarMensaje("Sistema Ranking: Error al consultar el ranking. Intenta de nuevo mas tarde.");
            LOGGER.log(Level.SEVERE, "Error al obtener ranking", e);
        }
    }

    public void mostrarEstadisticasVs(UnCliente cliente, String objetivo) throws IOException {
        String remitente = cliente.getNombreCliente();

        if (objetivo.length() < 4 || !objetivo.matches("^[a-zA-Z0-9]+$")) {
            cliente.enviarMensaje("Sistema VS: El nombre de usuario objetivo no tiene un formato valido.");
            return;
        }

        if (objetivo.equals(remitente)) {
            cliente.enviarMensaje("Sistema VS: No puedes ver estadisticas Head-to-Head contigo mismo.");
            return;
        }

        try {
            Map<String, Object> stats = BaseDatos.obtenerEstadisticasVs(remitente, objetivo);

            if (stats.containsKey("error")) {
                cliente.enviarMensaje("Sistema VS: Error. " + stats.get("error"));
                return;
            }

            int total = (int) stats.get("total_partidas");
            int v1 = (int) stats.get("victorias1");
            int v2 = (int) stats.get("victorias2");
            int e = (int) stats.get("empates");
            String pct1 = (String) stats.get("porcentaje_victorias1");
            String pct2 = (String) stats.get("porcentaje_victorias2");

            StringBuilder sb = new StringBuilder();
            sb.append("\n======================================================\n");
            sb.append(String.format("     ESTADÍSTICAS HEAD-TO-HEAD: %s vs %s \n", remitente, objetivo));
            sb.append("======================================================\n");
            sb.append(String.format("Partidas jugadas entre ellos: %d\n", total));
            sb.append("------------------------------------------------------\n");
            sb.append(String.format("» %-15s: Victorias: %-3d (%s)\n", remitente, v1, pct1));
            sb.append(String.format("» %-15s: Victorias: %-3d (%s)\n", objetivo, v2, pct2));
            sb.append(String.format("» Empates: %d\n", e));
            sb.append("======================================================\n");

            cliente.enviarMensaje(sb.toString());

        } catch (SQLException e) {
            cliente.enviarMensaje("Sistema VS: Error al consultar estadísticas Head-to-Head. Intenta de nuevo más tarde.");
            LOGGER.log(Level.SEVERE, "Error al obtener estadísticas VS", e);
        }
    }
}