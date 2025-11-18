package mensaje;
import basededatos.BdUsuario;
import basededatos.BdGrupos;
import servidormulti.Bloqueos;
import servidormulti.ControladorGrupo;
import servidormulti.ServicioRanking;
import servidormulti.ServidorMulti;
import servidormulti.UnCliente;
import java.io.IOException;

public class ControladorComandos {

    private static final java.util.Set<String> COMANDOS_PROHIBIDOS = java.util.Set.of(
            "login", "registrar", "bloquear", "desbloquear", "ranking", "vs", "gato"
    );

    private final ServicioRanking servicioRanking;
    private final ControladorGrupo controladorGrupo;

    public ControladorComandos(ServicioRanking servicioRanking, ControladorGrupo controladorGrupo) {
        this.servicioRanking = servicioRanking;
        this.controladorGrupo = controladorGrupo;
    }

    public static class ResultadoInicial {
        private final boolean exito;
        private final String usuario;
        private final String errorMsg;

        public ResultadoInicial(boolean exito, String usuario, String errorMsg) {
            this.exito = exito;
            this.usuario = usuario;
            this.errorMsg = errorMsg;
        }

        public boolean isExito() { return exito; }
        public String getUsuario() { return usuario; }
        public String getErrorMsg() { return errorMsg; }
    }

    public boolean procesarComando(String cmd, String[] p, UnCliente cliente) throws IOException {
        if (cmd.equals("/login") || cmd.equals("/registrar")) return manejarLoginRegistro(p, cliente, cmd);
        if (cmd.equals("/bloquear") || cmd.equals("/desbloquear")) return manejarBloqueoDesbloqueo(p, cliente, cmd);
        if (cmd.equals("/ranking")) return manejarRanking(cliente);
        if (cmd.equals("/vs")) return manejarVs(p, cliente);
        return false;
    }

    private boolean manejarLoginRegistro(String[] p, UnCliente cliente, String cmd) throws IOException {
        if (p.length != 3) {
            cliente.enviarMensaje("Sistema: Uso correcto: " + cmd + " usuario contrasena");
            return true;
        }
        String user = p[1], pass = p[2];
        String errorFormato = validarFormatoCredenciales(user, pass);
        if (errorFormato != null) {
            cliente.enviarMensaje("Sistema: Error de formato. " + errorFormato);
            return true;
        }
        ejecutarAutenticacion(user, pass, cmd, cliente);
        return true;
    }

    private void ejecutarAutenticacion(String user, String pass, String cmd, UnCliente cliente) throws IOException {
        boolean exito = cmd.equals("/login")
                ? BdUsuario.verificarCredenciales(user, pass)
                : BdUsuario.registrarUsuario(user, pass);
        if (exito) {
            manejarExitoAutenticacion(user, cmd, cliente);
        } else {
            manejarFalloAutenticacion(cmd, cliente);
        }
    }

    private void manejarExitoAutenticacion(String user, String cmd, UnCliente cliente) throws IOException {
        if (!cliente.getNombreCliente().equals(user)) ServidorMulti.clientes.remove(cliente.getNombreCliente());
        cliente.setRegistrado(user);
        ServidorMulti.clientes.put(user, cliente);
        if (cmd.equals("/login")) BdGrupos.unirseAGrupo(user, "Todos");
        String msg = (cmd.equals("/login")) ? "¡Login exitoso!" : "¡Registro exitoso!";
        cliente.enviarMensaje("Sistema: " + msg + " Ahora eres un usuario registrado (" + user + ").");
    }

    private void manejarFalloAutenticacion(String cmd, UnCliente cliente) throws IOException {
        String errorMsg = (cmd.equals("/login")) ? "Error de login. Credenciales inválidas." : "Error de registro. El usuario ya existe.";
        cliente.enviarMensaje("Sistema: " + errorMsg);
    }

    private boolean manejarBloqueoDesbloqueo(String[] p, UnCliente cliente, String cmd) throws IOException {
        if (!cliente.isRegistrado()) {
            cliente.enviarMensaje("Sistema: Debes estar registrado y logueado para usar el comando " + cmd + ".");
            return true;
        }
        if (p.length != 2) {
            cliente.enviarMensaje("Sistema: Uso correcto: " + cmd + " nombre_usuario");
            return true;
        }
        String remitente = cliente.getNombreCliente();
        String objetivo = p[1];
        return ejecutarBloqueoDesbloqueo(remitente, objetivo, cmd, cliente);
    }

    private boolean ejecutarBloqueoDesbloqueo(String remitente, String objetivo, String cmd, UnCliente cliente) throws IOException {
        if (validarUsuarioObjetivo(objetivo, cliente)) return true;
        if (objetivo.equals(remitente)) {
            cliente.enviarMensaje("Sistema: No puedes bloquearte/desbloquearte a ti mismo.");
            return true;
        }
        if (cmd.equals("/bloquear")) return realizarBloqueo(remitente, objetivo, cliente);
        return realizarDesbloqueo(remitente, objetivo, cliente);
    }

