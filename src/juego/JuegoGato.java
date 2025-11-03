package juego;
import servidormulti.UnCliente;
import java.io.IOException;
import java.util.Random;
import basededatos.BdJuegos;

public class JuegoGato {
    public enum EstadoCasilla { VACIO, X, O }
    public enum EstadoJuego { ACTIVO, GANA_X, GANA_O, EMPATE, ABANDONO }

    private UnCliente jugadorX;
    private UnCliente jugadorO;
    private EstadoCasilla[][] tablero;
    private UnCliente turnoActual;
    private EstadoJuego estado;

    public JuegoGato(UnCliente c1, UnCliente c2) throws IOException {
        inicializarTablero();
        this.estado = EstadoJuego.ACTIVO;
        asignarJugadores(c1, c2);
        establecerTurnoInicial();
        notificarInicio();
    }

    private void inicializarTablero() {
        this.tablero = new EstadoCasilla[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tablero[i][j] = EstadoCasilla.VACIO;
            }
        }
    }

    private void asignarJugadores(UnCliente c1, UnCliente c2) {
        Random rand = new Random();
        if (rand.nextBoolean()) {
            this.jugadorX = c1;
            this.jugadorO = c2;
        } else {
            this.jugadorX = c2;
            this.jugadorO = c1;
        }
    }

    private void establecerTurnoInicial() {
        Random rand = new Random();
        this.turnoActual = rand.nextBoolean() ? jugadorX : jugadorO;
    }

    public UnCliente getJugadorX() { return jugadorX; }
    public UnCliente getJugadorO() { return jugadorO; }
    public UnCliente getTurnoActual() { return turnoActual; }
    public EstadoJuego getEstado() { return estado; }

    public UnCliente getContrincante(UnCliente cliente) {
        if (cliente == jugadorX) return jugadorO;
        if (cliente == jugadorO) return jugadorX;
        return null;
    }

    private String getSimbolo(UnCliente cliente) {
        if (cliente == jugadorX) return "X";
        if (cliente == jugadorO) return "O";
        return "?";
    }

    private String dibujarTablero() {
        StringBuilder sb = new StringBuilder("\n  1 2 3\n");
        for (int i = 0; i < 3; i++) {
            dibujarFila(i, sb);
            dibujarSeparador(i, sb);
        }
        return sb.toString();
    }

    private void dibujarFila(int i, StringBuilder sb) {
        sb.append((i + 1)).append(" ");
        for (int j = 0; j < 3; j++) {
            dibujarCasilla(i, j, sb);
            if (j < 2) sb.append("|");
        }
        sb.append("\n");
    }

    private void dibujarCasilla(int i, int j, StringBuilder sb) {
        switch (tablero[i][j]) {
            case X: sb.append("X"); break;
            case O: sb.append("O"); break;
            case VACIO: sb.append("-"); break;
        }
    }

    private void dibujarSeparador(int i, StringBuilder sb) {
        if (i < 2) {
            sb.append("  -----\n");
        }
    }

    private boolean verificarGanador(EstadoCasilla simbolo) {
        return verificarFilasYColumnas(simbolo) || verificarDiagonales(simbolo);
    }

    private boolean verificarFilasYColumnas(EstadoCasilla simbolo) {
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] == simbolo && tablero[i][1] == simbolo && tablero[i][2] == simbolo) return true;
            if (tablero[0][i] == simbolo && tablero[1][i] == simbolo && tablero[2][i] == simbolo) return true;
        }
        return false;
    }

    private boolean verificarDiagonales(EstadoCasilla simbolo) {
        if (tablero[0][0] == simbolo && tablero[1][1] == simbolo && tablero[2][2] == simbolo) return true;
        return tablero[0][2] == simbolo && tablero[1][1] == simbolo && tablero[2][0] == simbolo;
    }

    private boolean verificarEmpate() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tablero[i][j] == EstadoCasilla.VACIO) {
                    return false;
                }
            }
        }
        return true;
    }

    public void notificarInicio() throws IOException {
        String mensajeGeneral = crearMensajeInicioGeneral();
        String mensajeTurno = crearMensajeTurno();
        enviarMensajesPersonalizados(mensajeGeneral, mensajeTurno);
    }

    private String crearMensajeInicioGeneral() {
        String infoX = jugadorX.getNombreCliente() + " (X)";
        String infoO = jugadorO.getNombreCliente() + " (O)";
        return "Sistema Gato: ¡Juego iniciado! " + infoX + " vs " + infoO + ".\n";
    }

    private String crearMensajeTurno() {
        return "\nTurno de " + turnoActual.getNombreCliente() + " (" + getSimbolo(turnoActual) + "). Usa mover <fila> <columna> (ej: mover 1 3)";
    }

    private void enviarMensajesPersonalizados(String mensajeGeneral, String mensajeTurno) throws IOException {
        jugadorX.enviarMensaje(mensajeGeneral + "Tú eres X." + dibujarTablero() + mensajeTurno);
        jugadorO.enviarMensaje(mensajeGeneral + "Tú eres O." + dibujarTablero() + mensajeTurno);
    }

    private void notificarMovimiento(String mensaje) throws IOException {
        jugadorX.enviarMensaje(mensaje + dibujarTablero());
        jugadorO.enviarMensaje(mensaje + dibujarTablero());
    }

    private void notificarResultado(String mensaje) throws IOException {
        jugadorX.enviarMensaje(mensaje + dibujarTablero());
        jugadorO.enviarMensaje(mensaje + dibujarTablero());
    }

    private void registrarResultadoFinal() {
        if (!jugadorX.isRegistrado() || !jugadorO.isRegistrado()) return;

        String ganador = determinarGanadorRegistrado();

        BdJuegos.registrarResultadoGato(
                jugadorX.getNombreCliente(),
                jugadorO.getNombreCliente(),
                ganador
        );
    }

    private String determinarGanadorRegistrado() {
        if (estado == EstadoJuego.GANA_X) {
            return jugadorX.getNombreCliente();
        } else if (estado == EstadoJuego.GANA_O) {
            return jugadorO.getNombreCliente();
        }
        return null;
    }

    public boolean realizarMovimiento(UnCliente cliente, int fila, int columna) throws IOException {
        if (!validarMovimiento(cliente, fila, columna)) return false;

        int r = fila - 1;
        int c = columna - 1;
        EstadoCasilla simbolo = (cliente == jugadorX) ? EstadoCasilla.X : EstadoCasilla.O;
        tablero[r][c] = simbolo;
        String notificacion = "Sistema Gato: " + cliente.getNombreCliente() + " ha jugado en (" + fila + "," + columna + ").";

        if (verificarGanador(simbolo) || verificarEmpate()) {
            manejarFinDeJuego(cliente, simbolo, notificacion);
            return true;
        }

        procesarFinDeTurno(cliente, notificacion);
        return false;
    }

    private boolean validarMovimiento(UnCliente cliente, int fila, int columna) throws IOException {
        if (!validarTurno(cliente)) return false;
        if (!validarCoordenadas(cliente, fila, columna)) return false;
        return validarCasillaOcupada(cliente, fila, columna);
    }

    private boolean validarTurno(UnCliente cliente) throws IOException {
        if (cliente != turnoActual) {
            cliente.enviarMensaje("Sistema Gato: No es tu turno.");
            return false;
        }
        return true;
    }

    private boolean validarCoordenadas(UnCliente cliente, int fila, int columna) throws IOException {
        if (fila < 1 || fila > 3 || columna < 1 || columna > 3) {
            cliente.enviarMensaje("Sistema Gato: Movimiento inválido. Usa mover <fila> <columna> (fila y columna deben ser 1, 2 o 3).");
            return false;
        }
        return true;
    }

    private boolean validarCasillaOcupada(UnCliente cliente, int fila, int columna) throws IOException {
        int r = fila - 1;
        int c = columna - 1;
        if (tablero[r][c] != EstadoCasilla.VACIO) {
            cliente.enviarMensaje("Sistema Gato: La posicion (" + fila + "," + columna + ") ya esta ocupada.");
            return false;
        }
        return true;
    }

    private void manejarFinDeJuego(UnCliente clienteQueMovio, EstadoCasilla simbolo, String notificacionMovimiento) throws IOException {
        String mensajeResultado = determinarResultadoYMensaje(clienteQueMovio, simbolo, notificacionMovimiento);
        notificarResultado(mensajeResultado);

        UnCliente oponente = getContrincante(clienteQueMovio);
        if (oponente != null) {
            notificarRevancha(clienteQueMovio, oponente);
        }


        if (jugadorX != null) jugadorX.setPropuestaJuegoPendiente(true);
        if (jugadorO != null) jugadorO.setPropuestaJuegoPendiente(true);

        registrarResultadoFinal();
    }

    private String determinarResultadoYMensaje(UnCliente clienteQueMovio, EstadoCasilla simbolo, String notificacionMovimiento) {
        if (verificarGanador(simbolo)) {
            estado = (simbolo == EstadoCasilla.X) ? EstadoJuego.GANA_X : EstadoJuego.GANA_O;
            return notificacionMovimiento + "\nSistema Gato: ¡" + clienteQueMovio.getNombreCliente() + " (" + getSimbolo(clienteQueMovio) + ") ha ganado! La partida ha finalizado.";
        }

        estado = EstadoJuego.EMPATE;
        return notificacionMovimiento + "\nSistema Gato: ¡Es un empate! La partida ha finalizado.";
    }

    private void notificarRevancha(UnCliente cliente1, UnCliente cliente2) throws IOException {
        String m1 = crearMensajeRevancha(cliente2.getNombreCliente());
        String m2 = crearMensajeRevancha(cliente1.getNombreCliente());
        cliente1.enviarMensaje(m1);
        cliente2.enviarMensaje(m2);
    }

    private String crearMensajeRevancha(String oponenteNombre) {
        return "\nSistema Gato: ¿Quieres jugar de nuevo contra " + oponenteNombre + "? Usa /gato " + oponenteNombre + " o espera su propuesta.";
    }

    private void procesarFinDeTurno(UnCliente clienteQueMovio, String notificacionMovimiento) throws IOException {
        turnoActual = getContrincante(clienteQueMovio);
        String notificacion = notificacionMovimiento + "\nTurno de " + turnoActual.getNombreCliente() + " (" + getSimbolo(turnoActual) + ").";
        notificarMovimiento(notificacion);
    }

    public void finalizarPorAbandono(UnCliente desconectado) throws IOException {
        if (estado != EstadoJuego.ACTIVO) return;

        estado = EstadoJuego.ABANDONO;
        UnCliente oponente = getContrincante(desconectado);

        if (oponente != null) {
            oponente.enviarMensaje("Sistema Gato: ¡" + desconectado.getNombreCliente() + " se ha desconectado! Has ganado automaticamente el juego. La partida ha finalizado.");
            registrarAbandono(oponente, desconectado);
            oponente.setPropuestaJuegoPendiente(false);
        }
    }

    private void registrarAbandono(UnCliente ganador, UnCliente perdedor) {
        if (ganador.isRegistrado() && perdedor.isRegistrado()) {
            BdJuegos.registrarResultadoGato(
                    ganador.getNombreCliente(),
                    perdedor.getNombreCliente(),
                    ganador.getNombreCliente()
            );
        }
    }
}