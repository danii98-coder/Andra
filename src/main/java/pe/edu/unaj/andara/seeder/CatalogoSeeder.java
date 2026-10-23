package pe.edu.unaj.andara.seeder;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unaj.andara.config.JsonRecursos;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogoSeeder {

    private final JsonRecursos json;
    private final SedeRepository sedes;
    private final EscuelaProfesionalRepository escuelas;
    private final EspacioRepository espacios;
    private final DocumentoRepository documentos;
    private final CategoriaEquipoRepository categorias;
    private final EquipoRepository equipos;
    private final TramiteRepository tramites;
    private final PersonaRepository personas;
    private final AdministradorRepository administradores;

    @Transactional
    public int sembrar() {
        JsonNode c = json.catalogo();
        int nuevos = 0;
        for (JsonNode s : c.path("sedes")) {
            if (sedes.findByCodigo(s.path("id").asText()).isEmpty()) {
                sedes.save(Sede.builder().codigo(s.path("id").asText()).nombre(s.path("name").asText())
                        .nombreCompleto(s.path("full").asText()).descripcion(s.path("desc").asText())
                        .oficina(s.path("office").asText()).horario(s.path("hours").asText()).build());
                nuevos++;
            }
        }
        for (JsonNode e : c.path("escuelas")) {
            if (escuelas.findByCodigo(e.path("id").asText()).isEmpty()) {
                escuelas.save(EscuelaProfesional.builder().codigo(e.path("id").asText()).nombre(e.path("name").asText())
                        .sede(sede(e.path("sede").asText())).build());
                nuevos++;
            }
        }
        for (JsonNode e : c.path("espacios")) {
            if (espacios.findByCodigo(e.path("id").asText()).isEmpty()) {
                espacios.save(Espacio.builder().codigo(e.path("id").asText()).nombre(e.path("name").asText())
                        .tipo(TipoEspacio.desdeEtiqueta(e.path("type").asText())).capacidad(e.path("cap").asInt())
                        .fueraDeServicio("Ocupado".equals(e.path("status").asText()))
                        .edificio(e.path("bld").asText()).sede(sede(e.path("sede").asText()))
                        .escuela(escuela(e.path("fac").asText(null))).build());
                nuevos++;
            }
        }
        for (JsonNode d : c.path("documentos")) {
            if (documentos.findByCodigo(d.path("id").asText()).isEmpty()) {
                documentos.save(Documento.builder().codigo(d.path("id").asText()).nombre(d.path("name").asText())
                        .descripcion(d.path("desc").asText()).tipo(d.path("type").asText())
                        .dependencia(d.path("dep").asText()).publico(d.path("aud").asText())
                        .fecha(LocalDate.parse(d.path("date").asText())).frecuente(d.path("freq").asBoolean())
                        .paginas(d.path("pages").asInt(1)).escuela(escuela(d.path("fac").asText(null))).build());
                nuevos++;
            }
        }
        for (JsonNode k : c.path("categoriasEquipo")) {
            if (categorias.findByCodigo(k.path("id").asText()).isEmpty()) {
                categorias.save(CategoriaEquipo.builder().codigo(k.path("id").asText()).nombre(k.path("name").asText())
                        .descripcion(k.path("desc").asText()).icono(k.path("icon").asText()).build());
                nuevos++;
            }
        }
        for (JsonNode e : c.path("equipos")) {
            if (equipos.findByCodigo(e.path("id").asText()).isEmpty()) {
                equipos.save(Equipo.builder().codigo(e.path("id").asText())
                        .categoria(categorias.findByCodigo(e.path("cat").asText()).orElseThrow())
                        .rotulo(e.path("label").asText()).nombre(e.path("name").asText())
                        .especificaciones(e.path("specs").asText()).ubicacion(e.path("loc").asText())
                        .puntoEntrega(e.path("point").asText()).sede(sede(e.path("sede").asText()))
                        .escuela(escuela(e.path("fac").asText(null))).unidadesIniciales(e.path("units").asInt())
                        .stock(e.path("units").asInt()).unidad(e.path("unit").asText())
                        .modo("turno".equals(e.path("mode").asText()) ? ModoEquipo.TURNO : ModoEquipo.PRESTAMO)
                        .proximoTurno(e.path("next").asText(null)).build());
                nuevos++;
            }
        }
        for (JsonNode t : c.path("tarifario")) {
            if (tramites.findByCodigo(t.path("id").asText()).isEmpty()) {
                List<String> aud = new ArrayList<>();
                t.path("aud").forEach(a -> aud.add(a.asText()));
                tramites.save(Tramite.builder().codigo(t.path("id").asText()).nombre(t.path("name").asText())
                        .categoria(t.path("cat").asText()).monto(new BigDecimal(t.path("monto").asText()))
                        .entrega(t.path("entrega").asText()).publicos(String.join("|", aud))
                        .plantilla(t.path("tpl").isNull() ? null : t.path("tpl").asText(null))
                        .descripcion(t.path("desc").asText()).unidad(t.path("unidad").asText(null))
                        .porCantidad(t.path("qty").asBoolean(false)).build());
                nuevos++;
            }
        }
        JsonNode u = json.demo().path("usuario");
        if (personas.findByCodigo(u.path("codigo").asText()).isEmpty()) {
            personas.save(Persona.builder().codigo(u.path("codigo").asText()).nombre(u.path("nombre").asText())
                    .dni(u.path("dni").asText()).tipo("Estudiante").escuela(escuela(u.path("escuela").asText()))
                    .sede(sede(u.path("sede").asText())).correo(u.path("correo").asText())
                    .celular(u.path("celular").asText()).principal(true).build());
            nuevos++;
        }
        for (JsonNode p : c.path("usuarios")) {
            String codigo = p.path("code").asText();
            if (personas.findByCodigo(codigo).isEmpty()) {
                personas.save(Persona.builder().codigo(codigo).nombre(p.path("name").asText())
                        .dni(p.path("dni").asText()).tipo(p.path("tipo").asText())
                        .escuela(escuela(p.path("fac").asText(null))).sede(sede(p.path("sede").asText()))
                        .correo(codigo.toLowerCase() + "@unaj.edu.pe").principal(false).build());
                nuevos++;
            }
        }
        JsonNode a = json.demo().path("cuentasDemo").path("administrador");
        if (administradores.findByCorreo(a.path("correo").asText()).isEmpty()) {
            administradores.save(Administrador.builder().nombre(a.path("nombre").asText())
                    .correo(a.path("correo").asText()).cargo(a.path("cargo").asText()).dni("41236587").build());
            nuevos++;
        }
        return nuevos;
    }

    private Sede sede(String codigo) {
        return sedes.findByCodigo(codigo).orElseThrow(() -> new IllegalStateException("Sede " + codigo));
    }

    private EscuelaProfesional escuela(String codigo) {
        if (codigo == null || codigo.isBlank() || "null".equals(codigo)) {
            return null;
        }
        return escuelas.findByCodigo(codigo).orElse(null);
    }
}