    private boolean validarUsuarioObjetivo(String objetivo, UnCliente cliente) throws IOException {
        if (objetivo.length() < 4 || !objetivo.matches("^[a-zA-Z0-9]+$")) {
            cliente.enviarMensaje("Sistema: Error de formato. El nombre de usuario objetivo debe tener al menos 4 caracteres y solo contener letras y números.");
            return true;
        }
        return false;
    }

    private boolean realizarBloqueo(String remitente, String objetivo, UnCliente cliente) throws IOException {
        if (Bloqueos.bloquearUsuario(remitente, objetivo)) {
            cliente.enviarMensaje("Sistema: Has bloqueado a '" + objetivo + "'. Ya no recibirás sus mensajes públicos ni privados.");
        } else {
            cliente.enviarMensaje("Sistema: Error al bloquear a '" + objetivo + "'. El usuario no existe, ya estaba bloqueado o intentaste bloquearte a ti mismo.");
        }
        return true;
    }

    private boolean realizarDesbloqueo(String remitente, String objetivo, UnCliente cliente) throws IOException {
        if (Bloqueos.desbloquearUsuario(remitente, objetivo)) {
            cliente.enviarMensaje("Sistema: Has desbloqueado a '" + objetivo + "'. Ahora recibirás sus mensajes.");
        } else {
            cliente.enviarMensaje("Sistema: Error al desbloquear a '" + objetivo + "'. El usuario no estaba bloqueado o no existe.");
        }
        return true;
    }

    private boolean manejarRanking(UnCliente cliente) throws IOException {
        if (!cliente.isRegistrado()) {
            cliente.enviarMensaje("Sistema: Debes estar registrado y logueado para ver el ranking.");
            return true;
        }
        servicioRanking.mostrarRankingGeneral(cliente);
        return true;
    }

    private boolean manejarVs(String[] p, UnCliente cliente) throws IOException {
        if (!cliente.isRegistrado()) {
            cliente.enviarMensaje("Sistema: Debes estar registrado y logueado para ver estadísticas Head-to-Head.");
            return true;
        }
        if (p.length != 3) {
            cliente.enviarMensaje("Sistema: Uso correcto: /vs usuario1 usuario2");
            return true;
        }
        String usuario1 = p[1], usuario2 = p[2];
        if (usuario1.equals(usuario2)) {
            cliente.enviarMensaje("Sistema VS: No puedes ver estadisticas Head-to-Head del mismo usuario.");
            return true;
        }
        servicioRanking.mostrarEstadisticasVsArbitrarias(cliente, usuario1, usuario2);
        return true;
    }

    public ResultadoInicial procesarComandoInicial(String[] p) {
        String errorDefecto = "Error en el comando de inicio de sesion/registro o credenciales invalidas.";
        if (p.length != 3) return new ResultadoInicial(false, "", errorDefecto);

        String cmd = p[0], usuario = p[1], pass = p[2];
        String errorFormato = validarFormatoCredenciales(usuario, pass);
        if (errorFormato != null) return new ResultadoInicial(false, "", "Error de formato. " + errorFormato);

        if (cmd.equals("/login")) return intentarLoginInicial(usuario, pass);
        if (cmd.equals("/registrar")) return intentarRegistroInicial(usuario, pass);

        return new ResultadoInicial(false, "", errorDefecto);
    }

    private ResultadoInicial intentarLoginInicial(String usuario, String pass) {
        boolean exito = BdUsuario.verificarCredenciales(usuario, pass);
        if (exito) return new ResultadoInicial(true, usuario, null);

        String errorMsg = "Error de login. Credenciales invalidas para: " + usuario;
        return new ResultadoInicial(false, "", errorMsg);
    }

    private ResultadoInicial intentarRegistroInicial(String usuario, String pass) {
        boolean exito = BdUsuario.registrarUsuario(usuario, pass);
        if (exito) return new ResultadoInicial(true, usuario, null);

        String errorMsg = "Error de registro. El usuario " + usuario + " ya existe.";
        return new ResultadoInicial(false, "", errorMsg);
    }

    private static String validarNombreUsuarioProhibido(String user) {
        if (COMANDOS_PROHIBIDOS.contains(user.toLowerCase())) {
            return "El nombre de usuario no puede ser una palabra reservada o un comando del sistema.";
        }
        return null;
    }

    private static String validarFormatoCredenciales(String user, String pass) {
        if (user == null || pass == null || user.length() < 4 || pass.length() < 4) {
            return "El nombre de usuario y la contraseña deben tener al menos 4 caracteres.";
        }
        if (!user.matches("^[a-zA-Z0-9]+$")) {
            return "El nombre de usuario solo debe contener letras y números.";
        }
        String errorComando = validarNombreUsuarioProhibido(user);
        if (errorComando != null) {
            return errorComando;
        }
        return null;
    }
}