# Registro de riesgos · CaroFarty

**Última revisión:** 2026-11-01, al terminar el prototipo en gris de M1 · **Abiertos:** 6 · **Vencidos sin acción:** 0

## Escalas

| Probabilidad | Significa | Impacto | Significa |
|---|---|---|---|
| **alta** | más probable que no en seis meses | **alto** | mata o pivota el proyecto; daño a personas; sanción |
| **media** | podría pasar | **medio** | retrasa una fase o cuesta dinero relevante |
| **baja** | sería raro | **bajo** | molesta, se absorbe |

## Riesgos vivos

| Id | Riesgo (qué pasaría y por qué) | Dimensión | Prob. | Impacto | Mitigación (qué hacemos para que no pase o duela menos) | Dueño | Fecha | Estado | Origen |
|---|---|---|---|---|---|---|---|---|---|
| R-01 | Si el motor propio sobre Compose Canvas no llega a 60 fps en un teléfono modesto, entonces el juego da tirones justo en lo que más importa, el salto, y cae H4 | RE | media | alto | Propuesta: dibujar en un búfer de baja resolución, sin asignaciones por fotograma, y medir en sus teléfonos desde el prototipo de M1 (TEC-04, TEC-17, MOB-06 del backlog; [ADR 0001](../adr/0001-motor-kotlin-multiplatform.md)). Hecho lo primero; medido el 2026-11-01 en el emulador y el simulador con el piloto automático: 60 FPS, de 1 a 4,5 ms de dibujo por fotograma y la memoria estable en 10 min ([rendimiento](../rendimiento.md)). Falta medirlo en sus teléfonos | Fangusk (propuesto) | — | mitigando | medida del agente (PRD-08) |
| R-02 | Si la música sintetizada en tiempo real se corta o llega con retraso en Android o en iOS, entonces la música dinámica que pidió Fangusk suena mal y cae H4 | RE | media | alto | Propuesta: una salida de audio de baja latencia en cada plataforma (AudioTrack, AVAudioEngine) y medir el coste de la síntesis en sus teléfonos (AUD-25, AUD-29 del backlog) | Fangusk (propuesto) | — | abierto | medida del agente (PRD-08) |
| R-03 | Si la música acaba cansando en una partida larga, entonces falla la especificación 8 de Fangusk («no puede aburrir porque la partida puede ser infinita») y cae H2 | UX | media | medio | Propuesta: música generativa con variación continua por escenario y la prueba de los 30 min a juicio de Fangusk (AUD-26, criterio de salida de M4) | Fangusk (propuesto) | — | abierto | medida del agente (PRD-08) |
| R-04 | Si el arte, la música o los sonidos se parecen demasiado a los de Super Mario World, entonces infringen la propiedad intelectual de Nintendo; con el juego en el teléfono de ella no pasa nada, pero impide publicarlo, que Fangusk deja abierto (ADR 0005) | LC | media | alto | Propuesta: la [guía de propiedad intelectual](../propiedad-intelectual.md), con su lista de comprobación en cada revisión de arte y de música (LEG-01, hecha) | Fangusk (propuesto) | — | mitigando | medida del agente (PRD-08) |
| R-05 | Si el salto por tiempo de pulsación no se entiende, entonces ella salta siempre igual, cae en los barrancos sin saber por qué y deja de jugar | UX | baja | medio | Propuesta: una pista en la primera partida, sin texto largo, y el ajuste fino jugando (GDD-17, PLY-15; [ADR 0002](../adr/0002-salto-por-tiempo-de-pulsacion.md)) | Fangusk (propuesto) | — | abierto | medida del agente (PRD-08) |
| R-06 | Si su teléfono es un iPhone y no hay Apple Developer Program, entonces la app caduca a los 7 días y hay que reinstalarla desde el Mac cada semana | OO | alta, si hay iPhone | medio | Propuesta: decidirlo al llegar a la instalación (TEC-13): reinstalar cada semana, o pagar el programa (ad hoc un año, o TestFlight 90 días por versión) | Fangusk (propuesto) | — | abierto | medida del agente (PRD-08) |

## Riesgos aceptados

| Id | Riesgo | Por qué se acepta | Quién acepta | Fecha | Se revisa el | Qué lo reabriría |
|---|---|---|---|---|---|---|

## Criterios de muerte enlazados

| Riesgo | Criterio de muerte | Fecha | Estado |
|---|---|---|---|

## Riesgos cerrados

| Id | Riesgo (qué pasaría y por qué) | Dimensión | Prob. | Impacto | Mitigación | Dueño | Fecha | Cómo se cerró | Origen |
|---|---|---|---|---|---|---|---|---|---|

## Reglas de este fichero

- Los ids `R-nn` no se reciclan.
- En cada puerta, toda fila de «Riesgos vivos» con fecha pasada se resuelve o se vuelve a fechar con motivo escrito; nunca se borra.
