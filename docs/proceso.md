# Proceso, criterios de salida y riesgos

## Cómo se trabaja (PRD-07)

- **El backlog** es el tablero compartido: `../backlog-juego-plataformas` (lo creó la sesión GameMaster). Al empezar una
  tarea pasa a «En curso» y al terminarla a «Hecho», con `python3 scripts/backlog.py estado "En curso" ID…`, que también
  regenera el CSV, el Excel y el resumen.
- **Decide Fangusk**: lo que él dicta va a un ADR como aceptado; lo que decide el agente, como propuesto.
- **Hecho quiere decir**, en cada tarea:
  1. el código con sus pruebas (la lógica, en pruebas comunes que corren en la JVM, en Android y en el simulador de iOS);
  2. `bash scripts/verificar.sh` termina en «La puerta pasa»: sin `!!`, ktlint, sin avisos del compilador, pruebas,
     lint de Android, APK de depuración y de lanzamiento y la app de iOS compilada;
  3. probado en el emulador de Android y en el simulador de iOS, con capturas si se ve;
  4. las decisiones, en un ADR; los documentos, en español; el código, en inglés;
  5. el estado al día en el backlog.
- **Al cerrar cada hito**, una demostración a Fangusk y su visto bueno.

## Criterios de salida (PRD-09)

| Hito | Sale cuando |
|---|---|
| M0 Diseño y decisiones | Fangusk valida la [página de diseño](diseno.md) y los ADR |
| M1 Prototipo en gris | En el emulador y en el simulador: carrera, salto variable, barrancos, recolectables que suman y restan, fin de partida de las dos formas y generación validada (10.000 tramos sin un salto imposible); escalado píxel perfecto en 5 proporciones de pantalla; Fangusk da el visto bueno |
| M2 Caro, recolectables y Bosque | Arte final de Caro, los 4 recolectables y el Bosque, revisado con la [guía de propiedad intelectual](propiedad-intelectual.md); Fangusk lo valida |
| M3 Nieve y Desierto | Los tres escenarios jugables y a elegir antes de la partida; 30 min de partida automática en cada uno sin fallos ni tirones |
| M4 Música dinámica y efectos | Música generativa en los tres escenarios y efectos de sonido sin cortes ni clics; 30 min jugando sin que la música canse, a juicio de Fangusk |
| M5 Menús, pulido y entrega | Título, pausa, récords, ajustes e icono; instalado en sus teléfonos; entregado |

## Riesgos (PRD-08)

Se revisan al cerrar cada hito.

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| El motor propio sobre Compose Canvas no llega a 60 fps en un teléfono modesto | Media | Alto | Búfer de baja resolución, atlas, cero asignaciones por fotograma y perfilado en sus teléfonos (TEC-04, TEC-17, MOB-06) |
| Cortes o latencia en el audio sintetizado en tiempo real | Media | Alto | Salida de baja latencia en cada plataforma y medir el coste de la síntesis (AUD-25, AUD-29) |
| La música acaba cansando | Media | Medio | Generación con variación continua y la prueba de los 30 min (AUD-26) |
| Parecerse demasiado a Super Mario World | Media | Alto | [Guía de propiedad intelectual](propiedad-intelectual.md) en cada revisión de arte y de música |
| El salto por tiempo de pulsación no se entiende | Baja | Medio | Pista en la primera partida (GDD-17) y ajuste fino (PLY-15) |
| La app del iPhone caduca a los 7 días sin cuenta de pago | Alta si hay iPhone | Medio | Decidirlo al llegar a la instalación (TEC-13) |
