package mensaje;

import basededatos.BdGrupos;
import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import servidormulti.ServicioRanking;
import servidormulti.ControladorGrupo;
import java.io.IOException;

public class Mensaje {

    private static final ServicioRanking servicioRanking = new ServicioRanking();
    private static final ControladorGrupo controladorGrupo = new ControladorGrupo();
    private static final ControladorComandos procesador = new ControladorComandos(servicioRanking, controladorGrupo);
    private static final ControladorMensajes manejador = new ControladorMensajes(controladorGrupo);

    public static void procesar(String mensaje, UnCliente remitente) throws IOException {
        if (!remitente.puedeEnviarMensaje()) {
            remitente.enviarMensaje("Sistema: Límite de 3 mensajes como invitado alcanzado. Por favor, /login o /registrar.");
            return;
        }
        manejador.procesar(mensaje, remitente);
    }

    public static void procesarComando(String mensaje, UnCliente cliente) throws IOException {
        String[] p = mensaje.split(" ");
        String cmd = p[0];

        if (controladorGrupo.manejarComandoGrupo(cmd, p, cliente)) return;

        boolean comandoManejado = procesador.procesarComando(cmd, p, cliente);

        if (!comandoManejado) {
            cliente.enviarMensaje("Sistema: Comando desconocido.");
        }
    }

    public static boolean procesarComandoInicial(String mensaje, UnCliente cliente) throws IOException {
        String[] p = mensaje.split(" ");

        ControladorComandos.ResultadoInicial res = procesador.procesarComandoInicial(p);

        if (res.isExito()) {
            manejarInicioSesionExitoso(cliente, res.getUsuario(), p);
            return true;
        } else {

            cliente.enviarMensaje("Sistema: " + res.getErrorMsg() + " Vuelve a intentarlo.");
            return false;
        }
    }

    public static void enviarMensajeUnico(UnCliente cliente, String mensaje) throws IOException {
        cliente.enviarMensaje(mensaje);
    }

    private static void manejarInicioSesionExitoso(UnCliente cliente, String usuario, String[] p) throws IOException {
        cliente.setRegistrado(usuario);
        if (p.length == 3 && p[0].equals("/login")) {
            BdGrupos.unirseAGrupo(usuario, "Todos");
        }
        cliente.enviarMensaje("Sistema: OK_REGISTRADO ¡Login exitoso! Bienvenido de nuevo.");
        notificarATodos(usuario + " se ha unido al chat.", cliente);
    }

    public static void notificarATodos(String notificacion, UnCliente clienteExcluido) {
        System.out.println(notificacion);
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            try {
                enviarNotificacion(cliente, clienteExcluido, notificacion);
            } catch (IOException e) {
            }
        }
    }

    private static void enviarNotificacion(UnCliente cliente, UnCliente clienteExcluido, String notificacion) throws IOException {
        if (cliente != clienteExcluido) {
            cliente.enviarMensaje("Sistema: " + notificacion);
        }
    }
}