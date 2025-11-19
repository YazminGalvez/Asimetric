package clientemulti;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ParaRecibir implements Runnable {
    final DataInputStream entrada;
    private final Socket socket;

    public ParaRecibir(Socket s) throws IOException {
        this.socket = s;
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        String mensaje;
        while (true) {
            try {
                if (socket.isClosed()) {
                    break;
                }
                mensaje = entrada.readUTF();
                System.out.println(mensaje);
            } catch (IOException ex) {
                if (!socket.isClosed()) {
                    System.err.println("\n*** ERROR DE CONEXIÓN: Se ha perdido la conexión con el servidor. ***");
                    System.err.println("Por favor, reinicie la aplicación para volver a intentar.");

                    try {
                        socket.close();
                    } catch (IOException closeEx) {
                    }
                }
                break;
            }
        }
    }
}