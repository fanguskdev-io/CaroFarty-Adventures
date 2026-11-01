# ADR-0005 · Un regalo sin conexión

| | |
|---|---|
| **Estado** | aceptada en el qué: «Para ella ahora, quizá publicar después» (Fangusk, 2026-11-01) · propuesta en el cómo: sin permiso de internet y todo guardado en el teléfono |
| **Fecha** | 2026-11-01 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M0 · **Tarea:** PRD-01 |
| **Propone · decide** | agente (Claude Code, Opus 5.5) · Fangusk |
| **Reversibilidad** | alta · lo comercial sigue en el backlog, aparcado |

## Contexto

Fangusk hace el juego para su esposa. Preguntado si quiere publicarlo en Google Play y App Store, eligió «para ella
ahora, quizá publicar después».

## Decisión

- **Sin anuncios, sin compras, sin analítica, sin servicios en línea** y sin permiso de internet. El récord y los ajustes
  se guardan en el teléfono.
- Lo comercial (monetización, servicios en línea, fichas de tienda, legal de publicación, marketing) **no se borra**:
  sigue en el backlog, en las tablas «Fuera de alcance (decide Fangusk)», por si algún día se publica.
- La instalación en sus teléfonos se decide en TEC-13. En Android basta el ordenador; en iPhone, con un Apple ID gratuito
  la app caduca a los 7 días, y con el Apple Developer Program dura un año (ad hoc) o 90 días por versión (TestFlight).

## Alternativas consideradas

| Alternativa | A favor | Por qué se descarta |
|---|---|---|
| Preparar desde ya la publicación | Menos trabajo si al final se publica | Fangusk no lo ha decidido; multiplica el backlog (de unos 330 a unos 750 días-persona) |
| Analítica para ajustar la dificultad | Datos reales | Saca datos del teléfono de su esposa sin necesidad; las pruebas las hace Fangusk |

## Consecuencias

- La dificultad se ajusta con las partidas de prueba de Fangusk y con el piloto automático de los tests, no con datos
  remotos.
- Si se publica: reactivar EP14, EP15 y EP20 y las tareas legales (política de privacidad, formularios de las tiendas,
  clasificación por edades).
