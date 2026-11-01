# Métricas y curva de dificultad

Las medidas del recorrido (GDD-06) y cómo crece la dificultad (GDD-08). Salen del propio código: `MetricsReportTest` las
mide con el salto del juego y las deja en `shared/build/metricas/`, así que si cambia un ajuste de
`files/ajustes.json` se vuelven a sacar y se copian aquí. Son los valores iniciales, para afinar jugando (PLY-15).
Unidades: 1 tile = 16 px = 1 m.

## El salto

El detalle está en [movimiento](movimiento.md). Los extremos:

- **El más bajo**, un toque rápido (0,05 s): 24 px de alto (1,5 tiles) y 2,6 tiles de largo a 120 px/s.
- **El más alto**, con el dedo apoyado 0,37 s o más: 67 px (4,2 tiles) y 0,75 s en el aire, 5,6 tiles de largo a
  120 px/s y 9,4 a 200 px/s.

## Los barrancos

| Distancia | Velocidad | Dificultad | Pide el generador | Justo: igual / +2 / −3 | Posible: igual / +2 / −3 |
|---|---|---|---|---|---|
| 0 m | 120 px/s | 0 % | 2 tiles | 4 / 3 / 5 | 4 / 3 / 5 |
| 300 m | 137 px/s | 3 % | 2 tiles | 4 / 3 / 6 | 5 / 4 / 6 |
| 600 m | 151 px/s | 14 % | 3 tiles | 5 / 4 / 7 | 5 / 4 / 7 |
| 900 m | 162 px/s | 29 % | 4 tiles | 5 / 4 / 7 | 6 / 5 / 7 |
| 1200 m | 170 px/s | 47 % | 4 tiles | 6 / 5 / 8 | 6 / 5 / 8 |
| 1800 m | 182 px/s | 80 % | 5 tiles | 6 / 5 / 8 | 7 / 6 / 9 |
| 2400 m | 189 px/s | 99 % | 6 tiles | 7 / 5 / 9 | 7 / 6 / 9 |
| 3600 m | 196 px/s | 100 % | 6 tiles | 7 / 6 / 9 | 7 / 6 / 9 |
| 5000 m | 198 px/s | 100 % | 6 tiles | 7 / 6 / 9 | 8 / 6 / 10 |

- **Pide el generador**: el barranco más ancho que puede salir a esa distancia; cada barranco se sortea entre 2 tiles y
  ese.
- **Justo**: el más ancho que pasa la regla del validador (3 momentos de despegue con 4 pulsaciones buenas cada uno,
  [generación](generacion.md)), al mismo nivel, con el suelo de enfrente 2 tiles más alto o 3 más bajo.
- **Posible**: el más ancho que se cruza con algún salto, aunque sea perfecto. Más allá, imposible.

Así que un barranco **fácil** es el de 2 tiles del principio, con 2 tiles de holgura sobre lo justo; uno **difícil**, el
de 6 tiles a toda velocidad, a 1 tile del límite; e **imposible**, el que pasa de «Posible», que el generador no pide
nunca (la prueba lo comprueba en cada fila). Subiendo 2 tiles el límite justo baja a 5: el validador descarta esas
combinaciones y se genera otra pieza.

## Las alturas de los recolectables

| Dónde | Altura del centro sobre el suelo de despegue | Qué pide |
|---|---|---|
| En el suelo: fila u obstáculo | 8 px | La fila se recoge corriendo; el obstáculo se salva con cualquier salto que pase por encima |
| Techo sobre un barranco | 74 px | Lo toca un salto que suba más de 48 px por debajo (el dedo apoyado más de unos 0,2 s): pide uno más corto |
| Arco sobre un barranco | Sobre la trayectoria de un salto medio que cruza | Recogerlo es medir bien el salto |
| Elección | En lo alto de un salto largo sobre el obstáculo, por encima de 54 px | Un toque rápido (la cabeza llega a 44 px) pasa por debajo; manteniendo el dedo se recoge |

## La curva de dificultad

![La dificultad y la velocidad según la distancia](img/curva-dificultad.svg)

| Distancia | Tiempo de juego | Velocidad | Dificultad | Barranco más ancho | Restan cada 100 m: objetivo · medido |
|---|---|---|---|---|---|
| 0 a 500 m | Hasta 1 min | 120 a 147 px/s | 0 a 10 % | 2 a 3 tiles | 2,0 a 2,6 · 1,9 |
| 500 a 1000 m | 1 a 1,8 min | 147 a 165 px/s | 10 a 35 % | 3 a 4 tiles | 2,6 a 4,1 · 3,1 |
| 1000 a 2000 m | 1,8 a 3,4 min | 165 a 185 px/s | 35 a 90 % | 4 a 6 tiles | 4,1 a 7,4 · 4,4 |
| 2000 a 3500 m | 3,4 a 5,4 min | 185 a 196 px/s | 90 a 100 % | 6 tiles | 7,4 a 8 · 5,0 |
| Más de 3500 m | Desde 5,4 min | 196 a 200 px/s | 100 % | 6 tiles | 8 · 5,3 |

- **La dificultad** es una curva en S de la distancia: casi plana al principio para aprender, sube entre los 500 y los
  2000 m y llega al máximo a los 2500 m, a los 4 minutos de partida.
- **La velocidad** sube antes y se acerca a los 200 px/s sin llegar a tocarlos ([movimiento](movimiento.md)).
- **Los barrancos** piden del 45 % al 75 % del salto largo a la velocidad del momento.
- **Lo que resta** busca de 2 a 8 cada 100 m, como mucho dos por pieza; lo medido en 20 partidas de 5 km se queda en
  1,9 a 5,3, porque el validador quita los que no se pueden esquivar con margen. Lo que suma se mantiene en unos 16 cada
  100 m ([recolectables](recolectables.md)).
- **Los respiros** no cambian: cada 150 a 250 m, un tramo llano de 12 a 20 tiles sin nada que reste, con una fila de
  perritos y gatitos 6 de cada 10 veces. Y la partida empieza con 30 tiles llanos.

Desde los 4 minutos la dificultad ya no sube: lo que mantiene el interés es la variedad del recorrido y, en M4, la música.
Si jugando se hace corto o largo, se ajusta en `files/ajustes.json` (`difficultyRampMeters`, `speedRampMeters`).
