package servidormulti;

import juego.JuegoGato;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Map.Entry;

public class ControladorJuego {
    private final Map<String, String> propuestasPendientes;
    private final Map<String, JuegoGato> juegosActivosPorPar;
    private final Map<String, List<JuegoGato>> jugadorJuegosMap;

    public ControladorJuego() {
        this.propuestasPendientes = Collections.synchronizedMap(new HashMap<>());
        this.juegosActivosPorPar = Collections.synchronizedMap(new HashMap<>());
        this.jugadorJuegosMap = Collections.synchronizedMap(new HashMap<>());
    }

    private String getCanonicalPair(String name1, String name2) {
        if (name1.compareTo(name2) < 0) {
            return name1 + ":" + name2;
        } else {
            return name2 + ":" + name1;
        }
    }

    public void manejarComando(String mensaje, UnCliente remitente) throws IOException {
        if (!remitente.isRegistrado()) {
            remitente.enviarMensaje("Sistema Gato: Debes estar registrado para jugar al Gato.");
            return;
        }

        String[] p = mensaje.split(" ", 3);
        String cmd = p[0];
        String arg1 = p.length > 1 ? p[1] : "";
        String arg2 = p.length > 2 ? p[2] : "";

        procesarComandoJuego(cmd, arg1, arg2, remitente);
    }

    private void procesarComandoJuego(String comando, String arg1, String arg2, UnCliente remitente) throws IOException {
        switch (comando) {
            case "/gato":
                proponerJuego(remitente, arg1);
                break;
            case "acepto":
                aceptarPropuesta(remitente, arg1);
                break;
            case "rechazo":
                rechazarPropuesta(remitente, arg1);
                break;
            case "mover":
                manejarMovimiento(remitente, arg1, arg2);
                break;
            default:
                remitente.enviarMensaje("Sistema Gato: Comando de juego desconocido.");
                break;
        }
    }

    private void proponerJuego(UnCliente proponente, String nombreDestino) throws IOException {
        String proponenteNombre = proponente.getNombreCliente();

        if (validarPropuestaInicial(proponente, proponenteNombre, nombreDestino)) return;

        UnCliente clienteDestino = ServidorMulti.getCliente(nombreDestino);
        if (validarDestino(proponente, clienteDestino, nombreDestino)) return;

        if (validarBloqueosYJuegoActivo(proponente, proponenteNombre, nombreDestino)) return;

        propuestasPendientes.put(proponenteNombre, nombreDestino);

        proponente.enviarMensaje("Sistema Gato: Propuesta enviada a " + nombreDestino + ". Esperando respuesta...");
        clienteDestino.enviarMensaje("Sistema Gato: ¡" + proponenteNombre + " te ha propuesto jugar al Gato! Usa acepto " + proponenteNombre + " o rechazo " + proponenteNombre + ".");
    }

    private boolean validarPropuestaInicial(UnCliente proponente, String pName, String dName) throws IOException {
        if (dName.isEmpty()) {
            proponente.enviarMensaje("Sistema Gato: Uso incorrecto. Usa /gato <usuario>");
            return true;
        }
        if (pName.equals(dName)) {
            proponente.enviarMensaje("Sistema Gato: No puedes jugar al Gato contigo mismo.");
            return true;
        }
        return false;
    }

    private boolean validarDestino(UnCliente proponente, UnCliente destino, String dName) throws IOException {
        if (destino == null || !destino.isRegistrado()) {
            proponente.enviarMensaje("Sistema Gato: El usuario '" + dName + "' no esta conectado o no esta autenticado.");
            return true;
        }
        return false;
    }

    private boolean validarBloqueosYJuegoActivo(UnCliente proponente, String pName, String dName) throws IOException {
        if (chequearBloqueos(proponente, pName, dName)) return true;
        if (chequearJuegoActivo(proponente, pName, dName)) return true;
        if (chequearPropuestaPendiente(proponente, pName, dName)) return true;
        return false;
    }

