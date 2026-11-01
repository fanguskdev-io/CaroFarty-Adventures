---
harness_version: 1.15.0                # la que declara scoring/*.yaml del arnés; su «Sin publicar» está vacía (commit e2f6f8f5, 2026-10-31)
harness_path: ../mayeutic-harness      # relativa a la raíz de este repositorio
proyecto: CaroFarty
estado: activo
talla: sin declarar                    # la decide la persona; su ronda está en cola en entrevistas/one-pager-pending.md
fase: P0
proxima_puerta: G0
decisor: Fangusk                       # medido: dicta el juego y contesta sus decisiones en la sesión del 2026-11-01 (docs/diseno.md, docs/adr/README.md); ninguna señal de que decida otra persona (docs/05 §2)
fecha_revision:                        # vacía: todavía no hay acta, ni criterio de muerte con fecha, ni riesgo aceptado
actualizado: 2026-11-01
---

# Estado · CaroFarty

## Dónde estamos

P0, la chispa. El 2026-11-01 Fangusk pidió integrar CaroFarty con el arnés «al igual que has hecho con todos los desarrollos
anteriores», cuando ya existían las especificaciones que él dictó, la página de diseño y los ADR de M0
([`docs/diseno.md`](../diseno.md), [`docs/adr/`](../adr/README.md)). Se adopta como en BakiFace: el arnés sirve al desarrollo
(ADR con estado, puerta de calidad y CI `mayeutic`) y las rondas de descubrimiento se quedan escritas en su cola, sin
abrir, porque el decisor dijo en BakiFace que no son relevantes y que él expone lo que tiene que hacer la app. Sin la vez
concreta del problema ni criterios de muerte, G0 no se puede revisar y el proyecto sigue en P0. A la vez, el agente
construye el juego como **borrador anticipado** (docs/07 §1 del arnés): el código y los ADR existen antes de G4, los
revisan las puertas que los piden y no deciden la talla, los criterios de muerte ni ninguna salida. El trabajo se ordena
en el backlog compartido, `../backlog-juego-plataformas` (de la sesión GameMaster); las condiciones de una puerta irían
allí como tareas (docs/05 §4).

**Siguiente paso:** ninguno del arnés; el desarrollo sigue por orden de Fangusk: M1, el prototipo en gris. Si
un día quiere revisar G0, se abre la primera ronda de la cola de [entrevistas/one-pager-pending.md](entrevistas/one-pager-pending.md).

## Qué se está haciendo

| Qué | Dimensión | Quién | Desde | Estado |
|---|---|---|---|---|
| Adopción del arnés ([hilo](entrevistas/one-pager-pending.md)) | PU, PV, EE, EI | agente (Claude Code, Opus 5.5) | 2026-11-01 | `bloqueado por Fangusk`: las rondas de descubrimiento no se abren mientras él no lo pida (lo dijo en BakiFace, ronda 2, 2026-11-01) |
| Borrador anticipado del juego: código, puerta de calidad y [ADR](../adr/README.md), pedido por Fangusk | AT | agente (Claude Code, Opus 5.5) | 2026-11-01 | `en curso`: M0 validado por Fangusk el 2026-11-01 («tienes mi ok para M0»), con sus ADR aceptados; empieza M1, el prototipo en gris. `comprobar` avisa de que se escribe sin criterios de muerte confirmados (docs/07 §1 del arnés): Fangusk pidió construir ya, y el borrador no cuenta como evidencia de ninguna puerta hasta que confirme los del one-pager, cuya ronda está en cola |

## Equipo e hitos (EE)

| Persona o agente | Qué parte hace | Horas por semana (medidas, últimas cuatro semanas; EE-E1) |
|---|---|---|
| Fangusk | el juego que pide, las decisiones, las referencias del arte y las pruebas en los teléfonos | (hueco: en cola) |
| Claude Code (Opus 5.5), sesión «CaroFarty The Game» | el código, los ADR, la puerta de calidad y el estado del backlog | no aplica: es un agente |
| Claude, sesión «GameMaster» | el backlog compartido | no aplica: es un agente |

| Hito (EE-E4) | Fecha | Depende de | Colchón | Estado | Si se retrasa tres meses |
|---|---|---|---|---|---|
| M0 a M5 del backlog (diseño → prototipo en gris → Caro y el Bosque → Nieve y Desierto → música → entrega) | sin fecha: Fangusk no la fija (2026-11-01) | Fangusk: su validación de cada hito y sus referencias en M2 | — | M0 validado el 2026-11-01; M1 en curso | nada que pagar: es un regalo sin fecha |

## Qué espera respuesta humana

| Entrevista | Para quién | Ronda abierta | Desde | Qué decide la respuesta |
|---|---|---|---|---|

## Criterios de muerte vivos

| # | Criterio | Fecha | Estado | Resolución |
|---|---|---|---|---|

## Condiciones pendientes

| Condición | Puerta | Dueño | Fecha | Estado |
|---|---|---|---|---|

## Scorecard provisional

| Dimensión | Nota provisional | Confianza | Fichero |
|---|---|---|---|
| PU | 0 · sin cerrar · one-pager §2 | baja | [one-pager §2](one-pager.md#2-el-usuario-con-nombre-pu) |
| PV | 0 · sin cerrar · one-pager §1 | baja | [one-pager §1](one-pager.md#1-lo-que-creemos-pv) |
| MC | 0 · sin cerrar · one-pager §6 | baja | [one-pager §6](one-pager.md#6-existe-ya-la-búsqueda-de-diez-minutos-mc) |
| EE | 0 · sin cerrar · one-pager §7 | baja | [one-pager §7](one-pager.md#7-quién-lo-hace-y-con-cuánto-tiempo-ee) |
| EI | 0 · ninguna | baja | — |

## Actas

| Puerta | Fecha | Salida | Acta |
|---|---|---|---|

## Entregas

| Fecha | Salida | Afirmaciones de la portada | Commit | Versión del arnés | A quién se entregó |
|---|---|---|---|---|---|

## Riesgos que hay que mirar esta semana

- R-01 y R-02 · el motor sobre Compose Canvas y el audio sintetizado en tiempo real, sin probar todavía en un teléfono · impacto alto, se miden al construir el prototipo (M1) y la música (M4).
- R-04 · parecerse demasiado a Super Mario World · impacto alto, mitigando con la guía y su lista de comprobación.

## Historial de este fichero

| Fecha | Quién | Qué cambió |
|---|---|---|
| 2026-11-01 | agente (Claude Code, Opus 5.5) | Adopción del arnés 1.15.0 en modo nuevo, pedida por Fangusk, sobre las especificaciones, el diseño y los ADR de M0; rondas de descubrimiento en cola sin abrir, como en BakiFace; el desarrollo sigue como borrador anticipado; riesgos de `docs/proceso.md` pasados a `riesgos.md`; bloque del arnés en `AGENTS.md` y CI `mayeutic` |
| 2026-11-01 | agente (Claude Code, Opus 5.5) | Fangusk valida M0 («tienes mi ok para M0»): ADR 0001 a 0006 aceptados (los récords por escenario siguen propuestos); el borrador anticipado pasa a M1 |
