package clientemulti;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClienteMulti {

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 8080);

        System.out.println("Opciones de inicio:");
        System.out.println("1. Entrar como invitado (3 mensajes limitados)");
        System.out.println("2. Iniciar sesión (/login usuario contrasena)");
        System.out.println("3. Registrarse (/registrar usuario contrasena)");
        System.out.print("Introduce tu nombre de invitado o un comando (ej: /login user pass): ");

        Scanner teclado = new Scanner(System.in);
        String entradaInicial = teclado.nextLine();

        System.out.println("------------------------------------------");
        System.out.println("Opciones de chat:");
        System.out.println("- Mensaje privado: @usuario mensaje");
        System.out.println("- Login/Registro: /login usuario contrasena o /registrar usuario contrasena");
        System.out.println("- Bloquear un usuario (solo reg.): /bloquear nombre_usuario");
        System.out.println("- Desbloquear un usuario (solo reg.): /desbloquear nombre_usuario");
        System.out.println("------------------------------------------");


        ParaMandar paraMandar = new ParaMandar(s, entradaInicial);
        Thread hiloParaMandar = new Thread(paraMandar);
        hiloParaMandar.start();

        ParaRecibir paraRecibir = new ParaRecibir(s);
        Thread hiloParaRecibir = new Thread(paraRecibir);
        hiloParaRecibir.start();
    }
}