    private boolean chequearBloqueos(UnCliente proponente, String pName, String dName) throws IOException {
        boolean bPorDestino = Bloqueos.estaBloqueadoPor(dName, pName);
        boolean bPorRemitente = Bloqueos.estaBloqueadoPor(pName, dName);

        if (bPorDestino || bPorRemitente) {
            String razon = bPorRemitente ? "Tienes bloqueado al usuario." : "El usuario te tiene bloqueado.";
            proponente.enviarMensaje("Sistema Gato: Error al proponer juego a '" + dName + "'. " + razon + " No puedes jugar con alguien con quien tienes un bloqueo activo.");
            return true;
        }
        return false;
    }

    private boolean chequearJuegoActivo(UnCliente proponente, String pName, String dName) throws IOException {
        String parCanonica = getCanonicalPair(pName, dName);
        if (juegosActivosPorPar.containsKey(parCanonica)) {
            proponente.enviarMensaje("Sistema Gato: Ya tienes una partida activa con '" + dName + "'.");
            return true;
        }
        return false;
    }

    private boolean chequearPropuestaPendiente(UnCliente proponente, String pName, String dName) throws IOException {
        if (propuestasPendientes.getOrDefault(pName, "").equals(dName) ||
                propuestasPendientes.getOrDefault(dName, "").equals(pName)) {
            proponente.enviarMensaje("Sistema Gato: Ya tienes una propuesta pendiente (enviada o recibida) con " + dName + ".");
            return true;
        }
        return false;
    }

    private void aceptarPropuesta(UnCliente aceptante, String nombreProponente) throws IOException {
        String aceptanteNombre = aceptante.getNombreCliente();

        if (validarAceptacionInicial(aceptante, nombreProponente)) return;

        String proponenteEsperado = propuestasPendientes.get(nombreProponente);
        if (validarProponente(aceptante, nombreProponente, proponenteEsperado)) return;

        UnCliente proponente = ServidorMulti.getCliente(nombreProponente);
        if (validarEstadoProponente(aceptante, proponente, nombreProponente)) return;

        iniciarNuevoJuego(proponente, aceptante, nombreProponente, aceptanteNombre);
    }

    private boolean validarAceptacionInicial(UnCliente aceptante, String nombreProponente) throws IOException {
        if (nombreProponente.isEmpty()) {
            aceptante.enviarMensaje("Sistema Gato: Uso incorrecto. Usa acepto <usuario>");
            return true;
        }
        return false;
    }

    private boolean validarProponente(UnCliente aceptante, String pName, String pEsperado) throws IOException {
        String aceptanteNombre = aceptante.getNombreCliente();
        if (pEsperado == null || !pEsperado.equals(aceptanteNombre)) {
            aceptante.enviarMensaje("Sistema Gato: El usuario '" + pName + "' no te ha propuesto un juego.");
            return true;
        }
        return false;
    }

    private boolean validarEstadoProponente(UnCliente aceptante, UnCliente proponente, String pName) throws IOException {
        if (proponente == null || !proponente.isRegistrado()) {
            aceptante.enviarMensaje("Sistema Gato: El proponente se ha desconectado o no esta autenticado. No se pudo iniciar el juego.");
            propuestasPendientes.remove(pName);
            return true;
        }
        return false;
    }

    private void iniciarNuevoJuego(UnCliente proponente, UnCliente aceptante, String pName, String aName) throws IOException {
        String parCanonica = getCanonicalPair(pName, aName);

        if (juegosActivosPorPar.containsKey(parCanonica)) {
            aceptante.enviarMensaje("Sistema Gato: Ya existe una partida activa entre tú y el proponente. No se puede iniciar otra.");
            propuestasPendientes.remove(pName);
            return;
        }

        propuestasPendientes.remove(pName);
        JuegoGato juego = new JuegoGato(proponente, aceptante);

        juegosActivosPorPar.put(parCanonica, juego);
        asociarJuegoAJugador(pName, juego);
        asociarJuegoAJugador(aName, juego);
    }

