package servidormulti;
import mensaje.Mensaje;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class UnCliente implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;
    private String nombreCliente;
    private boolean registrado = false;
    private int mensajesRestantes = 3;

    UnCliente(Socket s) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
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

    @Override
    public void run() {
        try {
            String primerMensaje = entrada.readUTF();

            if (primerMensaje.startsWith("/login") || primerMensaje.startsWith("/registrar")) {
                Mensaje.procesarComandoInicial(primerMensaje, this);
            } else {
                this.nombreCliente = "Invitado-" + primerMensaje;
                Mensaje.enviarMensajeUnico(this, "Sistema: OK_INVITADO Has entrado como invitado. (Límite: " + mensajesRestantes + " mensajes)");
                Mensaje.notificarATodos(nombreCliente + " (Invitado) se ha unido al chat.", this);
            }

            ServidorMulti.clientes.put(nombreCliente, this);

            while (true) {
                String mensaje = entrada.readUTF();
                if (mensaje.startsWith("/") && !mensaje.startsWith("@")) {
                    Mensaje.procesarComando(mensaje, this);
                } else {
                    Mensaje.procesar(mensaje, this);
                }
            }
        } catch (SocketException e) {
            if (nombreCliente != null) {
                ServidorMulti.clientes.remove(nombreCliente);
                Mensaje.notificarATodos(nombreCliente + " ha abandonado el chat.", null);
            }
        } catch (IOException ex) {
            System.out.println("Error de comunicación con " + (nombreCliente != null ? nombreCliente : "un cliente"));
        }
    }
}