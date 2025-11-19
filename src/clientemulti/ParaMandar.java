package clientemulti;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ParaMandar implements Runnable {

    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataOutputStream salida;
    private final String nombre;
    private final Socket socket;

    public ParaMandar(Socket s, String nombre) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
        this.nombre = nombre;
    }
    @Override
    public void run() {
        try {
            salida.writeUTF(nombre);
        } catch (IOException e) {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException closeEx) {}
            return;
        }

        while (true) {
            String mensaje;
            try {
                if (socket.isClosed()) {
                    break;
                }

                mensaje = teclado.readLine();
                salida.writeUTF(mensaje);
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("\n*** ERROR DE CONEXIÓN: Fallo al enviar mensaje. Cerrando conexión. ***");
                    try {
                        socket.close();
                    } catch (IOException closeEx) {}
                }
                break;
            }
        }
    }
}