    private void asociarJuegoAJugador(String nombre, JuegoGato juego) {
        jugadorJuegosMap.computeIfAbsent(nombre, k -> new ArrayList<>()).add(juego);
    }

    private void rechazarPropuesta(UnCliente rechazante, String nombreProponente) throws IOException {
        String rechazanteNombre = rechazante.getNombreCliente();

        if (validarRechazoInicial(rechazante, nombreProponente)) return;

        String proponenteEsperado = propuestasPendientes.get(nombreProponente);
        if (proponenteEsperado == null || !proponenteEsperado.equals(rechazanteNombre)) {
            rechazante.enviarMensaje("Sistema Gato: El usuario '" + nombreProponente + "' no te ha propuesto un juego.");
            return;
        }

        propuestasPendientes.remove(nombreProponente);
        notificarRechazo(rechazante, nombreProponente);
    }

    private boolean validarRechazoInicial(UnCliente rechazante, String nombreProponente) throws IOException {
        if (nombreProponente.isEmpty()) {
            rechazante.enviarMensaje("Sistema Gato: Uso incorrecto. Usa rechazo <usuario>");
            return true;
        }
        return false;
    }

    private void notificarRechazo(UnCliente rechazante, String pName) throws IOException {
        UnCliente proponente = ServidorMulti.getCliente(pName);
        if (proponente != null) {
            proponente.enviarMensaje("Sistema Gato: " + rechazante.getNombreCliente() + " ha rechazado tu propuesta de juego.");
        }
        rechazante.enviarMensaje("Sistema Gato: Has rechazado la propuesta de " + pName + ".");
    }

    private void manejarMovimiento(UnCliente cliente, String sFila, String sColumna) throws IOException {
        String clienteNombre = cliente.getNombreCliente();

        JuegoGato juego = buscarJuegoActivo(cliente, clienteNombre);
        if (juego == null) return;

        if (validarEstadoJuego(cliente, juego)) return;

        try {
            int fila = Integer.parseInt(sFila);
            int columna = Integer.parseInt(sColumna);
            ejecutarMovimientoYFinalizar(cliente, juego, fila, columna);
        } catch (NumberFormatException e) {
            cliente.enviarMensaje("Sistema Gato: Los argumentos para mover deben ser numeros. Usa mover <fila> <columna> (ej: mover 1 3)");
        }
    }

    private JuegoGato buscarJuegoActivo(UnCliente cliente, String cName) throws IOException {
        List<JuegoGato> juegosDelCliente = jugadorJuegosMap.get(cName);

        if (juegosDelCliente == null || juegosDelCliente.isEmpty()) {
            cliente.enviarMensaje("Sistema Gato: No estás en un juego activo. Usa /gato <usuario> para proponer uno.");
            return null;
        }

        JuegoGato juego = juegosDelCliente.stream().filter(j -> j.getTurnoActual() == cliente).findFirst().orElse(null);

        if (juego == null) {
            cliente.enviarMensaje("Sistema Gato: No es tu turno en ninguna partida activa.");
        }
        return juego;
    }

    private boolean validarEstadoJuego(UnCliente cliente, JuegoGato juego) throws IOException {
        if (juego.getEstado() != JuegoGato.EstadoJuego.ACTIVO) {
            cliente.enviarMensaje("Sistema Gato: El juego ha terminado (" + juego.getEstado().name() + ").");
            UnCliente oponente = juego.getContrincante(cliente);
            if (oponente != null) removerJuego(cliente.getNombreCliente(), oponente.getNombreCliente());
            return true;
        }
        return false;
    }

