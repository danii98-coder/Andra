package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.unaj.andara.exception.NegocioException;
import pe.edu.unaj.andara.model.Ajustes;
import pe.edu.unaj.andara.model.EscuelaProfesional;
import pe.edu.unaj.andara.model.Sede;

@Service
@RequiredArgsConstructor
public class ContextoService {

    private final AjustesService ajustes;
    private final CatalogoService catalogo;

    public boolean todasLasSedes() {
        return Ajustes.TODAS.equals(ajustes.obtener().getSede());
    }

    public String codigoSede() {
        return ajustes.obtener().getSede();
    }

    public Sede sede() {
        String c = codigoSede();
        return Ajustes.TODAS.equals(c) ? null : catalogo.sede(c).orElse(null);
    }

    public Sede sedeOPrincipal() {
        Sede s = sede();
        return s != null ? s : catalogo.sede("capilla").orElseThrow();
    }

    public String nombreSede() {
        Sede s = sede();
        return s == null ? "Todas" : s.getNombre();
    }

    public String textoSede() {
        Sede s = sede();
        return s == null ? "Todas las sedes" : "Sede " + s.getNombre();
    }

    public EscuelaProfesional escuela() {
        return catalogo.escuela(ajustes.obtener().getEscuela()).orElse(null);
    }

    public String textoEscuela() {
        EscuelaProfesional e = escuela();
        return e == null ? "Todas las escuelas" : "EP " + e.getNombre();
    }

    public boolean incluyeSede(Sede sede) {
        String c = codigoSede();
        return Ajustes.TODAS.equals(c) || (sede != null && sede.getCodigo().equals(c));
    }

    public boolean incluyeEscuela(EscuelaProfesional escuelaRecurso) {
        String elegida = ajustes.obtener().getEscuela();
        return elegida == null || escuelaRecurso == null || escuelaRecurso.getCodigo().equals(elegida);
    }

    public boolean incluye(Sede sede, EscuelaProfesional escuelaRecurso) {
        return incluyeSede(sede) && incluyeEscuela(escuelaRecurso);
    }

    public static boolean pertenece(String sedeContexto, String escuelaContexto, Sede sede, EscuelaProfesional escuela) {
        boolean enSede = Ajustes.TODAS.equals(sedeContexto) || (sede != null && sede.getCodigo().equals(sedeContexto));
        boolean enEscuela = escuelaContexto == null || escuela == null || escuela.getCodigo().equals(escuelaContexto);
        return enSede && enEscuela;
    }

    public String cambiarSede(String codigo) {
        if (Ajustes.TODAS.equals(codigo)) {
            ajustes.cambiar(a -> {
                a.setSede(Ajustes.TODAS);
                a.setEscuela(null);
            });
            return "Mostrando información de todas las sedes.";
        }
        Sede s = catalogo.sede(codigo).orElseThrow(() -> new NegocioException("La sede no existe."));
        EscuelaProfesional actual = escuela();
        boolean limpia = actual != null && !actual.getSede().getCodigo().equals(codigo);
        ajustes.cambiar(a -> {
            a.setSede(codigo);
            if (limpia) {
                a.setEscuela(null);
            }
        });
        return "Mostrando información de la Sede " + s.getNombre() + (limpia ? " · todas las escuelas" : "") + ".";
    }

    public String cambiarEscuela(String codigo) {
        if (codigo == null) {
            ajustes.cambiar(a -> a.setEscuela(null));
            return "Mostrando todas las escuelas de la sede.";
        }
        EscuelaProfesional e = catalogo.escuela(codigo).orElseThrow(() -> new NegocioException("La escuela no existe."));
        ajustes.cambiar(a -> {
            a.setSede(e.getSede().getCodigo());
            a.setEscuela(e.getCodigo());
        });
        return "EP " + e.getNombre() + " · Sede " + e.getSede().getNombre() + ".";
    }

    public void asegurarSedeParaEstudiante(String sedePropia) {
        if (todasLasSedes()) {
            ajustes.cambiar(a -> {
                a.setSede(sedePropia);
                a.setEscuela(null);
            });
        }
    }
}
