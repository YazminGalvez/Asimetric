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

        if (cmd.equals("/login") || cmd.equals("/registrar")) {
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
        } else if (cmd.equals("/bloquear") || cmd.equals("/desbloquear")) { // <-- Lógica de bloqueo
            if (!cliente.isRegistrado()) {
                cliente.enviarMensaje("Sistema: Debes estar registrado y logueado para usar el comando " + cmd + ".");
                return;
            }
            if (p.length != 2) {
                cliente.enviarMensaje("Sistema: Uso correcto: " + cmd + " nombre_usuario"); // <-- Mensaje de uso correcto para /bloquear
                return;
            }

            String remitente = cliente.getNombreCliente();
            String objetivo = p[1];

            if (objetivo.equals(remitente)) {
                cliente.enviarMensaje("Sistema: No puedes bloquearte/desbloquearte a ti mismo.");
                return;
            }

            if (cmd.equals("/bloquear")) {
                if (Usuarios.bloquearUsuario(remitente, objetivo)) {
                    cliente.enviarMensaje("Sistema: Has bloqueado a '" + objetivo + "'. Ya no recibirás sus mensajes públicos ni privados.");
                } else {
                    cliente.enviarMensaje("Sistema: Error al bloquear a '" + objetivo + "'. El usuario no existe, ya estaba bloqueado o intentaste bloquearte a ti mismo.");
                }
            } else {
                if (Usuarios.desbloquearUsuario(remitente, objetivo)) {
                    cliente.enviarMensaje("Sistema: Has desbloqueado a '" + objetivo + "'. Ahora recibirás sus mensajes.");
                } else {
                    cliente.enviarMensaje("Sistema: Error al desbloquear a '" + objetivo + "'. El usuario no estaba bloqueado o no existe.");
                }
            }
        } else {
            cliente.enviarMensaje("Sistema: Comando desconocido.");
        }
    }
    public static void procesarComandoInicial(String mensaje, UnCliente cliente) throws IOException {
        String[] p = mensaje.split(" ");
        boolean exito = false;
        String usuario = "";
        String temp = "Invitado-Temporal";
        String errorMsg = "Error en el comando de inicio de sesión/registro o credenciales inválidas.";

        if (p.length == 3) {
            usuario = p[1];
            if (p[0].equals("/login")) {
                exito = Usuarios.verificarCredenciales(usuario, p[2]);
                if (!exito) {
                    errorMsg = "Error de login. Credenciales inválidas para: " + usuario;
                }
            }
            else if (p[0].equals("/registrar")) {
                exito = Usuarios.registrarUsuario(usuario, p[2]);
                if (!exito) {
                    errorMsg = "Error de registro. El usuario " + usuario + " ya existe.";
                }
            }
        }

        if (exito) {
            cliente.setRegistrado(usuario);
            cliente.enviarMensaje("Sistema: ¡Login exitoso! Bienvenido de nuevo.");
            notificarATodos(usuario + " se ha unido al chat.", cliente);
        } else {
            cliente.enviarMensaje("Sistema: " + errorMsg + " Puedes usar /login o /registrar en cualquier momento, o empezar a chatear como invitado.");
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
        String nombreRemitente = remitente.getNombreCliente();

        for (String dest : destStr.split(",")) {
            String nombreDestino = dest.trim();
            UnCliente destino = ServidorMulti.clientes.get(nombreDestino);

            if (destino != null) {
                if (destino.isRegistrado() && Usuarios.estaBloqueadoPor(nombreRemitente, nombreDestino)) {
                    remitente.enviarMensaje("Sistema: Tu mensaje a '" + nombreDestino + "' no fue entregado porque te ha bloqueado.");
                    continue;
                }

                destino.enviarMensaje(msgFormateado);
                enviados++;
            } else {
                remitente.enviarMensaje("Sistema: El usuario '" + nombreDestino + "' no está conectado o no existe.");
            }
        }
        remitente.enviarMensaje("(Mensaje privado para " + destStr + "): " + msgPrivado);
        return enviados > 0;
    }
    private static void difundirMensajePublico(String mensaje, UnCliente remitente) throws IOException {
        String msgCompleto = remitente.getNombreCliente() + ": " + mensaje;
        String nombreRemitente = remitente.getNombreCliente();

        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente != remitente) {
                String nombreDestino = cliente.getNombreCliente();

                if (cliente.isRegistrado() && Usuarios.estaBloqueadoPor(nombreRemitente, nombreDestino)) {
                    continue;
                }

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