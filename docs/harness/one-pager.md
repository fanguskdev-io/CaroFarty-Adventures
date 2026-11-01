# One-pager · CaroFarty

| | |
|---|---|
| **Proyecto** | CaroFarty (el nombre que Fangusk le dio al proyecto el 2026-11-01) |
| **Fecha** | 2026-11-01 |
| **Autor/a** | Fangusk (quien contesta en la sesión del 2026-11-01; la identidad de git de los commits del proyecto, `Fangusk <fangusk.dev@gmail.com>`) |
| **Objetivo, en una frase** | Regalarle a su esposa un juego de plataformas infinito en pixel art para su móvil, en el que Caro corre sola, ella solo salta y elige escenario, y al que quiera volver porque no aburre · propuesto |
| **Talla** | sin declarar (motivo en la sección 8) |
| **Retroactivo** | no |

## 1. Lo que creemos (PV)

Fangusk le hace a su esposa un juego a su medida: Caro corre sola por un recorrido infinito, ella salta los barrancos
manteniendo el dedo y recoge perritos y gatitos (que suman) esquivando huevos fritos y pescado (que restan), en uno de
tres escenarios con el estilo de Super Mario World (las especificaciones, literales, en [docs/diseno.md](../diseno.md)).
Creemos que un juego hecho para ella, con su protagonista y sus reglas, le hará más ilusión y la hará volver más que
cualquier juego de tienda (supuesto). (PV-E1 en parte: el cambio frente a lo que ella juega hoy lo da la ronda de contraste,
sección 6)

## 2. El usuario, con nombre (PU)

- **Quién:** su esposa, por su papel: «necesito que me ayudes a crear un videojuego para android y iOS para mi esposa»
  (Fangusk, 2026-11-01) · reportado. No contesta en este hilo: lo que diga de ella llega por Fangusk
- **Usuarios con nombre:** ninguno más: es un regalo; «para ella ahora, quizá publicar después» (Fangusk, 2026-11-01;
  [ADR 0005](../adr/0005-un-regalo-sin-conexion.md)) · reportado
- **Dónde y cuántos, a ojo:** una persona, en su teléfono · reportado
- **Plataforma:** Android e iPhone: «para android y iOS» (Fangusk) · reportado; qué teléfono tiene ella, (hueco: se fija al
  llegar a la instalación, TEC-13 del backlog)
- **Cómo sabemos que existe:** reportado · lo dice Fangusk, que la conoce; lo que cuenta quien hace el juego no cuenta como
  conversación con alguien de fuera (docs/03 §5 del arnés)
- **Para quién no es:** el público de las tiendas, mientras Fangusk no decida publicarlo (ADR 0005) · reportado

## 3. El problema, en una frase (PU)

> (hueco: la frase en palabras de ella, que diría Fangusk; no se pregunta: el decisor expone lo que tiene que hacer el
> juego)

- **La última vez:** (hueco: PU-E2 sin preguntar; está en cola y no se abre mientras Fangusk no lo pida). La búsqueda no
  buscó una queja pública: un regalo no resuelve una queja de un mercado.

## 4. Hipótesis: qué tendría que ser cierto

| # | Hipótesis | Tipo | Cómo la comprobaríamos | Evidencia hoy |
|---|---|---|---|---|
| H1 | A su esposa le hace ilusión un juego hecho para ella y lo juega por su cuenta | problema | después de instalárselo, vuelve a jugar otro día sin que nadie se lo pida (lo observa Fangusk) | supuesto |
| H2 | Con un recorrido que no se repite, la música generativa y tres escenarios para elegir, una partida larga no la aburre | valor | 30 min de juego sin que la música ni el recorrido cansen, a juicio de Fangusk (criterio de salida de M4) | supuesto |
| H3 | Fangusk tiene tiempo para validar cada hito, pasar las referencias del arte y probar en los teléfonos | negocio | cada hito del backlog se valida sin quedarse parado más de dos semanas | supuesto |
| H4 | Un motor propio sobre Compose Canvas da 60 fps y la música sintetizada en tiempo real no se corta en sus teléfonos | ejecución | el prototipo de M1 y la música de M4, medidos en sus teléfonos ([ADR 0001](../adr/0001-motor-kotlin-multiplatform.md); R-01, R-02) | supuesto |

*Las hipótesis las ha formulado el agente con lo dicho: la ronda que toque las devuelve para que Fangusk las corrija.*

## 5. Criterios de muerte

*(hueco: los dice Fangusk en la ronda de criterios de muerte, en cola; el agente no los inventa)*

| # | Criterio: si en esa fecha… | Fecha | …entonces | Hipótesis que cae | Clase | Estado | Resolución |
|---|---|---|---|---|---|---|---|

**De dónde sale cada uno:** (hueco)

**Pre-mortem:** (hueco: en cola)

## 6. ¿Existe ya? La búsqueda de diez minutos (MC)

- **Términos buscados:** «endless runner pixel art game collect cats and dogs avoid obstacles mobile 2026» · «Super Mario
  Run price free to start unlock full game tap to jump auto runner» · **Fecha:** 2026-11-01
