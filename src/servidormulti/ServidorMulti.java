package servidormulti;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import mensaje.Mensaje;

public class ServidorMulti {

    public static final Map<String, UnCliente> clientes = new HashMap<>();
    public static ControladorJuego controladorJuego;

    public static void main(String[] args) throws IOException {
        controladorJuego = new ControladorJuego();

        ServerSocket servidorSocket = new ServerSocket(8080);
        System.out.println("Servidor iniciado y esperando clientes...");

        while (true) {
            Socket s = servidorSocket.accept();
            UnCliente unCliente = new UnCliente(s, controladorJuego);
            Thread hilo = new Thread(unCliente);
            hilo.start();
        }
    }

    public static UnCliente getCliente(String nombre) {
        UnCliente cliente = clientes.get(nombre);
        if (cliente != null && cliente.isRegistrado()) {
            return cliente;
        }
        return null;
    }

    public static void desconectarCliente(UnCliente cliente) {
        String nombre = cliente.getNombreCliente();
        if (nombre != null) {
            clientes.remove(nombre);
            Mensaje.notificarATodos(nombre + " ha abandonado el chat.", null);
        }
    }
}