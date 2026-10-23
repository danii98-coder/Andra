package pe.edu.unaj.andara.service;

import org.junit.jupiter.api.Test;
import pe.edu.unaj.andara.config.JsonRecursos;
import pe.edu.unaj.andara.model.*;
import pe.edu.unaj.andara.util.DiasHabiles;
import pe.edu.unaj.andara.util.TarjetaJemm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasTest {

    private static Tramite tramite(String entrega) {
        return Tramite.builder().codigo("t").nombre("Constancia de estudios").categoria("Constancias").monto(new BigDecimal("15"))
                .entrega(entrega).publicos("Estudiante|Egresado").plantilla("constancia").build();
    }

    @Test
    void luhnYMarcaDeTarjeta() {
        assertThat(TarjetaJemm.luhn("4111 1111 1111 1111")).isTrue();
        assertThat(TarjetaJemm.luhn("4111 1111 1111 1112")).isFalse();
        assertThat(TarjetaJemm.marca("4111")).isEqualTo(MetodoPago.VISA);
        assertThat(TarjetaJemm.marca("5500 0000")).isEqualTo(MetodoPago.MASTERCARD);
        assertThat(TarjetaJemm.marca("3782 822463")).isEqualTo(MetodoPago.AMEX);
        assertThat(TarjetaJemm.marca("3056 9309")).isEqualTo(MetodoPago.DINERS);
        assertThat(TarjetaJemm.marca("6011")).isNull();
        assertThat(TarjetaJemm.formatear("4111111111111111")).isEqualTo("4111 1111 1111 1111");
        assertThat(TarjetaJemm.vencimientoValido("12/30", YearMonth.of(2026, 10))).isTrue();
        assertThat(TarjetaJemm.vencimientoValido("09/26", YearMonth.of(2026, 10))).isFalse();
        assertThat(PagoService.validarTarjeta("4111 1111 1111 1112", "A", "12/30", "123", "a@b.pe", YearMonth.of(2026, 10)))
                .containsEntry("numero", "El número de tarjeta no es válido. Revísalo.");
    }

    @Test
    void validacionesDeYape() {
        assertThat(PagoService.validarYape("951234567", "123456")).isEmpty();
        assertThat(PagoService.validarYape("851234567", "123456")).containsEntry("celular", "Celular de 9 dígitos que empiece con 9.");
        assertThat(PagoService.validarYape("951234567", "12345")).containsEntry("codigo", "El código tiene 6 dígitos.");
        assertThat(PagoService.validarYape("", "")).containsKeys("celular", "codigo");
    }

    @Test
    void recojoConDiasHabilesYCorteDeLasQuince() {
        LocalDate martes = LocalDate.of(2026, 10, 20);
        PagoService.Recojo antes = PagoService.recojo(tramite("mismo"), martes.atTime(14, 59));
        assertThat(antes.fecha()).isEqualTo(martes);
        assertThat(antes.texto()).startsWith("Hoy, ").endsWith("desde las 16:00");
        PagoService.Recojo despues = PagoService.recojo(tramite("mismo"), martes.atTime(15, 0));
        assertThat(despues.fecha()).isEqualTo(martes.plusDays(1));
        assertThat(despues.texto()).endsWith("desde las 09:00");
        LocalDate viernes = LocalDate.of(2026, 10, 23);
        assertThat(PagoService.recojo(tramite("mismo"), viernes.atTime(16, 0)).fecha()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(PagoService.recojo(tramite("mismo"), LocalDate.of(2026, 10, 24).atTime(9, 0)).fecha()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(PagoService.recojo(tramite("3"), viernes.atTime(9, 0)).fecha()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(PagoService.recojo(tramite("carne"), martes.atTime(9, 0)).fecha()).isEqualTo(DiasHabiles.sumar(martes, 10));
        assertThat(PagoService.recojo(tramite("inmediato"), martes.atTime(9, 0)).fecha()).isNull();
        assertThat(DiasHabiles.sumar(martes, 15)).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void totalesDeOrdenes() {
        Tramite silabos = tramite("mismo");
        silabos.setPorCantidad(true);
        silabos.setMonto(new BigDecimal("5"));
        List<ItemPago> items = PagoService.itemsPara(silabos, 3);
        assertThat(items.get(0).getConcepto()).endsWith("(3 cursos)");
        assertThat(PagoService.totalOrden(items)).isEqualByComparingTo("15");
        assertThat(PagoService.itemsPara(silabos, 50).get(0).getCantidad()).isEqualTo(20);
        Pago p = Pago.builder().items(List.of(new ItemPago("A", 1, new BigDecimal("60")), new ItemPago("B", 1, new BigDecimal("20")))).build();
        assertThat(p.getTotal()).isEqualByComparingTo("80");
    }

    @Test
    void carneCambiaCadaQuinceSegundosYValidaFranjaYFirma() {
        long t = 1_790_000_000_000L;
        long f = CarneService.franja(t);
        assertThat(CarneService.franja(t + 15000)).isEqualTo(f + 1);
        assertThat(CarneService.token("2024207009", f)).isNotEqualTo(CarneService.token("2024207009", f + 1));
        assertThat(CarneService.firma("2024207009", f)).hasSize(6).matches("[0-9A-Z]{6}");
        String token = CarneService.token("2024207009", f);
        Matcher m = Pattern.compile("^ANDARA-ID\\|(\\w+)\\|(\\d+)\\|(\\w+)$").matcher(token);
        assertThat(m.matches()).isTrue();
        String alterado = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");
        assertThat(CarneService.firma("2024207009", f)).isNotEqualTo(alterado.substring(alterado.lastIndexOf('|') + 1));
        assertThat(CarneService.segundosRestantes(f * 15000)).isEqualTo(15);
    }

    @Test
    void reglasDeContexto() {
        Sede capilla = Sede.builder().codigo("capilla").build();
        Sede ayabacas = Sede.builder().codigo("ayabacas").build();
        EscuelaProfesional iss = EscuelaProfesional.builder().codigo("iss").sede(capilla).build();
        EscuelaProfesional imc = EscuelaProfesional.builder().codigo("imc").sede(capilla).build();
        assertThat(ContextoService.pertenece("capilla", null, capilla, iss)).isTrue();
        assertThat(ContextoService.pertenece("capilla", "iss", capilla, null)).isTrue();
        assertThat(ContextoService.pertenece("capilla", "iss", capilla, iss)).isTrue();
        assertThat(ContextoService.pertenece("capilla", "iss", capilla, imc)).isFalse();
        assertThat(ContextoService.pertenece("capilla", null, ayabacas, null)).isFalse();
        assertThat(ContextoService.pertenece("all", null, ayabacas, null)).isTrue();
    }

    @Test
    void filtrosDeDocumentosYRangos() {
        EscuelaProfesional iss = EscuelaProfesional.builder().codigo("iss").nombre("Ingeniería de Software y Sistemas").build();
        Documento a = Documento.builder().codigo("a").nombre("Reglamento de matrícula").descripcion("Normas").tipo("Reglamentos")
                .dependencia("Vicerrectorado Académico").publico("Estudiantes").fecha(LocalDate.of(2026, 1, 20)).build();
        Documento b = Documento.builder().codigo("b").nombre("Sílabo Algoritmos").descripcion("Contenidos").tipo("Sílabos")
                .dependencia("Ingeniería de Software y Sistemas").publico("Estudiantes").fecha(LocalDate.of(2025, 8, 3)).escuela(iss).build();
        LocalDate hoy = LocalDate.of(2026, 10, 21);
        List<Documento> todos = List.of(a, b);
        assertThat(DocumentoService.filtrar(todos, new DocumentoService.Criterio("silabo", "", "", "", "any", null, null), null, hoy)).containsExactly(b);
        assertThat(DocumentoService.filtrar(todos, new DocumentoService.Criterio("", "", "", "", "year", null, null), null, hoy)).containsExactly(a);
        assertThat(DocumentoService.filtrar(todos, new DocumentoService.Criterio("", "", "", "", "2025", null, null), null, hoy)).containsExactly(b);
        assertThat(DocumentoService.filtrar(todos, new DocumentoService.Criterio("", "", "", "", "custom", LocalDate.of(2025, 8, 1),
                LocalDate.of(2025, 8, 5)), null, hoy)).containsExactly(b);
        assertThat(DocumentoService.validarRangoPersonalizado(new DocumentoService.Criterio("", "", "", "", "custom",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1)))).isEqualTo("La fecha final debe ser posterior a la inicial.");
        assertThat(DocumentoService.ordenar(todos, "old", false, java.util.Set.of())).containsExactly(b, a);
    }

    @Test
    void filtrosAvanzadosDelAdministrador() {
        record Fila(LocalDate fecha, String codigo, String escuela, String tipo) {
        }
        List<Fila> filas = List.of(new Fila(LocalDate.of(2026, 10, 1), "2024207009 72458913", "iss", "Estudiante"),
                new Fila(LocalDate.of(2026, 10, 15), "D-10452 40125874", "gpd", "Docente"));
        EstadoFiltro e = new EstadoFiltro();
        e.getAvanzados().put("from", "2026-10-10");
        FiltroService.Accesores<Fila> acc = new FiltroService.Accesores<>(Fila::fecha, Fila::codigo,
                java.util.Map.of("fac", Fila::escuela, "tipo", Fila::tipo));
        assertThat(FiltroService.aplicarAvanzado(filas, e, acc)).extracting(Fila::tipo).containsExactly("Docente");
        e.limpiar();
        e.getAvanzados().put("code", "72458913");
        assertThat(FiltroService.aplicarAvanzado(filas, e, acc)).hasSize(1);
        e.limpiar();
        e.getAvanzados().put("fac", "gpd");
        e.getAvanzados().put("tipo", "Docente");
        assertThat(FiltroService.aplicarAvanzado(filas, e, acc)).hasSize(1);
        assertThat(e.cantidad()).isEqualTo(2);
        assertThat(FiltroService.validarRango("2026-10-10", "2026-10-01")).isEqualTo("La fecha “Hasta” debe ser posterior a “Desde”.");
    }

    @Test
    void permisosPorRol() {
        assertThat(SesionService.puedeAcceder(Rol.ESTUDIANTE, "admin")).isFalse();
        assertThat(SesionService.puedeAcceder(Rol.ESTUDIANTE, "a-datos")).isFalse();
        assertThat(SesionService.puedeAcceder(Rol.ESTUDIANTE, "carne")).isTrue();
        assertThat(SesionService.puedeAcceder(Rol.ADMINISTRADOR, "admin")).isTrue();
        assertThat(SesionService.puedeAcceder(Rol.ADMINISTRADOR, "pay")).isFalse();
        assertThat(SesionService.puedeAcceder(null, "inicio")).isFalse();
        assertThat(SesionService.rutas(Rol.ESTUDIANTE)).hasSize(8);
        assertThat(SesionService.rutas(Rol.ADMINISTRADOR)).hasSize(6);
    }

    private static Expediente expediente(ClaseExpediente clase, GrupoExpediente grupo, Pago pago, String estado) {
        Expediente e = Expediente.builder().codigo("SOL-2026-000001").clase(clase).grupo(grupo).pago(pago).estado(estado).paso(1)
                .sede(Sede.builder().nombre("Capilla").build()).fecha(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
        return e;
    }

    @Test
    void etapasDeSeguimientoParaCadaTipo() {
        Pago pendiente = Pago.builder().estado(EstadoPago.PENDIENTE).build();
        SeguimientoService.SeguimientoCjsv a = SeguimientoService.calcular(expediente(ClaseExpediente.DOC, GrupoExpediente.TRAMITE, pendiente, "Pago pendiente"));
        assertThat(a.etapas()).hasSize(6);
        assertThat(a.actual()).isEqualTo(1);
        assertThat(a.estimado()).isEqualTo("Esperando tu pago en Andara Pay");
        Pago pagado = Pago.builder().estado(EstadoPago.PAGADO).pagado(LocalDateTime.of(2026, 10, 20, 10, 0)).build();
        assertThat(SeguimientoService.calcular(expediente(ClaseExpediente.DOC, GrupoExpediente.LISTO, pagado, "Listo")).actual()).isEqualTo(4);
        assertThat(SeguimientoService.calcular(expediente(ClaseExpediente.DOC, GrupoExpediente.LISTO, pagado, "Entregado al solicitante")).estimado()).isEqualTo("Entregado");
        SeguimientoService.SeguimientoCjsv sin = SeguimientoService.calcular(expediente(ClaseExpediente.DOC, GrupoExpediente.TRAMITE, null, "En revisión"));
        assertThat(sin.etapas()).extracting(SeguimientoService.Etapa::titulo).containsExactly("Recibido", "En revisión", "En firma",
                "Listo en ventanilla " + sin.ventanilla(), "Entregado");
        assertThat(sin.estimado()).isEqualTo("Estimado: 2 a 3 días hábiles");
        SeguimientoService.SeguimientoCjsv obs = SeguimientoService.calcular(expediente(ClaseExpediente.DOC, GrupoExpediente.OBSERVADO, null, "Observado"));
        assertThat(obs.observado()).isTrue();
        assertThat(obs.estimado()).isEqualTo("Detenido: tienes una observación por subsanar");
        SeguimientoService.SeguimientoCjsv lab = SeguimientoService.calcular(expediente(ClaseExpediente.LAB, GrupoExpediente.LISTO, null, "Confirmado"));
        assertThat(lab.etapas()).extracting(SeguimientoService.Etapa::titulo).containsExactly("Solicitado", "Aprobado", "Listo para usar", "Finalizado");
        assertThat(lab.actual()).isEqualTo(2);
        assertThat(SeguimientoService.calcular(expediente(ClaseExpediente.EQ, GrupoExpediente.ANULADO, null, "Cancelado")).actual()).isZero();
        assertThat(SeguimientoService.ventanilla("SOL-2026-084920")).isBetween(1, 6).isEqualTo(SeguimientoService.ventanilla("SOL-2026-084920"));
    }

    @Test
    void mapaDeCalorYCeldaPico() {
        int[][] calor = AnaliticaService.mapaCalor(List.of());
        assertThat(calor).hasDimensions(5, 5);
        int[] pico = AnaliticaService.pico(calor);
        int max = 0;
        for (int[] fila : calor) {
            for (int v : fila) {
                max = Math.max(max, v);
            }
        }
        assertThat(calor[pico[0]][pico[1]]).isEqualTo(max);
        assertThat(AnaliticaService.nivel(max, max)).isEqualTo(4);
        assertThat(AnaliticaService.nivel(0, max)).isZero();
        Pago lunes = Pago.builder().creado(LocalDateTime.of(2026, 10, 19, 10, 30)).build();
        assertThat(AnaliticaService.mapaCalor(List.of(lunes))[0][1]).isEqualTo(calor[0][1] + 3);
    }

    @Test
    void franjaDelMapaEnVivo() {
        assertThat(OcupacionService.franjaPara(LocalDateTime.of(2026, 10, 21, 10, 30))).isEqualTo(new OcupacionService.FranjaBnps(1, 0, true));
        assertThat(OcupacionService.franjaPara(LocalDateTime.of(2026, 10, 21, 19, 0))).isEqualTo(new OcupacionService.FranjaBnps(0, 1, false));
        assertThat(OcupacionService.franjaPara(LocalDateTime.of(2026, 10, 24, 10, 0)).enVivo()).isFalse();
        assertThat(OcupacionService.nombreCorto("Laboratorio de Suelos y Calidad Ambiental")).isEqualTo("Suelos y Calidad Ambiental");
    }

    @Test
    void idiomasConservanMarcadoresYCaenAlCastellano() {
        IdiomaService idioma = new IdiomaService(new JsonRecursos());
        idioma.cargar();
        assertThat(idioma.totalTextos()).isGreaterThan(1500);
        Pattern marca = Pattern.compile("\\{\\d+}");
        for (String clave : idioma.claves()) {
            List<String> esperadas = marca.matcher(clave).results().map(r -> r.group()).sorted().toList();
            for (String l : List.of("en", "qu", "ay")) {
                String t = idioma.directa(clave, l);
                assertThat(t).as(l + ": " + clave).isNotBlank();
                assertThat(marca.matcher(t).results().map(r -> r.group()).sorted().toList()).as(l + ": " + clave).isEqualTo(esperadas);
            }
        }
        for (String s : List.of("Inicio", "Documentos", "Reservas", "Equipos", "Mis Solicitudes", "Ayuda", "Favoritos", "Panel", "Análisis",
                "Trámites", "Transacciones", "Préstamos", "Notificaciones", "Idioma", "Cerrar sesión", "Ingresa a tu cuenta", "Mi carné digital",
                "Seguimiento en vivo", "Buscar disponibilidad", "Filtros rápidos", "Lo que dicen los datos")) {
            for (String l : List.of("en", "qu", "ay")) {
                assertThat(idioma.traducir(s, l)).as(l + ": " + s).isNotEqualTo(s);
            }
        }
        assertThat(idioma.traducir("Tienes 3 pagos pendientes, incluida tu matrícula 2026-2.", "es")).startsWith("Tienes 3");
        assertThat(idioma.traducir("Se renueva en 7 s", "en")).isEqualTo("Renews in 7 s");
        assertThat(idioma.traducir("Texto inexistente para la prueba", "en")).isEqualTo("Texto inexistente para la prueba");
        assertThat(idioma.traducir("Martes, 20 de octubre, 2026", "en")).isEqualTo("Tuesday, October 20, 2026");
        assertThat(idioma.traducir("Martes, 20 de octubre, 2026", "qu")).endsWith("20 octubre killa, 2026");
        assertThat(idioma.traducir("Martes, 20 de octubre, 2026", "ay")).contains("octubre phaxsi");
    }
}
