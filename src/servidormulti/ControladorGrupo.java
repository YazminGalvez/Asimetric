package servidormulti;

import basededatos.BaseDatos;
import java.io.IOException;


public class ControladorGrupo {

    public boolean manejarComandoGrupo(String cmd, String[] p, UnCliente cliente) throws IOException {
        switch (cmd) {
            case "/grupo": return cambiarGrupo(p, cliente);
            case "/crear": return crearGrupo(p, cliente);
            case "/borrar": return borrarGrupo(p, cliente);
            case "/unir": return unirAGrupo(p, cliente);
            case "/salir": return salirDeGrupo(p, cliente);
            default: return false;
        }
    }

    public void enviarMensajeGrupo(String grupo, String mensaje, UnCliente remitente) throws IOException {
        String nombreRemitente = remitente.getNombreCliente();
        String msgFormateado = nombreRemitente + " [" + grupo + "]: " + mensaje;




        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente != remitente && cliente.getGrupoActual().equals(grupo)) {
                cliente.enviarMensaje(msgFormateado);
            }
        }
        remitente.enviarMensaje(msgFormateado);
    }

    private void difundirMensajeGrupo(String grupo, String mensajeCompleto, UnCliente remitente) throws IOException {
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente != remitente && cliente.getGrupoActual().equals(grupo)) {
                enviarAMiembro(cliente, mensajeCompleto, remitente.getNombreCliente());
            }
        }
    }

    private void enviarAMiembro(UnCliente cliente, String mensaje, String nombreRemitente) throws IOException {
        cliente.enviarMensaje(mensaje);
    }

    private boolean cambiarGrupo(String[] p, UnCliente cliente) throws IOException {
        if (!validarArgumentos(p, 2, "/grupo")) return enviarErrorUso(cliente, "/grupo nombre_grupo");

        String nuevoGrupo = p[1];
        if (!BaseDatos.esMiembroDeGrupo(cliente.getNombreCliente(), nuevoGrupo)) {
            cliente.enviarMensaje("Sistema: No eres miembro del grupo '" + nuevoGrupo + "'. Usa /unir.");
            return true;
        }

        cliente.setGrupoActual(nuevoGrupo);
        cliente.enviarMensaje("Sistema: Has cambiado al grupo '" + nuevoGrupo + "'.");
        return true;
    }

    private boolean crearGrupo(String[] p, UnCliente cliente) throws IOException {
        if (!cliente.isRegistrado() || !validarArgumentos(p, 2, "/crear")) {
            return enviarErrorUso(cliente, "Debes ser registrado. Uso: /crear nombre_grupo");
        }
        String nombreGrupo = p[1];
        if (BaseDatos.crearGrupo(nombreGrupo, cliente.getNombreCliente())) {
            BaseDatos.unirseAGrupo(cliente.getNombreCliente(), nombreGrupo);
            cliente.setGrupoActual(nombreGrupo);
            cliente.enviarMensaje("Sistema: Grupo '" + nombreGrupo + "' creado y te has unido.");
        } else {
            cliente.enviarMensaje("Sistema: El grupo '" + nombreGrupo + "' ya existe o nombre inválido.");
        }
        return true;
    }

    private boolean borrarGrupo(String[] p, UnCliente cliente) throws IOException {
        if (!cliente.isRegistrado() || !validarArgumentos(p, 2, "/borrar")) {
            return enviarErrorUso(cliente, "Debes ser registrado. Uso: /borrar nombre_grupo");
        }
        String nombreGrupo = p[1];
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            cliente.enviarMensaje("Sistema: El grupo 'Todos' no se puede borrar.");
            return true;
        }
        return procesarBorrado(cliente, nombreGrupo);
    }

    private boolean procesarBorrado(UnCliente cliente, String nombreGrupo) throws IOException {
        if (BaseDatos.eliminarGrupo(nombreGrupo, cliente.getNombreCliente())) {
            cliente.enviarMensaje("Sistema: Grupo '" + nombreGrupo + "' eliminado correctamente.");
            if (cliente.getGrupoActual().equals(nombreGrupo)) {
                cliente.setGrupoActual("Todos");
            }
        } else {
            cliente.enviarMensaje("Sistema: Error al borrar. No existe, o no eres el creador.");
        }
        return true;
    }

    private boolean unirAGrupo(String[] p, UnCliente cliente) throws IOException {
        if (!validarArgumentos(p, 2, "/unir")) return enviarErrorUso(cliente, "/unir nombre_grupo");
        String grupo = p[1];
        if (!cliente.isRegistrado() && !grupo.equalsIgnoreCase("Todos")) {
            cliente.enviarMensaje("Sistema: Los invitados solo pueden unirse al grupo 'Todos'.");
            return true;
        }
        return procesarUnion(cliente, grupo);
    }

    private boolean procesarUnion(UnCliente cliente, String grupo) throws IOException {
        if (BaseDatos.unirseAGrupo(cliente.getNombreCliente(), grupo)) {
            cliente.setGrupoActual(grupo);
            cliente.enviarMensaje("Sistema: Te has unido al grupo '" + grupo + "'.");
        } else {
            cliente.enviarMensaje("Sistema: Error al unirse. El grupo no existe o ya eres miembro.");
        }
        return true;
    }

    private boolean salirDeGrupo(String[] p, UnCliente cliente) throws IOException {
        if (!validarArgumentos(p, 2, "/salir")) return enviarErrorUso(cliente, "/salir nombre_grupo");
        String grupo = p[1];
        if (grupo.equalsIgnoreCase("Todos")) {
            cliente.enviarMensaje("Sistema: No puedes salir del grupo 'Todos'.");
            return true;
        }
        return procesarSalida(cliente, grupo);
    }

    private boolean procesarSalida(UnCliente cliente, String grupo) throws IOException {
        if (BaseDatos.salirDeGrupo(cliente.getNombreCliente(), grupo)) {
            cliente.enviarMensaje("Sistema: Has salido del grupo '" + grupo + "'.");
            if (cliente.getGrupoActual().equals(grupo)) {
                cliente.setGrupoActual("Todos");
            }
        } else {
            cliente.enviarMensaje("Sistema: Error al salir. No existe o no eres miembro.");
        }
        return true;
    }

    private boolean validarArgumentos(String[] p, int length, String cmd) {
        if (p.length != length) return false;
        if (p[1].trim().length() < 1) return false;
        return true;
    }

    private boolean enviarErrorUso(UnCliente cliente, String mensaje) throws IOException {
        cliente.enviarMensaje("Sistema: Uso incorrecto. " + mensaje);
        return true;
    }
}