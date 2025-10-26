package servidormulti;

import juego.JuegoGato;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ControladorJuego {
    private final Map<String, String> propuestasPendientes;
    private final Map<String, JuegoGato> juegosActivos;

    public ControladorJuego() {
        this.propuestasPendientes = Collections.synchronizedMap(new HashMap<>());
        this.juegosActivos = Collections.synchronizedMap(new HashMap<>());
    }

    public JuegoGato getJuegoActivo(String nombre) {
        return juegosActivos.get(nombre);
    }

    public void manejarComando(String mensaje, UnCliente remitente) throws IOException {
        if (!remitente.isRegistrado()) {
            remitente.enviarMensaje("Sistema Gato: Debes estar registrado para jugar al Gato.");
            return;
        }

        String[] partes = mensaje.split(" ", 3);
        String comando = partes[0];
        String argumento1 = partes.length > 1 ? partes[1] : "";
        String argumento2 = partes.length > 2 ? partes[2] : "";

        switch (comando) {
            case "/gato":
                proponerJuego(remitente, argumento1);
                break;
            case "/accept":
                aceptarPropuesta(remitente, argumento1);
                break;
            case "/reject":
                rechazarPropuesta(remitente, argumento1);
                break;
            case "/move":
                manejarMovimiento(remitente, argumento1, argumento2);
                break;
            default:
                remitente.enviarMensaje("Sistema Gato: Comando de juego desconocido.");
                break;
        }
    }

    private void proponerJuego(UnCliente proponente, String nombreDestino) throws IOException {
        String proponenteNombre = proponente.getNombreCliente();
        if (nombreDestino.isEmpty()) {
            proponente.enviarMensaje("Sistema Gato: Uso incorrecto. Usa /gato <usuario>");
            return;
        }

        if (proponenteNombre.equals(nombreDestino)) {
            proponente.enviarMensaje("Sistema Gato: No puedes jugar al Gato contigo mismo.");
            return;
        }

        UnCliente clienteDestino = ServidorMulti.getCliente(nombreDestino);

        if (clienteDestino == null || !clienteDestino.isRegistrado()) {
            proponente.enviarMensaje("Sistema Gato: El usuario '" + nombreDestino + "' no está conectado o no está autenticado.");
            return;
        }

        if (juegosActivos.containsKey(proponenteNombre) || juegosActivos.containsKey(nombreDestino)) {
            proponente.enviarMensaje("Sistema Gato: Tú o el usuario '" + nombreDestino + "' ya están en una partida activa.");
            return;
        }

        if (propuestasPendientes.getOrDefault(proponenteNombre, "").equals(nombreDestino) ||
                propuestasPendientes.getOrDefault(nombreDestino, "").equals(proponenteNombre)) {
            proponente.enviarMensaje("Sistema Gato: Ya tienes una propuesta pendiente (enviada o recibida) con " + nombreDestino + ".");
            return;
        }

        propuestasPendientes.put(proponenteNombre, nombreDestino);

        proponente.enviarMensaje("Sistema Gato: Propuesta enviada a " + nombreDestino + ". Esperando respuesta...");
        clienteDestino.enviarMensaje("Sistema Gato: ¡" + proponenteNombre + " te ha propuesto jugar al Gato! Usa /accept " + proponenteNombre + " o /reject " + proponenteNombre + ".");
    }

    private void aceptarPropuesta(UnCliente aceptante, String nombreProponente) throws IOException {
        String aceptanteNombre = aceptante.getNombreCliente();

        if (nombreProponente.isEmpty()) {
            aceptante.enviarMensaje("Sistema Gato: Uso incorrecto. Usa /accept <usuario>");
            return;
        }

        String proponenteEsperado = propuestasPendientes.get(nombreProponente);

        if (proponenteEsperado == null || !proponenteEsperado.equals(aceptanteNombre)) {
            aceptante.enviarMensaje("Sistema Gato: El usuario '" + nombreProponente + "' no te ha propuesto un juego.");
            return;
        }

        UnCliente proponente = ServidorMulti.getCliente(nombreProponente);

        if (proponente == null || !proponente.isRegistrado()) {
            aceptante.enviarMensaje("Sistema Gato: El proponente se ha desconectado o no está autenticado. No se pudo iniciar el juego.");
            propuestasPendientes.remove(nombreProponente);
            return;
        }

        if (juegosActivos.containsKey(aceptanteNombre) || juegosActivos.containsKey(nombreProponente)) {
            aceptante.enviarMensaje("Sistema Gato: Tú o el proponente están ahora en una partida activa. No se puede iniciar otra.");
            propuestasPendientes.remove(nombreProponente);
            return;
        }

        propuestasPendientes.remove(nombreProponente);

        JuegoGato juego = new JuegoGato(proponente, aceptante);
        juegosActivos.put(nombreProponente, juego);
        juegosActivos.put(aceptanteNombre, juego);
    }

    private void rechazarPropuesta(UnCliente rechazante, String nombreProponente) throws IOException {
        String rechazanteNombre = rechazante.getNombreCliente();

        if (nombreProponente.isEmpty()) {
            rechazante.enviarMensaje("Sistema Gato: Uso incorrecto. Usa /reject <usuario>");
            return;
        }

        String proponenteEsperado = propuestasPendientes.get(nombreProponente);

        if (proponenteEsperado == null || !proponenteEsperado.equals(rechazanteNombre)) {
            rechazante.enviarMensaje("Sistema Gato: El usuario '" + nombreProponente + "' no te ha propuesto un juego.");
            return;
        }

        propuestasPendientes.remove(nombreProponente);

        UnCliente proponente = ServidorMulti.getCliente(nombreProponente);

        if (proponente != null) {
            proponente.enviarMensaje("Sistema Gato: " + rechazanteNombre + " ha rechazado tu propuesta de juego.");
        }
        rechazante.enviarMensaje("Sistema Gato: Has rechazado la propuesta de " + nombreProponente + ".");
    }

    private void manejarMovimiento(UnCliente cliente, String sFila, String sColumna) throws IOException {
        String clienteNombre = cliente.getNombreCliente();
        JuegoGato juego = getJuegoActivo(clienteNombre);

        if (juego == null) {
            cliente.enviarMensaje("Sistema Gato: No estás en un juego activo. Usa /gato <usuario> para proponer uno.");
            return;
        }

        if (juego.getEstado() != JuegoGato.EstadoJuego.ACTIVO) {
            cliente.enviarMensaje("Sistema Gato: El juego ha terminado (" + juego.getEstado().name() + ").");

            UnCliente oponente = juego.getContrincante(cliente);
            if (oponente != null) {
                removerJuego(clienteNombre, oponente.getNombreCliente());
            }
            return;
        }

        try {
            int fila = Integer.parseInt(sFila);
            int columna = Integer.parseInt(sColumna);
            boolean juegoTerminado = juego.realizarMovimiento(cliente, fila, columna);

            if (juegoTerminado) {
                UnCliente oponente = juego.getContrincante(cliente);
                if (oponente != null) {
                    removerJuego(clienteNombre, oponente.getNombreCliente());
                }
            }
        } catch (NumberFormatException e) {
            cliente.enviarMensaje("Sistema Gato: Los argumentos para /move deben ser números. Usa /move <fila> <columna> (ej: /move 1 3)");
        }
    }

    public void finalizarPorDesconexion(UnCliente desconectado) throws IOException {
        String nombreDesconectado = desconectado.getNombreCliente();

        JuegoGato juego = juegosActivos.get(nombreDesconectado);
        if (juego != null) {
            UnCliente oponente = juego.getContrincante(desconectado);

            juego.finalizarPorAbandono(desconectado);

            if (oponente != null) {
                String nombreOponente = oponente.getNombreCliente();
                removerJuego(nombreDesconectado, nombreOponente);
            } else {
                removerJuego(nombreDesconectado, null);
            }
        }

        cancelarPropuestasPendientes(nombreDesconectado);
    }

    private void cancelarPropuestasPendientes(String nombreDesconectado) throws IOException {
        String nombreDestino = propuestasPendientes.remove(nombreDesconectado);
        if (nombreDestino != null) {
            UnCliente destino = ServidorMulti.getCliente(nombreDestino);
            if (destino != null) {
                destino.enviarMensaje("Sistema Gato: La propuesta de juego de " + nombreDesconectado + " ha sido cancelada porque se ha desconectado.");
            }
        }

        String nombreProponenteA = null;
        for(Map.Entry<String, String> entry : propuestasPendientes.entrySet()) {
            if (entry.getValue().equals(nombreDesconectado)) {
                nombreProponenteA = entry.getKey();
                break;
            }
        }

        if (nombreProponenteA != null) {
            propuestasPendientes.remove(nombreProponenteA);
            UnCliente proponente = ServidorMulti.getCliente(nombreProponenteA);
            if (proponente != null) {
                proponente.enviarMensaje("Sistema Gato: La propuesta de juego a " + nombreDesconectado + " ha sido cancelada porque se ha desconectado.");
            }
        }
    }

    private void removerJuego(String nombre1, String nombre2) {
        juegosActivos.remove(nombre1);
        if (nombre2 != null) {
            juegosActivos.remove(nombre2);
        }
    }
}