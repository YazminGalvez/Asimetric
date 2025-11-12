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
        this.salida.writeUTF(mensaje);
    }

    public boolean puedeEnviarMensaje() {
        if (registrado) {
            return true;
        }
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
            procesarConexionInicial();
            mostrarMenuComandos();
            procesarMensajesDelCliente();
        } catch (SocketException e) {
            manejarDesconexion(e);
        } catch (IOException ex) {
            manejarIOException(ex);
        }
    }

    private void procesarConexionInicial() throws IOException {
        String primerMensaje = entrada.readUTF();

        if (primerMensaje.startsWith("/login") || primerMensaje.startsWith("/registrar")) {
            Mensaje.procesarComandoInicial(primerMensaje, this);
        } else {
            this.nombreCliente = "Invitado-" + primerMensaje;
            Mensaje.notificarATodos(nombreCliente + " (Invitado) se ha unido al chat.", this);
        }

        ServidorMulti.clientes.put(nombreCliente, this);
    }

    private void mostrarMenuComandos() throws IOException {
        enviarMensaje("Sistema: Juega al Gato (solo usuarios registrados):");
        enviarMensaje("Sistema: - Proponer juego: /gato <usuario>");
        enviarMensaje("Sistema: - Responder propuesta: acepto <usuario> o rechazo <usuario>");
        enviarMensaje("Sistema: - Mover ficha: mover <fila> <columna> (ej: mover 1 3)");
        enviarMensaje("Sistema: Ranking de Gato (solo usuarios registrados):");
        enviarMensaje("Sistema: - Ver Ranking General: /ranking");
        enviarMensaje("Sistema: - Estadisticas: /vs <usuario1> <usuario2>");
        enviarMensaje("Sistema: Comandos de Grupo:");
        enviarMensaje("Sistema: - Unirse/Cambiar: /grupo <nombre>");
        enviarMensaje("Sistema: - Unirse: /unir <nombre>");
        enviarMensaje("Sistema: - Salir de Grupo: /salir <nombre_grupo>");
        enviarMensaje("Sistema: - Crear/Borrar: /crear <nombre>, /borrar <nombre> (solo creador)");
    }

    private void procesarMensajesDelCliente() throws IOException {
        while (true) {
            String mensaje = entrada.readUTF();

            if (!isRegistrado()) {
                if (!puedeEnviarMensaje()) {
                    enviarMensaje("Sistema: Como invitado, has agotado tu limite de mensajes. Por favor, registrate o inicia sesion.");
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
            enviarMensaje("Sistema Gato: Debes estar registrado y logueado para jugar al Gato.");
        }
    }

    private boolean esMensajeBloqueado(String mensaje) throws IOException {
        if (estaEnPartida() && !mensaje.equalsIgnoreCase("/exit")) {
            enviarMensaje("Sistema: Estas en una partida de Gato. Solo se permite chat simple con el oponente o el comando mover.");
            return true;
        }
        return false;
    }

    private void manejarDesconexion(SocketException e) {
        if (nombreCliente != null) {
            if (isRegistrado() && controladorJuego != null) {
                try {
                    controladorJuego.finalizarPorDesconexion(this);
                } catch (IOException ioException) {
                    System.err.println("Error al finalizar juego por desconexion: " + ioException.getMessage());
                }
            }
            ServidorMulti.desconectarCliente(this);
        }
    }
    private void manejarIOException(IOException ex) {
        if (nombreCliente != null && isRegistrado() && controladorJuego != null) {
            try {
                controladorJuego.finalizarPorDesconexion(this);
            } catch (IOException ioException) {
                System.err.println("Error al finalizar juego por IOException: " + ioException.getMessage());
            }
            ServidorMulti.desconectarCliente(this);
        }
    }
}