    private void ejecutarMovimientoYFinalizar(UnCliente cliente, JuegoGato juego, int fila, int columna) throws IOException {
        boolean juegoTerminado = juego.realizarMovimiento(cliente, fila, columna);
        if (juegoTerminado) {
            UnCliente oponente = juego.getContrincante(cliente);
            if (oponente != null) {
                removerJuego(cliente.getNombreCliente(), oponente.getNombreCliente());
            }
        }
    }
    public void finalizarPorDesconexion(UnCliente desconectado) throws IOException {
        String nombreDesconectado = desconectado.getNombreCliente();

        finalizarJuegosActivos(desconectado, nombreDesconectado);
        cancelarPropuestasPendientes(nombreDesconectado);
    }

    private void finalizarJuegosActivos(UnCliente desconectado, String nombreDesconectado) throws IOException {
        List<JuegoGato> juegosDelCliente = jugadorJuegosMap.get(nombreDesconectado);
        if (juegosDelCliente != null) {
            for (JuegoGato juego : new ArrayList<>(juegosDelCliente)) {
                UnCliente oponente = juego.getContrincante(desconectado);
                juego.finalizarPorAbandono(desconectado);
                String nombreOponente = (oponente != null) ? oponente.getNombreCliente() : null;
                removerJuego(nombreDesconectado, nombreOponente);
            }
        }
    }

    private void cancelarPropuestasPendientes(String nombreDesconectado) throws IOException {
        cancelarPropuestaEnviada(nombreDesconectado);
        cancelarPropuestaRecibida(nombreDesconectado);
    }

    private void cancelarPropuestaEnviada(String nombreDesconectado) throws IOException {
        String nombreDestino = propuestasPendientes.remove(nombreDesconectado);
        if (nombreDestino != null) {
            UnCliente destino = ServidorMulti.getCliente(nombreDestino);
            if (destino != null) {
                destino.enviarMensaje("Sistema Gato: La propuesta de juego de " + nombreDesconectado + " ha sido cancelada porque se ha desconectado.");
            }
        }
    }

    private void cancelarPropuestaRecibida(String nombreDesconectado) throws IOException {
        String proponenteNombre = buscarProponenteDeDesconectado(nombreDesconectado);

        if (proponenteNombre != null) {
            propuestasPendientes.remove(proponenteNombre);
            UnCliente proponente = ServidorMulti.getCliente(proponenteNombre);
            if (proponente != null) {
                proponente.enviarMensaje("Sistema Gato: La propuesta de juego a " + nombreDesconectado + " ha sido cancelada porque se ha desconectado.");
            }
        }
    }

    private String buscarProponenteDeDesconectado(String dName) {
        for(Entry<String, String> entry : propuestasPendientes.entrySet()) {
            if (entry.getValue().equals(dName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void removerJuego(String nombre1, String nombre2) {
        if (nombre1 != null && nombre2 != null) {
            String parCanonica = getCanonicalPair(nombre1, nombre2);
            juegosActivosPorPar.remove(parCanonica);
        }
        removerJuegoDeMapaJugador(nombre1, nombre2);
        removerJuegoDeMapaJugador(nombre2, nombre1);
    }

    private void removerJuegoDeMapaJugador(String nombreJugador, String nombreOponente) {
        if (nombreJugador == null || nombreOponente == null) return;

        jugadorJuegosMap.computeIfPresent(nombreJugador, (k, list) -> {
            list.removeIf(j -> esParDeJugador(j, nombreJugador, nombreOponente));
            return list.isEmpty() ? null : list;
        });
    }

    private boolean esParDeJugador(JuegoGato j, String jName, String oName) {
        String jX = j.getJugadorX().getNombreCliente();
        String jO = j.getJugadorO().getNombreCliente();

        boolean esPar1 = jX.equals(oName) && jO.equals(jName);
        boolean esPar2 = jO.equals(oName) && jX.equals(jName);
        return esPar1 || esPar2;
    }

    public Map<String, List<JuegoGato>> getJugadorJuegosMap() {
        return jugadorJuegosMap;
    }
}