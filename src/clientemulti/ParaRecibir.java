package clientemulti;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
public class ParaRecibir implements Runnable{
    final DataInputStream entrada;
    private boolean primerMensaje = true;

    public ParaRecibir(Socket s) throws IOException {
        entrada = new DataInputStream(s.getInputStream());
    }

    private void imprimirMenuRegistrado() {
        System.out.println("------------------------------------------");
        System.out.println("Opciones de chat (Registrado):");
        System.out.println("- Mensaje privado: @usuario mensaje");
        System.out.println("- Bloquear un usuario: /bloquear nombre_usuario");
        System.out.println("- Desbloquear un usuario: /desbloquear nombre_usuario");
        System.out.println("------------------------------------------");
    }

    private void imprimirMenuInvitado() {
        System.out.println("------------------------------------------");
        System.out.println("Opciones de chat (Invitado):");
        System.out.println("- Mensaje privado: @usuario mensaje");
        System.out.println("- Login/Registro: /login usuario contrasena o /registrar usuario contrasena");
        System.out.println("------------------------------------------");
    }

    @Override
    public void run() {
        String mensaje;
        mensaje = "";
        while(true){
            try {
                mensaje = entrada.readUTF();

                if (primerMensaje) {
                    if (mensaje.startsWith("Sistema: OK_REGISTRADO")) {
                        imprimirMenuRegistrado();
                        primerMensaje = false;
                    } else if (mensaje.startsWith("Sistema: OK_INVITADO")) {
                        imprimirMenuInvitado();
                        primerMensaje = false;
                    }
                }

                System.out.println(mensaje);
            } catch (IOException ex) {
            }
        }
    }

}