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

    public ParaMandar(Socket s, String nombre) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
        this.nombre = nombre;
    }

    @Override
    public void run() {
        try {
            salida.writeUTF(nombre);
        } catch (IOException e) {
            System.err.println("Error crítico: No se pudo conectar para enviar el nombre.");
            System.exit(0);
        }

        while (true) {
            try {
                String mensaje = teclado.readLine();
                if (mensaje == null) break;

                salida.writeUTF(mensaje);
                salida.flush();

            } catch (IOException e) {
                System.err.println("\n*** ERROR: No se pudo enviar el mensaje. Red caída. ***");
                System.exit(0);
            }
        }
    }
}