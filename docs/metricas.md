# Métricas y curva de dificultad

Las medidas del recorrido (GDD-06) y cómo crece la dificultad (GDD-08). Salen del propio código: `MetricsReportTest` las
mide con el salto del juego y las deja en `shared/build/metricas/`, así que si cambia un ajuste de
`files/ajustes.json` se vuelven a sacar y se copian aquí. Son los valores iniciales, para afinar jugando (PLY-15).
Unidades: 1 tile = 16 px = 1 m.

## El salto

El detalle está en [movimiento](movimiento.md). Los extremos:

- **El más bajo**, un toque rápido (0,05 s): 24 px de alto (1,5 tiles) y 3,5 tiles de largo a 160 px/s.
- **El más alto**, con el dedo apoyado 0,37 s o más: 67 px (4,2 tiles) y 0,75 s en el aire, 7,5 tiles de largo a
  160 px/s y 16,9 a 360 px/s.

## Los barrancos

| Distancia | Velocidad | Dificultad | Pide el generador | Justo: igual / +2 / −3 | Posible: igual / +2 / −3 |
|---|---|---|---|---|---|
| 0 m | 160 px/s | 0 % | 3 tiles | 5 / 4 / 7 | 6 / 5 / 7 |
| 300 m | 224 px/s | 2 % | 4 tiles | 8 / 7 / 11 | 9 / 7 / 11 |
| 600 m | 254 px/s | 10 % | 5 tiles | 10 / 8 / 12 | 10 / 9 / 13 |
| 900 m | 272 px/s | 21 % | 6 tiles | 10 / 9 / 13 | 11 / 9 / 14 |
| 1200 m | 284 px/s | 35 % | 7 tiles | 11 / 9 / 14 | 12 / 10 / 14 |
| 1800 m | 301 px/s | 64 % | 9 tiles | 12 / 10 / 15 | 12 / 10 / 15 |
| 2400 m | 314 px/s | 89 % | 10 tiles | 12 / 10 / 16 | 13 / 11 / 16 |
| 3600 m | 331 px/s | 100 % | 11 tiles | 13 / 11 / 16 | 14 / 12 / 17 |
| 5000 m | 343 px/s | 100 % | 12 tiles | 14 / 11 / 17 | 14 / 12 / 18 |
| 7500 m | 354 px/s | 100 % | 12 tiles | 14 / 12 / 18 | 15 / 13 / 18 |
| 10000 m | 357 px/s | 100 % | 12 tiles | 14 / 12 / 18 | 15 / 13 / 19 |

- **Pide el generador**: el barranco más ancho que puede salir a esa distancia; cada barranco se sortea entre 2 tiles y
  ese.
- **Justo**: el más ancho que pasa la regla del validador (3 momentos de despegue con 4 pulsaciones buenas cada uno,
  [generación](generacion.md)), al mismo nivel, con el suelo de enfrente 2 tiles más alto o 3 más bajo.
- **Posible**: el más ancho que se cruza con algún salto, aunque sea perfecto. Más allá, imposible.

Así que un barranco **fácil** es el de 3 tiles del principio, con 2 tiles de holgura sobre lo justo; uno **difícil**, el
de 12 tiles a toda velocidad, a 2 tiles del límite: 0,54 s de carrera, dentro de los 0,55 a 0,6 s que recomienda la guía 1
de GameMaster. Uno **imposible** es el que pasa de «Posible», y el generador no lo pide nunca (la prueba lo comprueba en
cada fila). Subiendo 2 tiles el límite justo baja: el validador descarta esas combinaciones y se genera otra pieza. Los
barrancos crecen en tiles porque Caro va más deprisa, pero medidos en tiempo de carrera siguen siendo igual de largos.

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
| 0 a 500 m | Hasta 0,6 min | 160 a 247 px/s | 0 a 7 % | 3 a 5 tiles | 2,0 a 2,4 · 1,7 |
| 500 a 1000 m | 0,6 a 1,1 min | 247 a 277 px/s | 7 a 26 % | 5 a 6 tiles | 2,4 a 3,6 · 2,1 |
| 1000 a 2000 m | 1,1 a 2,1 min | 277 a 306 px/s | 26 a 74 % | 6 a 9 tiles | 3,6 a 6,4 · 3,2 |
| 2000 a 3500 m | 2,1 a 3,3 min | 306 a 330 px/s | 74 a 100 % | 9 a 11 tiles | 6,4 a 8 · 2,8 |
| Más de 3500 m | Desde 3,3 min | 330 a 360 px/s | 100 % | 11 a 12 tiles | 8 · 2,8 |

- **La velocidad** sube durante toda la partida, como pidió Fangusk: deprisa al principio (277 px/s al minuto) y cada
  vez más despacio hacia los 360, sin llegar (344 a los 4,5 minutos, 357 a los 8) ([movimiento](movimiento.md)).
- **La dificultad** de los barrancos va más despacio, una curva en S de la distancia: en el primer minuto piden como
  mucho 0,38 s de carrera, que se salvan con un toque rápido (la guía 1 pide que perder en el primer minuto sea casi
  imposible), y llegan al máximo a los 3000 m, hacia los 3 minutos.
- **Los barrancos** piden del 45 % al 75 % del salto largo a la velocidad del momento.
- **Lo que resta** busca de 2 a 8 cada 100 m en los tramos de tensión, como mucho dos por pieza. Lo medido en 20 partidas
  de 5 km se queda en 1,7 a 3,2 de media, porque las calmas no llevan ninguno y el validador quita los que no se pueden
  esquivar con margen. Lo que suma, de 15 a 17 cada 100 m ([recolectables](recolectables.md)).
- **Tensión y calma** se turnan: tras 150 a 250 m de tensión vienen de 3 a 8 piezas tranquilas, unos 10 s, con perritos
  y gatitos y sin nada que reste. La partida empieza con 30 tiles llanos y una fila de perritos.

Desde los 3 minutos los barrancos ya no se alargan en tiempo de carrera, pero la velocidad sigue subiendo, cada vez más
despacio: es la dificultad que pidió Fangusk. Con ella mantienen el interés el ritmo de tensión y calma, la variedad del
recorrido y, en M4, la música. Si jugando se hace corto o largo, se ajusta en `files/ajustes.json`: la velocidad con
`startSpeed`, `quickSpeed`, `maxSpeed`, `quickRampMeters` y `slowRampMeters`, y los barrancos con `difficultyRampMeters`.
