# Movimiento de Caro

La tabla de parámetros del salto y la carrera (GDD-05), lista para programar. Las unidades son píxeles del juego
(1 tile = 16 px = 1 m) y segundos. Son los valores iniciales: se ajustan jugando (PLY-15) y vivirán en datos (TEC-07).

## Parámetros

| Parámetro | Valor | Nota |
|---|---|---|
| Paso de la física | 1/60 s | Con interpolación para pantallas de 90 y 120 Hz (TEC-05) |
| Velocidad al empezar | 120 px/s (7,5 m/s) | Algo menos que correr en Super Mario World |
| Velocidad máxima | 200 px/s (12,5 m/s) | Algo más que su carrera a tope |
| Subida de la velocidad | `v(d) = 200 − 80 · e^(−d / 1200 m)` | 151 px/s a 600 m, 171 a 1200 m, 189 a 2400 m; la curva final se cierra en GDD-08 |
| Impulso del salto | 230 px/s hacia arriba | Al apoyar el dedo, sin esperar a levantarlo |
| Gravedad con el dedo apoyado | 300 px/s² | Mientras sube y como mucho durante la ventana de pulsación |
| Ventana de pulsación | 0,36 s | Mantener más no salta más |
| Gravedad normal | 1600 px/s² | Al soltar, al llegar arriba y al caer |
| Velocidad máxima de caída | 300 px/s | 5 px por fotograma |
| Coyote time | 0,08 s (5 fotogramas) | Se puede saltar justo después de dejar el borde |
| Buffer de salto | 0,1 s (6 fotogramas) | Un toque justo antes de aterrizar salta al aterrizar |
| Caja de colisión de Caro | 10 × 20 px | Provisional, menor que el sprite y a su favor (CHR-08) |
| Caja de los recolectables | 12 × 12 px | Provisional |
| Primer toque de la partida | Arranca la carrera | No salta ([ADR 0002](adr/0002-salto-por-tiempo-de-pulsacion.md)) |

## El salto según el tiempo de pulsación

Simulado con la misma lógica que el juego (60 pasos por segundo, el dedo cuenta por pasos), despegando y aterrizando a la
misma altura. Un toque normal en una pantalla dura entre 0,05 y 0,1 s.

| Dedo apoyado | Altura | Tiempo en el aire | Longitud a 120 px/s | A 160 px/s | A 200 px/s |
|---|---|---|---|---|---|
| 3 pasos, 0,05 s (toque rápido) | 24 px (1,5 tiles) | 0,35 s | 42 px (2,6 tiles) | 56 px | 70 px |
| 5 pasos, 0,08 s | 29 px (1,8 tiles) | 0,40 s | 48 px (3,0 tiles) | 64 px | 80 px |
| 7 pasos, 0,12 s | 35 px (2,2 tiles) | 0,45 s | 54 px (3,4 tiles) | 72 px | 90 px |
| 10 pasos, 0,17 s | 42 px (2,7 tiles) | 0,52 s | 62 px (3,9 tiles) | 83 px | 103 px |
| 12 pasos, 0,20 s | 47 px (3,0 tiles) | 0,55 s | 66 px (4,1 tiles) | 88 px | 110 px |
| 15 pasos, 0,25 s | 54 px (3,4 tiles) | 0,62 s | 74 px (4,6 tiles) | 99 px | 123 px |
| 18 pasos, 0,30 s | 60 px (3,7 tiles) | 0,68 s | 82 px (5,1 tiles) | 109 px | 137 px |
| 22 pasos o más, 0,37 s (pulsación larga) | 67 px (4,2 tiles) | 0,75 s | 90 px (5,6 tiles) | 120 px | 150 px (9,4 tiles) |

La ventana de pulsación de 0,36 s deja que cuenten 22 pasos: con el vigésimo segundo, el dedo lleva 0,367 s. `CaroTest`
comprueba que el juego salta exactamente esto.

Lo que sale de aquí para diseñar el recorrido (se afina en GDD-06):

- El barranco más ancho que se puede cruzar es la longitud de la pulsación larga a la velocidad del momento; el generador
  solo usa una parte de ella, con margen ([generación](generacion.md)).
- Subir de un suelo a otro más alto al otro lado de un barranco cuesta altura: el salto largo sube 4,2 tiles, así que los
  desniveles de subida se quedan en 2 tiles.
- Con la velocidad, la misma pulsación salta más lejos: la dificultad viene de la velocidad, no de cambiar el salto.
