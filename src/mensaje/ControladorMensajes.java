package mensaje;

import servidormulti.ControladorGrupo;
import servidormulti.UnCliente;
import servidormulti.Bloqueos;
import servidormulti.ServidorMulti;
import java.io.IOException;

public class ControladorMensajes {

    private final ControladorGrupo controladorGrupo;

    public ControladorMensajes(ControladorGrupo controladorGrupo) {
        this.controladorGrupo = controladorGrupo;
    }

    public void procesar(String mensaje, UnCliente remitente) throws IOException {
        if (mensaje.startsWith("@")) {
            if (enviarMensajePrivado(mensaje, remitente)) remitente.decrementarMensajeRestante();
        } else if (mensaje.trim().isEmpty()) {
            remitente.enviarMensaje("Sistema: No puedes enviar un mensaje público vacío.");
        } else {
            controladorGrupo.enviarMensajeGrupo(remitente.getGrupoActual(), mensaje, remitente);
            remitente.decrementarMensajeRestante();
        }
    }

    public boolean enviarMensajePrivado(String mensaje, UnCliente remitente) throws IOException {
        String[] p = mensaje.split(" ", 2);
        String destStr = p[0].substring(1);
        String msgPrivado = (p.length > 1) ? p[1] : "";

        if (msgPrivado.trim().isEmpty()) {
            remitente.enviarMensaje("Sistema: No puedes enviar un mensaje privado vacío.");
            return false;
        }

        int enviados = procesarDestinos(destStr, msgPrivado, remitente);
        remitente.enviarMensaje("(Mensaje privado para " + destStr + "): " + msgPrivado);
        return enviados > 0;
    }

    private int procesarDestinos(String destStr, String msgPrivado, UnCliente remitente) throws IOException {
        String msgFormateado = "(Privado de " + remitente.getNombreCliente() + "): " + msgPrivado;
        String nombreRemitente = remitente.getNombreCliente();
        int enviados = 0;

        for (String dest : destStr.split(",")) {
            enviados += enviarAUnDestino(dest.trim(), nombreRemitente, msgFormateado, remitente);
        }
        return enviados;
    }

    private int enviarAUnDestino(String nombreDestino, String nombreRemitente, String msgFormateado, UnCliente remitente) throws IOException {
        if (!validarFormatoDestino(nombreDestino, remitente)) return 0;

        UnCliente destino = ServidorMulti.clientes.get(nombreDestino);

        if (destino == null) {
            remitente.enviarMensaje("Sistema: El usuario '" + nombreDestino + "' no está conectado o no existe.");
            return 0;
        }

        if (chequearBloqueo(nombreRemitente, nombreDestino, destino, remitente)) return 0;

        destino.enviarMensaje(msgFormateado);
        return 1;
    }

    private boolean validarFormatoDestino(String nombreDestino, UnCliente remitente) throws IOException {
        if (nombreDestino.length() < 4 || !nombreDestino.matches("^[a-zA-Z0-9]+$")) {
            remitente.enviarMensaje("Sistema: El usuario destino '" + nombreDestino + "' no tiene un formato válido para un usuario registrado.");
            return false;
        }
        return true;
    }

    private boolean chequearBloqueo(String nombreRemitente, String nombreDestino, UnCliente destino, UnCliente remitente) throws IOException {
        if (destino.isRegistrado() && Bloqueos.estaBloqueadoPor(nombreRemitente, nombreDestino)) {
            remitente.enviarMensaje("Sistema: Tu mensaje a '" + nombreDestino + "' no fue entregado porque te ha bloqueado.");
            return true;
        }
        return false;
    }

    public void difundirMensajePublico(String mensaje, UnCliente remitente) throws IOException {
        String msgCompleto = remitente.getNombreCliente() + ": " + mensaje;
        String nombreRemitente = remitente.getNombreCliente();

        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente != remitente) {
                enviarMensajeACliente(cliente, remitente, msgCompleto);
            }
        }
    }

    private void enviarMensajeACliente(UnCliente cliente, UnCliente remitente, String msgCompleto) throws IOException {
        String nombreDestino = cliente.getNombreCliente();
        String nombreRemitente = remitente.getNombreCliente();

        if (cliente.isRegistrado() && Bloqueos.estaBloqueadoPor(nombreRemitente, nombreDestino)) {
            return;
        }
        cliente.enviarMensaje(msgCompleto);
    }
}