package mensaje ;
import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import java.io.IOException;

public class Mensaje {

    public static void procesar(String mensaje, UnCliente remitente) throws IOException {

        if (!remitente.puedeEnviarMensaje()) {
            remitente.enviarMensaje("Sistema: Límite de 3 mensajes como invitado alcanzado. Por favor, /login o /registrar.");
            return;
        }

        if (mensaje.startsWith("@")) {
            enviarMensajePrivado(mensaje, remitente);
        } else {
            difundirMensajePublico(mensaje, remitente);
        }
        remitente.decrementarMensajeRestante();
    }
    public static void procesarComando(String mensaje, UnCliente cliente) throws IOException {
        cliente.enviarMensaje("Sistema:Usa /login o /registrar.");
    }

    public static void procesarComandoInicial(String mensaje, UnCliente cliente) throws IOException {
        cliente.enviarMensaje("Sistema:Por favor, ingresa tu nombre de invitado o un comando válido.");
    }

    private static void enviarMensajePrivado(String mensaje, UnCliente remitente) throws IOException {
        String[] partes = mensaje.split(" ", 2);
        String destinatariosStr = partes[0].substring(1);
        String mensajePrivado = (partes.length > 1) ? partes[1] : "";

        if (mensajePrivado.isEmpty()) {
            remitente.enviarMensaje("Sistema: No puedes enviar un mensaje privado vacío.");
            return;
        }

        String[] destinatarios = destinatariosStr.split(",");
        String mensajeFormateado = "(Privado de " + remitente.getNombreCliente() + "): " + mensajePrivado;

        for (String dest : destinatarios) {
            String nombreDestinatario = dest.trim();
            UnCliente clienteDestino = ServidorMulti.clientes.get(nombreDestinatario);

            if (clienteDestino != null) {
                clienteDestino.enviarMensaje(mensajeFormateado);
            } else {
                remitente.enviarMensaje("Sistema: El usuario '" + nombreDestinatario + "' no está conectado o no existe.");
            }
        }
        remitente.enviarMensaje("(Mensaje privado para " + destinatariosStr + "): " + mensajePrivado);
    }

    private static void difundirMensajePublico(String mensaje, UnCliente remitente) throws IOException {
        String mensajeCompleto = remitente.getNombreCliente() + ": " + mensaje;
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente != remitente) {
                cliente.enviarMensaje(mensajeCompleto);
            }
        }
    }
    public static void notificarATodos(String notificacion, UnCliente clienteExcluido) {
        System.out.println(notificacion);
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            try {
                if (cliente != clienteExcluido) {
                    cliente.enviarMensaje("Sistema: " + notificacion);
                }
            } catch (IOException e) {
            }
        }
    }
}