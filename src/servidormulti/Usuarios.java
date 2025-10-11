package servidormulti;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Usuarios {
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final Map<String, String> credenciales = new HashMap<>();

    static {
        cargarUsuarios();
    }

    private static void cargarUsuarios() {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2) {
                    credenciales.put(partes[0], partes[1]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Archivo de usuarios no encontrado. Se creará uno al registrar el primer usuario.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void guardarUsuario(String usuario, String contrasena) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            pw.println(usuario + ":" + contrasena);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean registrarUsuario(String usuario, String contrasena) {
        if (credenciales.containsKey(usuario)) {
            return false;
        }
        credenciales.put(usuario, contrasena);
        guardarUsuario(usuario, contrasena);
        return true;
    }

    public static boolean verificarCredenciales(String usuario, String contrasena) {
        return contrasena.equals(credenciales.get(usuario));
    }
}