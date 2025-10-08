package servidormulti;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServidorMulti {

    public static HashMap<String, UnCliente> clientes = new HashMap<String, UnCliente>();
    public static void main(String[] args) throws IOException {
        ServerSocket servidorSocket = new ServerSocket(8080);
        int contador = 0;
        System.out.println("Servidor iniciado y esperando clientes...");

        while (true) {
            Socket s = servidorSocket.accept();
            UnCliente unCliente = new UnCliente(s);
            Thread hilo = new Thread(unCliente);
            clientes.put(Integer.toString(contador), unCliente);
            hilo.start();
            contador++;
        }
    }
}


