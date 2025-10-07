package clientemulti;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClienteMulti {

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 8080);

        System.out.print("Introduce tu nombre: ");
        Scanner teclado = new Scanner(System.in);
        String nombre = teclado.nextLine();

        System.out.println("Para enviar un mensaje privado, usa: @usuario mensaje");

        ParaMandar paraMandar = new ParaMandar(s, nombre);
        Thread hiloParaMandar = new Thread(paraMandar);
        hiloParaMandar.start();

        ParaRecibir paraRecibir = new ParaRecibir(s);
        Thread hiloParaRecibir = new Thread(paraRecibir);
        hiloParaRecibir.start();
    }
}