package clientemulti;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ParaRecibir implements Runnable {
    final DataInputStream entrada;

    public ParaRecibir(Socket s) throws IOException {
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        String mensaje;
        while (true) {
            try {
                mensaje = entrada.readUTF();

                if (mensaje.equals("/ping")) {
                    continue;
                }

                System.out.println(mensaje);

            } catch (IOException ex) {
                System.err.println("\n*** ERROR DE CONEXIÓN: El servidor dejó de responder. ***");
                System.exit(0);
            }
        }
    }
}