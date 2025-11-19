package servidormulti;
import juego.JuegoGato;
import mensaje.Mensaje;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class UnCliente implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;
    private String nombreCliente;
    private boolean registrado = false;
    private int mensajesRestantes = 3;
    private String grupoActual = "Todos";
    private boolean propuestaJuegoPendiente = false;

    private final ControladorJuego controladorJuego;

    UnCliente(Socket s, ControladorJuego controladorJuego) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        this.controladorJuego = controladorJuego;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void enviarMensaje(String mensaje) throws IOException {
        synchronized (salida) {
            this.salida.writeUTF(mensaje);
        }
    }

    public boolean puedeEnviarMensaje() {
        if (registrado) return true;
        return mensajesRestantes > 0;
    }

    public void decrementarMensajeRestante() {
        if (!registrado && mensajesRestantes > 0) {
            mensajesRestantes--;
        }
    }

    public void setRegistrado(String nombre) {
        this.registrado = true;
        this.nombreCliente = nombre;
    }

    public void setNombre(String nombre) {
        this.nombreCliente = nombre;
    }

    public void setStatusRegistro(boolean estado) {
        this.registrado = estado;
    }

    public boolean isRegistrado() {
        return registrado;
    }

    public String getGrupoActual() {
        return grupoActual;
    }

    public void setGrupoActual(String grupo) {
        this.grupoActual = grupo;
    }

    public boolean estaEnPartida() {
        List<JuegoGato> juegos = ServidorMulti.controladorJuego.getJugadorJuegosMap().get(this.nombreCliente);
        return juegos != null && !juegos.isEmpty();
    }

    public boolean tienePropuestaJuegoPendiente() {
        return propuestaJuegoPendiente;
    }

    public void setPropuestaJuegoPendiente(boolean estado) {
        this.propuestaJuegoPendiente = estado;
    }

    @Override
    public void run() {
        try {
            iniciarLatidoCardiaco();
            procesarConexionInicial();
            mostrarMenuComandos();
            procesarMensajesDelCliente();
        } catch (EOFException e) {
        } catch (SocketException e) {
            System.err.println(nombreCliente + " se ha desconectado (SocketException).");
        } catch (IOException ex) {
            System.err.println("Error de IO con " + nombreCliente);
        } finally {
            limpiarYDesconectar();
        }
    }

    private void iniciarLatidoCardiaco() {
        Thread hiloPing = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    enviarMensaje("/ping");
                } catch (Exception e) {
                    break;
                }
            }
        });
        hiloPing.start();
    }

    private void limpiarYDesconectar() {
        if (nombreCliente == null) return;

        System.out.println("Limpiando conexión de: " + nombreCliente);

        if (isRegistrado() && controladorJuego != null) {
            try {
                controladorJuego.finalizarPorDesconexion(this);
            } catch (IOException ioException) {
                System.err.println("Error cerrando juego: " + ioException.getMessage());
            }
        }
        ServidorMulti.desconectarCliente(this);

        try { entrada.close(); } catch (Exception e) {}
        try { salida.close(); } catch (Exception e) {}
    }

    private void procesarConexionInicial() throws IOException {
        boolean identificado = false;

        while (!identificado) {
            String mensaje = entrada.readUTF();

            if (mensaje.equals("/ping")) continue;

            if (mensaje.startsWith("/")) {
                if (mensaje.startsWith("/login") || mensaje.startsWith("/registrar")) {
                    boolean exito = Mensaje.procesarComandoInicial(mensaje, this);
                    if (exito) {
                        identificado = true;
                    }
                } else {
                    enviarMensaje("Sistema: Comando no reconocido en el inicio. Usa /login o /registrar.");
                }
            } else {
                if (mensaje.contains("/")) {
                    enviarMensaje("Sistema: El nombre de invitado no puede contener el caracter '/'. Intenta de nuevo.");
                } else {
                    this.nombreCliente = "Invitado-" + mensaje;
                    Mensaje.notificarATodos(nombreCliente + " (Invitado) se ha unido al chat.", this);
                    identificado = true;
                }
            }
        }
        ServidorMulti.clientes.put(nombreCliente, this);
    }

    // En src/servidormulti/UnCliente.java

    private void mostrarMenuComandos() throws IOException {
        enviarMensaje("Sistema: Juega al Gato:");
        enviarMensaje("Sistema: - Proponer juego: /gato <usuario>");
        enviarMensaje("Sistema: - Responder propuesta: acepto <usuario> o rechazo <usuario>");
        enviarMensaje("Sistema: - Mover ficha: mover <fila> <columna> (ej: mover 1 3)");

        enviarMensaje("Sistema: Ranking de Gato:");
        enviarMensaje("Sistema: - Ver Ranking General: /ranking");
        enviarMensaje("Sistema: - Estadisticas: /vs <usuario1> <usuario2>");

        enviarMensaje("Sistema: Privacidad y Bloqueos:");
        enviarMensaje("Sistema: - Bloquear usuario: /bloquear <usuario>");
        enviarMensaje("Sistema: - Desbloquear usuario: /desbloquear <usuario>");

        enviarMensaje("Sistema: Comandos de Grupo:");
        enviarMensaje("Sistema: - Unirse/Cambiar: /grupo <nombre>");
        enviarMensaje("Sistema: - Unirse: /unir <nombre>");
        enviarMensaje("Sistema: - Salir de Grupo: /salir <nombre_grupo>");
        enviarMensaje("Sistema: - Crear/Borrar: /crear <nombre>, /borrar <nombre> (solo creador)");
    }

    private void procesarMensajesDelCliente() throws IOException {
        while (true) {
            String mensaje = entrada.readUTF();

            if (mensaje.equals("/ping")) continue;

            if (!isRegistrado()) {
                if (!puedeEnviarMensaje()) {
                    enviarMensaje("Sistema: Límite de mensajes alcanzado.");
                    continue;
                }
                decrementarMensajeRestante();
            }

            if (esComandoDeJuego(mensaje)) {
                manejarComandoDeJuego(mensaje);
            } else if (esMensajeBloqueado(mensaje)) {
            } else if (mensaje.startsWith("/") && !mensaje.startsWith("@")) {
                Mensaje.procesarComando(mensaje, this);
            } else {
                Mensaje.procesar(mensaje, this);
            }
        }
    }

    private boolean esComandoDeJuego(String mensaje) {
        return mensaje.startsWith("/gato") || mensaje.startsWith("acepto") ||
                mensaje.startsWith("rechazo") || mensaje.startsWith("mover");
    }

    private void manejarComandoDeJuego(String mensaje) throws IOException {
        if (isRegistrado()) {
            controladorJuego.manejarComando(mensaje, this);
        } else {
            enviarMensaje("Sistema Gato: Debes estar registrado.");
        }
    }

    private boolean esMensajeBloqueado(String mensaje) throws IOException {
        if (estaEnPartida() && !mensaje.equalsIgnoreCase("/exit")) {
            enviarMensaje("Sistema: Estas en partida. Usa comandos.");
            return true;
        }
        return false;
    }
}