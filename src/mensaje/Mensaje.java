package mensaje ;
import servidormulti.Usuarios;
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
            if (enviarMensajePrivado(mensaje, remitente)) remitente.decrementarMensajeRestante();
        } else if (mensaje.trim().isEmpty()) {
            remitente.enviarMensaje("Sistema: No puedes enviar un mensaje público vacío.");
        } else {
            difundirMensajePublico(mensaje, remitente);
            remitente.decrementarMensajeRestante();
        }
    }
    public static void procesarComando(String mensaje, UnCliente cliente) throws IOException {
        String[] p = mensaje.split(" ");
        String cmd = p[0];
        if (p.length != 3) {
            cliente.enviarMensaje("Sistema: Uso correcto: " + cmd + " usuario contrasena");
            return;
        }
        String user = p[1], pass = p[2];
        boolean exito = cmd.equals("/login")
                ? Usuarios.verificarCredenciales(user, pass)
                : Usuarios.registrarUsuario(user, pass);
        if (exito) {
            if (!cliente.getNombreCliente().equals(user)) ServidorMulti.clientes.remove(cliente.getNombreCliente());

            cliente.setRegistrado(user);
            ServidorMulti.clientes.put(user, cliente);
            String msg = (cmd.equals("/login")) ? "¡Login exitoso!" : "¡Registro exitoso!";
            cliente.enviarMensaje("Sistema: " + msg + " Ahora eres un usuario registrado (" + user + ").");
        } else {
            String errorMsg = (cmd.equals("/login")) ? "Error de login. Credenciales inválidas." : "Error de registro. El usuario ya existe.";
            cliente.enviarMensaje("Sistema: " + errorMsg);
        }
    }
    public static void procesarComandoInicial(String mensaje, UnCliente cliente) throws IOException {
        String[] p = mensaje.split(" ");
        boolean exito = false;
        String usuario = "";
        String temp = "Invitado-Temporal";
        if (p.length == 3) {
            usuario = p[1];
            if (p[0].equals("/login")) exito = Usuarios.verificarCredenciales(usuario, p[2]);
            else if (p[0].equals("/registrar")) exito = Usuarios.registrarUsuario(usuario, p[2]);
        }
        if (exito) {
            cliente.setRegistrado(usuario);
            cliente.enviarMensaje("Sistema: ¡Login exitoso! Bienvenido de nuevo.");
            notificarATodos(usuario + " se ha unido al chat.", cliente);
        } else {
            cliente.enviarMensaje("Sistema: Error en el comando de inicio de sesión/registro o credenciales inválidas. Puedes usar /login o /registrar en cualquier momento, o empezar a chatear como invitado.");
            cliente.setStatusRegistro(false);
            cliente.setNombre(temp);
            notificarATodos(temp + " (Temporal) se ha unido al chat.", cliente);
        }
    }

    private static boolean enviarMensajePrivado(String mensaje, UnCliente remitente) throws IOException {
        String[] p = mensaje.split(" ", 2);
        String destStr = p[0].substring(1);
        String msgPrivado = (p.length > 1) ? p[1] : "";
        if (msgPrivado.trim().isEmpty()) {
            remitente.enviarMensaje("Sistema: No puedes enviar un mensaje privado vacío.");
            return false;
        }
        String msgFormateado = "(Privado de " + remitente.getNombreCliente() + "): " + msgPrivado;
        int enviados = 0;

        for (String dest : destStr.split(",")) {
            UnCliente destino = ServidorMulti.clientes.get(dest.trim());
            if (destino != null) {
                destino.enviarMensaje(msgFormateado);
                enviados++;
            } else {
                remitente.enviarMensaje("Sistema: El usuario '" + dest.trim() + "' no está conectado o no existe.");
            }
        }
        remitente.enviarMensaje("(Mensaje privado para " + destStr + "): " + msgPrivado);
        return enviados > 0;
    }
    private static void difundirMensajePublico(String mensaje, UnCliente remitente) throws IOException {
        String msgCompleto = remitente.getNombreCliente() + ": " + mensaje;
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente != remitente) {
                cliente.enviarMensaje(msgCompleto);
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