- **Lo más parecido encontrado:** el dinosaurio de Google Chrome, la referencia que dio Fangusk, gratis dentro del
  navegador. [Super Mario Run](https://www.neowin.net/news/super-mario-run-now-available-on-android---free-to-download-999-to-unlock-the-full-game/)
  (Nintendo, 2016), una carrera automática con salto al tocar: gratis para empezar y 9,99 $ para desbloquear el juego entero,
  y pide conexión permanente ([Super Mario Wiki](https://www.mariowiki.com/Super_Mario_Run)). Runners con gatos:
  [Stray Cat Runner](https://straycatrunner.com/), en pixel art, donde se recoge pescado, y
  [Cat Runner](https://play.google.com/store/apps/details?id=com.solou.catendless.run&hl=en), en 3D, con perros y gatos
  como personajes y monedas que recoger (lectura de la IA, verificada el 2026-11-01)
- **Qué tareas de las nuestras cubre:** correr solo y saltar: sí, todos. El salto según cuánto se mantiene el dedo: Super Mario
  Run sí. Recoger perritos y gatitos y esquivar huevos fritos y pescado: ninguno. Una protagonista que es ella: ninguno.
  Elegir escenario: sin medir
- **Límites de su plan gratuito:** Super Mario Run, solo los primeros niveles; los demás, sin medir · 2026-11-01
- **Nota, valoraciones y quejas:** sin medir: no se leyeron sus fichas ni sus reseñas
- **¿Hace exactamente esto, gratis y bien hecho?** No, por lo medido: ninguno lleva a Caro ni las reglas que dictó Fangusk;
  el regalo es justo eso
- **La diferencia, en una frase:** (hueco: sale de la ronda de contraste, en cola, frente a lo que ella juega hoy)

## 7. Quién lo hace y con cuánto tiempo (EE)

- **Personas:** Fangusk (hueco: horas por semana de las últimas cuatro semanas, EE-E1, en cola)
- **Agentes y herramientas:** Claude Code (Opus 5.5) escribe el código, los ADR de `docs/adr/` y la puerta de calidad, como
  borrador anticipado; la sesión GameMaster lleva el backlog compartido; Android Studio, el emulador de Android y Xcode con
  sus simuladores, en este Mac (observado)
- **Tiempo disponible para P1:** (hueco) · **Dependencias externas:** ninguna biblioteca de terceros fuera de Kotlin y Compose
  Multiplatform; para el iPhone, la firma de Apple (R-06)
- **Lo que pide P1 en la talla:** (hueco: con la talla sin declarar) · **Antes del:** (hueco)
- **Evidencia de fuera:** (hueco)
- **Presupuesto de la idea:** ninguno, mientras la persona no fije uno

## 8. Talla y motivo

- **Talla:** sin declarar: la ronda de la talla está en cola
- **Por qué:** (hueco: el agente la propone con su motivo en esa ronda)

## 9. Declaraciones

| Pregunta | Respuesta | Consecuencia |
|---|---|---|
| ¿Hay software propio, también firmware? (software) | sí: un juego de Kotlin Multiplatform para Android e iOS, medido en el repositorio (`androidApp/`, `iosApp/`, `shared/`) · propuesta por el agente | G4 y G5 comprueban sus criterios de contenedores |
| ¿Qué pantalla, mensajes, órdenes o aparato propios usa quien lo usa? (interfaz) | sí: las pantallas del juego (título, elegir escenario, la partida con su marcador, pausa y fin de partida), medido en [docs/diseno.md](../diseno.md) · propuesta por el agente | UX y AC se abren, críticas en G3, y AC también en G5 |
| ¿Hay componente físico? (HF) | no: usa el teléfono de ella · propuesta por el agente | HF `n/a` cuando se confirme |
| ¿El producto usa modelos de IA, o sus usuarios son agentes? (IA) | no: el juego no usa modelos de IA y lo juega una persona (los agentes solo lo construyen) · propuesta por el agente | IA `n/a` cuando se confirme |
| ¿Cómo le llega a la persona lo que hacéis? (perfil) | (hueco: lo propone el agente en la ronda de la talla, en cola) | — |
| ¿Qué se ha decidido sobre quién se queda con el dinero que sobra? (sin ánimo de lucro) | (hueco) | — |
| ¿Quién puede salir perjudicado sin ser su usuario? (EI-E1) | (hueco: en cola) | — |
| ¿Se tocan datos de salud, menores o finanzas? (DP) | (hueco: en cola); medido: el juego solo guarda en el teléfono sus récords y sus ajustes, sin conexión ([ADR 0005](../adr/0005-un-regalo-sin-conexion.md)) | — |
| ¿Desde qué país se vende, se factura o se recibe el dinero de fuera? (LC) | (hueco: en cola; no cuenta en G0) | — |

## 11. Siguiente paso

- **Próxima puerta:** G0 · (hueco: fecha) · **Quién la revisa:** (hueco)
- **Después de G0:** `/harness-interview problema-y-usuarios`
