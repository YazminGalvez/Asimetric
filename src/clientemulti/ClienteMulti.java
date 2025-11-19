package clientemulti;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClienteMulti {

    private static String validarFormatoCredenciales(String user, String pass) {
        if (user == null || pass == null || user.length() < 4 || pass.length() < 4) {
            return "El nombre de usuario y la contraseña deben tener al menos 4 caracteres.";
        }
        if (!user.matches("^[a-zA-Z0-9]+$")) {
            return "El nombre de usuario solo debe contener letras y números.";
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 8080);
        s.setSoTimeout(4000);

        System.out.println("Opciones de inicio:");
        System.out.println("1. Entrar como invitado (3 mensajes limitados)");
        System.out.println("2. Iniciar sesión (/login usuario contrasena)");
        System.out.println("3. Registrarse (/registrar usuario contrasena)");

        Scanner teclado = new Scanner(System.in);
        String entradaInicial;

        while (true) {
            System.out.print("Introduce tu nombre de invitado o un comando (ej: /login user pass): ");
            entradaInicial = teclado.nextLine().trim();

            if (entradaInicial.isEmpty()) {
                System.out.println("Sistema (Cliente): La entrada no puede estar vacía.");
                continue;
            }

            if (entradaInicial.startsWith("/login") || entradaInicial.startsWith("/registrar")) {
                String[] p = entradaInicial.split(" ");
                if (p.length != 3) {
                    System.out.println("Sistema (Cliente): Uso incorrecto. El comando " + p[0] + " requiere usuario y contraseña (ej: " + p[0] + " user pass).");
                    continue;
                }
                String user = p[1], pass = p[2];
                String errorFormato = validarFormatoCredenciales(user, pass);
                if (errorFormato != null) {
                    System.out.println("Sistema (Cliente): Error de formato. " + errorFormato);
                    continue;
                }
            } else if (entradaInicial.startsWith("/")) {
                System.out.println("Sistema (Cliente): Comando inicial inválido. Solo se acepta /login o /registrar.");
                continue;
            }
            break;
        }

        ParaMandar paraMandar = new ParaMandar(s, entradaInicial);
        Thread hiloParaMandar = new Thread(paraMandar);
        hiloParaMandar.start();

        ParaRecibir paraRecibir = new ParaRecibir(s);
        Thread hiloParaRecibir = new Thread(paraRecibir);
        hiloParaRecibir.start();


        Thread hiloHeartbeat = new Thread(() -> {
            try {
                DataOutputStream salidaPing = new DataOutputStream(s.getOutputStream());
                while (true) {
                    Thread.sleep(200);
                    synchronized(s) {
                        salidaPing.writeUTF("/ping");
                        salidaPing.flush();
                    }
                }
            } catch (InterruptedException e) {
            } catch (IOException e) {
                System.err.println("\n*** ERROR DE CONEXIÓN: Red caída . ***");
                System.exit(0);
            }
        });
        hiloHeartbeat.start();
    }
}