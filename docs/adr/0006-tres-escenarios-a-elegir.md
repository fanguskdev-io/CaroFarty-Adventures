# ADR-0006 · Tres escenarios distintos y ella elige dónde jugar

| | |
|---|---|
| **Estado** | aceptada: el qué lo dictó Fangusk el 2026-11-01, y la pantalla para elegir escenario entra con su visto bueno a M0: «tienes mi ok para M0» (Fangusk, 2026-11-01) · propuesta: los récords por escenario, que siguen pendientes de que lo confirme |
| **Fecha** | 2026-11-01 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M0 · **Tarea:** GDD-12 (y GEN-08, UI-20) |
| **Propone · decide** | Fangusk · Fangusk |
| **Reversibilidad** | alta en el cómo; el qué es suyo |

## Contexto

La primera página de diseño proponía un recorrido que pasaba de un escenario a otro cada 600 m, con tramos de transición.
Fangusk lo corrigió el mismo día: «Vale una pequeña aclaración sobre los escenarios, no tienen que ser el mismo y que vaya
cambiando cada 600 metros. Son 3 escenarios distintos, el usuario elige donde jugar sin en Bosque, Nieve o Desierto».

## Decisión

- **Tres escenarios distintos**: Bosque, Nieve y Desierto. Antes de cada partida, ella elige en cuál jugar.
- **La partida entera transcurre en el escenario elegido**, con su decorado, su paleta y su música, sin transiciones a otro.
  El generador recibe el escenario y usa sus piezas (GEN-08).
- **Una pantalla para elegir escenario** antes de la partida, con una vista de cada uno, que recuerda el último elegido
  (UI-20, propuesta). Llega en M3, cuando existan los tres; mientras solo exista el Bosque (M2), se juega directamente en él.
- **Récords**: los dos que decidió Fangusk (distancia y puntos máximos). Que vayan por escenario es una propuesta pendiente de
  que la confirme: los escenarios podrían acabar teniendo dificultades distintas.

## Consecuencias

- Fuera las transiciones entre escenarios (ART-15 pasa a fuera de alcance) y la música ya no cambia de escenario en mitad de
  una partida (AUD-19): cada escenario tiene la suya, que no puede aburrir en una partida larga.
- El flujo de pantallas gana un paso: título → elegir escenario → partida (UI-01).
- Una particularidad jugable de un escenario (hielo que resbala, arenas…) sigue siendo una decisión de Fangusk; entraría como
  reglas de ese escenario, sin tocar el validador ([ADR 0004](0004-generacion-por-reglas-con-validador.md)).
