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

    public boolean estaEnPartida() {
        List<JuegoGato> juegos = ServidorMulti.controladorJuego.getJugadorJuegosMap().get(this.nombreCliente);
        return juegos != null && !juegos.isEmpty();
    }


    @Override
    public void run() {
        try {
            String primerMensaje = entrada.readUTF();

            if (primerMensaje.startsWith("/login") || primerMensaje.startsWith("/registrar")) {
                Mensaje.procesarComandoInicial(primerMensaje, this);
            } else {
                this.nombreCliente = "Invitado-" + primerMensaje;
                Mensaje.notificarATodos(nombreCliente + " (Invitado) se ha unido al chat.", this);
            }

            ServidorMulti.clientes.put(nombreCliente, this);

            enviarMensaje("Sistema: Juega al Gato (solo usuarios registrados):");
            enviarMensaje("Sistema: - Proponer juego: /gato <usuario>");
            enviarMensaje("Sistema: - Responder propuesta: acepto <usuario> o rechazo <usuario>");
            enviarMensaje("Sistema: - Mover ficha: mover <fila> <columna> (ej: mover 1 3)");


            while (true) {
                String mensaje = entrada.readUTF();

                if (mensaje.startsWith("/gato") || mensaje.startsWith("acepto") ||
                        mensaje.startsWith("rechazo") || mensaje.startsWith("mover")) {

                    if (isRegistrado()) {
                        controladorJuego.manejarComando(mensaje, this);
                    } else {
                        enviarMensaje("Sistema Gato: Debes estar registrado y logueado para jugar al Gato.");
                    }
                }
                else if (estaEnPartida() && !mensaje.equalsIgnoreCase("/exit")) {
                    enviarMensaje("Sistema: Estas en una partida de Gato. Solo se permite chat simple con el oponente o el comando mover.");
                }
                else if (mensaje.startsWith("/") && !mensaje.startsWith("@")) {
                    Mensaje.procesarComando(mensaje, this);
                } else {
                    Mensaje.procesar(mensaje, this);
                }
            }
        } catch (SocketException e) {
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
        } catch (IOException ex) {
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
}