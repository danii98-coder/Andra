package pe.edu.unaj.andara.controller;

import pe.edu.unaj.andara.model.Persona;
import pe.edu.unaj.andara.service.CatalogoService;
import pe.edu.unaj.andara.ui.Filtros;
import pe.edu.unaj.andara.ui.Opcion;

import java.util.ArrayList;
import java.util.List;

final class AdminFiltros {

    private AdminFiltros() {
    }

    static List<Opcion> escuelas(CatalogoService catalogo) {
        List<Opcion> l = new ArrayList<>();
        catalogo.escuelas().forEach(e -> l.add(Opcion.de(e.getCodigo(), e.getNombre())));
        return l;
    }

    static List<Filtros.Avanzado> persona(CatalogoService catalogo, String etiquetaCodigo, String ejemplo) {
        return new ArrayList<>(List.of(Filtros.Avanzado.texto("code", etiquetaCodigo, ejemplo),
                Filtros.Avanzado.selector("fac", "Escuela profesional", "Todas las escuelas", escuelas(catalogo)),
                Filtros.Avanzado.selector("tipo", "Tipo de solicitante", "Todos", Opcion.lista(CatalogoService.SOLICITANTES))));
    }

    static List<Filtros.Avanzado> fechas(String desde, String hasta) {
        return new ArrayList<>(List.of(Filtros.Avanzado.fecha("from", desde), Filtros.Avanzado.fecha("to", hasta)));
    }

    static String clave(Persona p) {
        return p.getCodigo() + " " + (p.getDni() == null ? "" : p.getDni()) + " " + p.getNombre();
    }

    static String escuela(Persona p) {
        return p.getEscuela() == null ? null : p.getEscuela().getCodigo();
    }
}
