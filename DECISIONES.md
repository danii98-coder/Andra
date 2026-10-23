# Decisiones

Decisiones mínimas tomadas donde el prompt no definía algo, o donde se adaptó a la arquitectura pedida.

1. **Persistencia en SQLite (no JSON).** Por pedido del equipo, todo el estado vive en `andara.db` con JPA/Hibernate, igual que `andara-spring`. `datos-demo.json` es la semilla; el archivo se crea en la carpeta del proyecto (como en `andara-spring`) en lugar de `~/.andara/estado.json`.
2. **Fechas relativas.** Al sembrar, todas las fechas de `datos-demo.json` se desplazan `hoy − fechaReferencia` días; los textos con fechas `dd/mm/aaaa` de otros usuarios también. El préstamo activo vence a la hora actual + 2 h 15 min.
3. **Traducción.** Se carga `traducciones.json` directamente (sin `.properties`) con un traductor que replica el del prototipo: busca el texto exacto, luego con números y códigos como `{0}`, traduce fechas, separa por `·`, `:` o `,` y aplica prefijos. Todas las etiquetas están enlazadas a la propiedad de idioma, así cambian al instante (también modales y toasts). Si falta una traducción se muestra el castellano y se registra en el log.
4. **Documentos generados y comprobantes** se muestran siempre en castellano (oficiales). Los nombres de espacios y personas no se traducen.
5. **Tipografía.** En macOS se usa la fuente del sistema (SF); en Windows/Linux, Inter incluida. JavaFX no mapea el peso 600 de la fuente del sistema, por eso los títulos usan negrita.
6. **Vidrio.** La cabecera usa `VidrioDasm`: captura lo que hay detrás, lo desenfoca (28 px) y satura; se actualiza a ~30 fps y cae a solo translucidez si tarda más de 24 ms. Modales: el contenido de atrás se desenfoca 8 px con velo. Popovers, toasts y tarjetas usan translucidez sin captura.
7. **Popovers y modales** se dibujan dentro de la misma ventana (no ventanas emergentes) para poder aplicar vidrio, Esc y foco.
8. **Giro 360°.** `RotateTransition` en eje Y con `PerspectiveCamera` en la escena; la tarjeta no se mueve.
9. **Descargas.** Toda descarga abre "Guardar como" y genera un PDF con PDFBox y marca de agua "DEMOSTRACIÓN"; los documentos del catálogo generan un PDF de demostración.
10. **Contraseñas.** Se comparan contra huellas SHA-256 fijas en el código.
11. **"Reducir movimiento"** está en el menú de usuario (no hay pantalla de Ajustes aparte).
12. **Firma del carné:** 6 caracteres base 36 del SHA-256 de `código|franja|andara`.
13. **Ventanilla N** del seguimiento = 1 + hash del código del expediente mod 6.
14. **Ocupación simulada** de espacios: misma función hash del prototipo (nombre + día + franja), más las reservas reales.
15. **Nombres únicos del equipo.** Algunas clases y variables llevan las iniciales de los integrantes: `VidrioDasm` (Daniel Angel Soto Macedo), `TarjetaJemm` y `ordenJemm` (Jair Edison Mendoza Mamani), `OcupacionService.FranjaBnps` y `Graficos.mapaCalorBnps` (Brayan Nestor Puma Saldivar), `RastreadorCjsv`, `SeguimientoCjsv` y `SEMILLA_CJSV` (Carlos Josue Saldaña Valeriano).
16. **Sin comentarios** en ningún archivo de código, por pedido del equipo. Por eso no se incluye el `mvnw` (el script estándar trae comentarios); se usa Maven o IntelliJ.

## Cobertura (§4–§7)

| Función | Estado | Nota |
|---|---|---|
| §4.1 Ingreso estudiante/administrador, demo, error, sesión recordada, cerrar sesión | Hecho | |
| §4.2 Cabecera de vidrio: globo, campana, escuela, sede, usuario | Hecho | |
| §4.3 Barra 8/6 secciones, Ctrl/Cmd + 1…8 | Hecho | Desplazable si no cabe |
| §4.4 Pie con YouTube, Facebook, teléfono (modal) | Hecho | |
| Ventana 1280×800, mínima 1024×680, recuerda tamaño/posición/maximizado, título por sección | Hecho | |
| §5 Contexto sede/escuela, "Todas las sedes" para admin | Hecho | |
| §6.1 Inicio: carrusel, aviso Andara Pay, accesos rápidos, buscador con calendario | Hecho | |
| §6.2 Reservas: buscador, Galería/Mapa, pestañas, Magno destacado, giro, detalle, reserva, mis reservas | Hecho | |
| §6.3 Documentos: tarifario, buscador, rangos, filtros, orden, secciones, mis documentos | Hecho | |
| §6.4 Equipos: categorías, buscador, catálogo con giro, detalle, préstamo activo, historial | Hecho | |
| §6.5 Mis Solicitudes: filtros, pestañas, indicadores, alerta, rastreador, paginación, nueva solicitud | Hecho | |
| §6.6 Ayuda | Hecho | |
| §6.7 Favoritos | Hecho | |
| §6.8 Asistente de 4 pasos, reglas de recojo, documento generado + PDF | Hecho | |
| §6.9 Pantalla de pago: Yape (QR/código), Plin (QR/número), tarjeta (Luhn y marca) | Hecho | QR reales con ZXing |
| §6.10 Andara Pay: saldo de vidrio, matrícula en 3 pasos, derechos de pago, nuevo pago | Hecho | |
| §6.11 Componente de Filtros | Hecho | |
| §6.12 Carné digital: QR cada 15 s, anillo, lectura simulada, validación ±1 | Hecho | |
| §6.13 Mapa de ocupación en vivo | Hecho | Refresco cada 60 s |
| §6.14 Seguimiento tipo delivery, preferencias y mensajes | Hecho | |
| §6.15 Modo sin conexión: barra, cola, pagos bloqueados, sincronización | Hecho | Comprobación HEAD cada 15 s |
| §7.1 Panel | Hecho | |
| §7.2 Trámites con revisar/observar/emitir/entregar | Hecho | |
| §7.3 Transacciones con reembolso | Hecho | |
| §7.4 Reservas y §7.5 Préstamos | Hecho | |
| §7.6 Análisis: periodo, indicadores, conclusiones, barras, mapa de calor, validador | Hecho | |
| §8.1 Cuatro idiomas al instante | Hecho | Las opciones de los selectores se repintan al cambiar |
| Fechas del DatePicker estándar en otros idiomas | Parcial | El calendario emergente de JavaFX sigue en castellano; el calendario propio de Inicio sí se traduce |
