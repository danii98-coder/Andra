package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.Administrador;
import pe.edu.unaj.andara.model.Persona;
import pe.edu.unaj.andara.model.Rol;
import pe.edu.unaj.andara.repository.AdministradorRepository;
import pe.edu.unaj.andara.repository.PersonaRepository;
import pe.edu.unaj.andara.util.Fechas;
import pe.edu.unaj.andara.util.Hash;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SesionService {

    public record Ruta(String clave, String etiqueta) {
    }

    public static final List<Ruta> RUTAS_ESTUDIANTE = List.of(new Ruta("inicio", "Inicio"),
            new Ruta("documentos", "Documentos"), new Ruta("reservas", "Reservas"), new Ruta("equipos", "Equipos"),
            new Ruta("solicitudes", "Mis Solicitudes"), new Ruta("pay", "Andara Pay"), new Ruta("ayuda", "Ayuda"),
            new Ruta("favoritos", "Favoritos"));
    public static final List<Ruta> RUTAS_ADMIN = List.of(new Ruta("admin", "Panel"), new Ruta("a-datos", "Análisis"),
            new Ruta("a-tramites", "Trámites"), new Ruta("a-pagos", "Transacciones"), new Ruta("a-reservas", "Reservas"),
            new Ruta("a-prestamos", "Préstamos"));

    private static final String HUELLA_ESTUDIANTE_DASM = "9362e25906ba1571f3a48703fe792967a828c41ada755267a7e44b762bc7648c";
    private static final String HUELLA_ADMIN_DASM = "6051fc84a7a0d74c225fb18a496b09952da5642e60723ecae543298edd7d82d6";

    private final AjustesService ajustes;
    private final PersonaRepository personas;
    private final AdministradorRepository administradores;
    private final ContextoService contexto;

    public Rol rol() {
        return ajustes.obtener().getSesion();
    }

    public boolean conectado() {
        return rol() != null;
    }

    public boolean esAdministrador() {
        return rol() == Rol.ADMINISTRADOR;
    }

    public Persona estudiante() {
        return personas.findFirstByPrincipalTrue().orElseThrow(() -> new NegocioException("No hay estudiante registrado."));
    }

    public Administrador administrador() {
        return administradores.findFirstByOrderByIdAsc().orElseThrow(() -> new NegocioException("No hay administrador."));
    }

    public Rol ingresar(Rol rol, String usuario, String contrasena) {
        String u = usuario == null ? "" : usuario.trim().toLowerCase();
        String huella = Hash.sha256Hex(contrasena == null ? "" : contrasena);
        if (rol == Rol.ESTUDIANTE) {
            Persona p = estudiante();
            boolean coincide = (u.equals(p.getCodigo()) || u.equals(p.getDni())) && HUELLA_ESTUDIANTE_DASM.equals(huella);
            if (!coincide) {
                throw new NegocioException("El código, DNI o contraseña no coinciden. Revisa los datos o usa la cuenta de demostración.");
            }
            contexto.asegurarSedeParaEstudiante(p.getSede().getCodigo());
        } else {
            Administrador a = administrador();
            boolean coincide = u.equals(a.getCorreo().toLowerCase()) && HUELLA_ADMIN_DASM.equals(huella);
            if (!coincide) {
                throw new NegocioException("El correo o la contraseña no coinciden. Revisa los datos o usa la cuenta de demostración.");
            }
        }
        ajustes.cambiar(a -> {
            a.setSesion(rol);
            a.setSesionDesde(Fechas.ahora());
        });
        return rol;
    }

    public void cerrar() {
        ajustes.cambiar(a -> {
            a.setSesion(null);
            a.setSesionDesde(null);
        });
    }

    public static List<Ruta> rutas(Rol rol) {
        return rol == Rol.ADMINISTRADOR ? RUTAS_ADMIN : RUTAS_ESTUDIANTE;
    }

    public static boolean puedeAcceder(Rol rol, String ruta) {
        if (rol == null) {
            return "login".equals(ruta);
        }
        if (rol == Rol.ESTUDIANTE && "carne".equals(ruta)) {
            return true;
        }
        return rutas(rol).stream().anyMatch(r -> r.clave().equals(ruta));
    }

    public static String rutaInicial(Rol rol) {
        return rol == null ? "login" : rutas(rol).get(0).clave();
    }

    public static String etiqueta(String ruta) {
        if ("carne".equals(ruta)) {
            return "Mi carné";
        }
        return java.util.stream.Stream.concat(RUTAS_ESTUDIANTE.stream(), RUTAS_ADMIN.stream())
                .filter(r -> r.clave().equals(ruta)).map(Ruta::etiqueta).findFirst().orElse("");
    }
}
