package pe.edu.unaj.andara.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.config.JsonRecursos;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    public static final List<String> FRANJAS = List.of("08:00", "10:00", "12:00", "14:00", "16:00");
    public static final List<String> HORARIOS = List.of("08:00 - 10:00", "10:00 - 12:00", "12:00 - 14:00",
            "14:00 - 16:00", "16:00 - 18:00");
    public static final List<String> SOLICITANTES = List.of("Estudiante", "Egresado", "Docente", "Administrativo",
            "Público en general");

    private final SedeRepository sedes;
    private final EscuelaProfesionalRepository escuelas;
    private final EspacioRepository espacios;
    private final DocumentoRepository documentos;
    private final CategoriaEquipoRepository categorias;
    private final EquipoRepository equipos;
    private final TramiteRepository tramites;
    private final PersonaRepository personas;
    private final JsonRecursos json;

    public List<Sede> sedes() {
        return sedes.findAllByOrderByIdAsc();
    }

    public Optional<Sede> sede(String codigo) {
        return sedes.findByCodigo(codigo);
    }

    public List<EscuelaProfesional> escuelas() {
        return escuelas.findAllByOrderByIdAsc();
    }

    public List<EscuelaProfesional> escuelasDe(Sede sede) {
        return escuelas().stream().filter(e -> e.getSede().getId().equals(sede.getId())).toList();
    }

    public Optional<EscuelaProfesional> escuela(String codigo) {
        return codigo == null ? Optional.empty() : escuelas.findByCodigo(codigo);
    }

    public String nombreEscuela(String codigo) {
        return escuela(codigo).map(EscuelaProfesional::getNombre).orElse("Uso general");
    }

    public List<Espacio> espacios() {
        return espacios.findAllByOrderByIdAsc();
    }

    public Optional<Espacio> espacio(String codigo) {
        return espacios.findByCodigo(codigo);
    }

    public List<Documento> documentos() {
        return documentos.findAllByOrderByIdAsc();
    }

    public List<CategoriaEquipo> categorias() {
        return categorias.findAllByOrderByIdAsc();
    }

    public List<Equipo> equipos() {
        return equipos.findAllByOrderByIdAsc();
    }

    public Optional<Equipo> equipo(String codigo) {
        return equipos.findByCodigo(codigo);
    }

    public List<Tramite> tramites() {
        return tramites.findAllByOrderByIdAsc();
    }

    public Optional<Tramite> tramite(String codigo) {
        return tramites.findByCodigo(codigo);
    }

    public List<Persona> personas() {
        return personas.findAllByOrderByIdAsc();
    }

    public Optional<Persona> persona(String codigo) {
        return personas.findByCodigo(codigo);
    }

    public Optional<Persona> personaPorCodigoODni(String valor) {
        String v = valor == null ? "" : valor.trim();
        return personas.findByCodigo(v).or(() -> personas.findFirstByDni(v));
    }

    public List<String> tiposDocumento() {
        List<String> l = new ArrayList<>();
        json.catalogo().path("tiposDocumento").forEach(t -> l.add(t.asText()));
        return l;
    }

    public List<String> dependencias() {
        List<String> l = new ArrayList<>();
        json.catalogo().path("dependenciasAdministrativas").forEach(t -> l.add(t.asText()));
        return l;
    }

    public String etiquetaDependencia(String dependencia) {
        return escuelas().stream().anyMatch(e -> e.getNombre().equals(dependencia))
                ? "Escuela Profesional de " + dependencia : dependencia;
    }
}
