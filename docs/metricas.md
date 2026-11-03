# Métricas y curva de dificultad

Las medidas del recorrido (GDD-06) y cómo crece la dificultad (GDD-08). Salen del propio código: `MetricsReportTest` las
mide con el salto del juego y las deja en `shared/build/metricas/`, así que si cambia un ajuste de
`files/ajustes.json` se vuelven a sacar y se copian aquí. Son los valores iniciales, para afinar jugando (PLY-15).
Unidades: 1 tile = 16 px = 1 m.

## El salto

El detalle está en [movimiento](movimiento.md). Los extremos:

- **El más bajo**, un toque rápido (0,05 s): 24 px de alto (1,5 tiles) y 3,5 tiles de largo a 160 px/s.
- **El más alto**, con el dedo apoyado 0,37 s o más: 67 px (4,2 tiles) y 0,75 s en el aire, 7,5 tiles de largo a
  160 px/s, 16,9 a 360 px/s y 26,3 a 560 px/s.

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
| 3600 m | 339 px/s | 100 % | 11 tiles | 13 / 11 / 17 | 14 / 12 / 18 |
| 5000 m | 368 px/s | 100 % | 12 tiles | 15 / 12 / 19 | 15 / 13 / 19 |
| 7500 m | 405 px/s | 100 % | 14 tiles | 16 / 14 / 20 | 17 / 15 / 20 |
| 10000 m | 432 px/s | 100 % | 15 tiles | 18 / 15 / 20 | 18 / 16 / 20 |

- **Pide el generador**: el barranco más ancho que puede salir a esa distancia; cada barranco se sortea entre 2 tiles y
  ese.
- **Justo**: el más ancho que pasa la regla del validador (3 momentos de despegue con 4 pulsaciones buenas cada uno,
  [generación](generacion.md)), al mismo nivel, con el suelo de enfrente 2 tiles más alto o 3 más bajo.
- **Posible**: el más ancho que se cruza con algún salto, aunque sea perfecto. Más allá, imposible.

Así que un barranco **fácil** es el de 3 tiles del principio, con 2 tiles de holgura sobre lo justo; uno **difícil**, el
de 11 a 15 tiles a toda dificultad, a 2 o 3 tiles del límite: de 0,52 a 0,56 s de carrera, cerca de los 0,55 a 0,6 s que
recomienda la guía 1 de GameMaster. Uno **imposible** es el que pasa de «Posible», y el generador no lo pide nunca (la prueba lo comprueba en
cada fila). Subiendo 2 tiles el límite justo baja: el validador descarta esas combinaciones y se genera otra pieza. Los
barrancos crecen en tiles porque Caro va más deprisa, pero medidos en tiempo de carrera siguen siendo igual de largos.

## Las alturas de los recolectables

| Dónde | Altura del centro sobre el suelo de despegue | Qué pide |
|---|---|---|
| En el suelo: fila u obstáculo | 8 px | La fila se recoge corriendo; el obstáculo se salva con cualquier salto que pase por encima |
| Techo sobre un barranco | 74 px | Lo toca un salto que suba más de 48 px por debajo (el dedo apoyado más de unos 0,2 s): pide uno más corto |
| Encima de una plataforma con premio | 8 px sobre la plataforma, que tiene su borde de abajo de 28 a 36 px sobre el suelo | Sin saltar se pasa por debajo; con el dedo apoyado algo más de 0,2 s se sube a recogerlo |

## La curva de dificultad

![La dificultad y la velocidad según la distancia](img/curva-dificultad.svg)

| Distancia | Tiempo de juego | Velocidad | Dificultad | Barranco más ancho | Restan cada 100 m: objetivo · medido |
|---|---|---|---|---|---|
| 0 a 500 m | Hasta 0,6 min | 160 a 247 px/s | 0 a 7 % | 3 a 5 tiles | 2,5 a 3,7 · 1,0 |
| 500 a 1000 m | 0,6 a 1,1 min | 247 a 277 px/s | 7 a 26 % | 5 a 6 tiles | 3,7 a 7,0 · 2,8 |
| 1000 a 2000 m | 1,1 a 2,1 min | 277 a 306 px/s | 26 a 74 % | 6 a 9 tiles | 7,0 a 15 · 3,9 |
| 2000 a 3500 m | 2,1 a 3,3 min | 306 a 337 px/s | 74 a 100 % | 9 a 11 tiles | 15 a 20 · 4,2 |
| Más de 3500 m | Desde 3,3 min | 337 px/s; 462 a los 10 min y 553 a los 30, sin pasar de 560 | 100 % | 11 tiles; 15 a los 10 000 m | 20 · 3,7 |

- **La velocidad** sube durante toda la partida, como pidió Fangusk: deprisa al principio (270 px/s al minuto) y cada
  vez más despacio (327 a los 3 minutos). Desde los 3000 m, tras la beta 7, sigue subiendo despacio hacia los 560, que
  no pasa nunca: 382 a los 5 minutos, 462 a los 10, 507 a los 15 y 553 a los 30 (GDD-23,
  [ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md), [movimiento](movimiento.md)).
- **La dificultad** de los barrancos va más despacio, una curva en S de la distancia: en el primer minuto piden como
  mucho 0,38 s de carrera, que se salvan con un toque rápido (la guía 1 pide que perder en el primer minuto sea casi
  imposible), y llegan al máximo a los 3000 m, hacia los 3 minutos. Desde ahí sigue creciendo hasta el minuto 30, hacia
  los 53 km: barrancos un poco más justos, calmas más cortas y tensiones más largas, más plataformas, casi todas de 1
  tile y móviles, y más parejas alto/bajo (ADR 0011).
- **Los barrancos** piden del 45 % al 75 % del salto largo a la velocidad del momento.
- **Lo que resta** busca de 2,5 a 20 cada 100 m en los tramos de tensión desde la beta 8 (de 2 a 8 hasta la beta 6, de
  2,5 a 10 en la 7), como mucho tres por pieza, y aparte los de los saltos entre plataformas, los de donde se aterriza y
  los que vigilan a los animales ([ADR 0010](adr/0010-animales-vigilados.md),
  [ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md)). Lo medido en 20 partidas de 5 km se queda en 1 al principio
  y de 3,7 a 4,2 desde los 1000 m, con los pájaros, los peces que saltan y los que flotan de la beta 9 (de 3,2 a 3,8 con
  la 8), porque las calmas no llevan y el validador quita los que no se pueden esquivar con margen (0,9 a 2,3 con la beta
  6). Lo que suma, de 3,9 al principio a 1,2 a toda dificultad (de 11,5 a 8,6 con la beta
  7): ahora hay más que restar que que sumar desde los 1000 m ([recolectables](recolectables.md)).
- **Los obstáculos en el aire** (ADR 0007) empiezan hacia los 300 m y suben con la dificultad: a toda velocidad, unos
  0,6 bloques cada 100 m, y los huevos y pescados en el camino del salto ya cuentan en «Restan».
- **Tensión y calma** se turnan: tras 150 a 250 m de tensión vienen 2 o 3 piezas tranquilas, unos 4 s, sin nada que
  reste y con perritos y gatitos en el 60 % al empezar y en el 15 % a toda dificultad. La partida empieza con 30 tiles
  llanos y una fila de perritos.

Desde los 3 minutos los barrancos ya no se alargan en tiempo de carrera, pero la velocidad sigue subiendo hasta el minuto
30 y con ella lo que piden: es la dificultad que pidió Fangusk. Con ella mantienen el interés el ritmo de tensión y
calma, la variedad del recorrido y la música. Si jugando se hace corto o largo, se ajusta en `files/ajustes.json`: la
velocidad con `startSpeed`, `quickSpeed`, `maxSpeed`, `quickRampMeters`, `slowRampMeters`, `lateSpeed`,
`lateFromMeters` y `lateRampMeters`, y la dificultad con `difficultyRampMeters` y `lateDifficultyMeters`.

## El equilibrio

Cuánto duran las partidas y cómo acaban (GEN-23, GEN-24, GEN-25): `BalanceReportTest` juega el Bosque con varios
jugadores y lo deja en `shared/build/metricas/equilibrio.md`. El **piloto cuidadoso** es el de las pruebas. El **jugador que a veces falla**
sigue sus consejos con los errores de una persona: uno de cada diez saltos lo aprieta de 67 a 133 ms tarde y el resto
hasta 33 ms tarde; mantiene el dedo hasta 50 ms más o menos de lo que haría falta; y uno de cada cien no lo hace. El
tercero es el mismo jugador **a por todos los animales**: rescata el que tiene delante antes de saltar un huevo o un
pescado, y salta a las plataformas con un animal encima aunque haya algo que reste. Cada partida dura como mucho 10
minutos.

Con la beta 6:

| Quién | Partidas | Metros (mediana) | Metros (1.er y 3.er cuartil) | Duración (mediana) | Por barranco | Sin puntos | Siguen a los 10 min | Rescates por minuto | Restas por minuto |
|---|---|---|---|---|---|---|---|---|---|
| Piloto cuidadoso | 6 | 12309 | 12296–12310 | 600,0 s | 0,0 % | 0,0 % | 100,0 % | 111,7 | 0,0 |
| Jugador que a veces falla | 80 | 268 | 128–456 | 22,9 s | 100,0 % | 0,0 % | 0,0 % | 77,2 | 1,4 |
| El mismo, a por todos los animales | 80 | 221 | 128–453 | 18,9 s | 100,0 % | 0,0 % | 0,0 % | 83,7 | 1,2 |

Con más plataformeo y los animales vigilados (beta 7):

| Quién | Partidas | Metros (mediana) | Metros (1.er y 3.er cuartil) | Duración (mediana) | Por barranco | Sin puntos | Siguen a los 10 min | Rescates por minuto | Restas por minuto |
|---|---|---|---|---|---|---|---|---|---|
| Piloto cuidadoso | 6 | 12319 | 12317–12323 | 600,0 s | 0,0 % | 0,0 % | 100,0 % | 84,4 | 0,0 |
| Jugador que a veces falla | 80 | 293 | 136–559 | 24,6 s | 100,0 % | 0,0 % | 0,0 % | 70,3 | 1,6 |
| El mismo, a por todos los animales | 80 | 246 | 84–509 | 21,2 s | 100,0 % | 0,0 % | 0,0 % | 74,9 | 1,5 |

- **El piloto cuidadoso** sigue sin caer ni tocar nada que reste en 10 minutos: siempre hay un camino limpio. Rescata
  menos, porque deja atrás los animales de los dilemas.
- **El jugador que falla** dura más o menos lo mismo que con la beta 6. Se cae casi siempre en un barranco de los de
  siempre: de sus 80 caídas, solo 10 fueron en un barranco con plataformas, aunque ya son la mitad de los retos. Para
  este jugador simulado, las plataformas no piden más que los barrancos a los que sustituyen. A una persona quizá sí: las
  escaleras, las móviles y los saltos más largos entre plataformas son cosas que el modelo no nota.
- **Ir a por todos los animales** cuesta: restas por minuto y partidas algo más cortas que sin ir a por ellos.
- **Ninguna partida acaba sin puntos**: se rescatan unos 70 animales por minuto y se tocan 1 o 2 huevos o pescados. El
  dilema cuenta para los puntos máximos, no para seguir jugando.

Con la beta 8 ([ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md)): 5 puntos al empezar, el huevo a −3 y el pescado a
−5, muchos menos animales, más restas, parejas alto/bajo, restas donde se aterriza, plataformas pequeñas, la velocidad y
la dificultad hasta el minuto 30, y animales vigilados uno de cada 2, casi todos en un dilema. Se añaden dos jugadores:
el piloto que va a por todos los animales, sin fallar nunca un salto, en 40 partidas, y uno hábil, que falla un salto de
cada quinientos y aprieta tarde uno de cada treinta.

| Quién | Partidas | Metros (mediana) | Metros (1.er y 3.er cuartil) | Duración (mediana) | Por barranco | Sin puntos | Siguen a los 10 min | Rescates por minuto | Restas por minuto |
|---|---|---|---|---|---|---|---|---|---|
| Piloto cuidadoso | 10 | 13781 | 13780–13784 | 600,0 s | 0,0 % | 0,0 % | 100,0 % | 11,7 | 0,0 |
| El piloto, a por todos los animales | 40 | 13417 | 10251–13581 | 596,2 s | 25,0 % | 30,0 % | 45,0 % | 17,5 | 4,2 |
| Jugador que a veces falla | 80 | 286 | 124–505 | 24,2 s | 100,0 % | 0,0 % | 0,0 % | 25,6 | 1,6 |
| El mismo, a por todos los animales | 80 | 286 | 131–511 | 24,2 s | 100,0 % | 0,0 % | 0,0 % | 26,8 | 1,9 |
| Jugador hábil | 40 | 450 | 184–713 | 35,4 s | 100,0 % | 0,0 % | 0,0 % | 25,9 | 1,5 |
| El hábil, a por todos los animales | 40 | 511 | 256–835 | 39,4 s | 100,0 % | 0,0 % | 0,0 % | 26,3 | 1,5 |

- **El piloto cuidadoso** sigue sin caer ni tocar nada que reste en 10 minutos, ya a 462 px/s: el camino limpio sigue
  estando, también con las plataformas pequeñas. Rescata 12 animales por minuto (84 con la beta 7): hay muchos menos y
  deja los de los dilemas.
- **Quedarse sin puntos ya pasa** (Fangusk: «quiero que sea un riesgo real quedarse sin puntos»): el piloto que va a por
  todos los animales, sin fallar nunca un salto, se queda sin puntos en 3 de cada 10 partidas, dentro del objetivo que
  propuso GameMaster (del 25 al 40 %). En los 3 primeros minutos junta unos 45 puntos; después, cada animal vigilado le
  cuesta 2 o 4 y toca unas 4 restas por minuto, y el colchón se le va acabando. Con la beta 7 rescataba 62 animales por
  minuto y ninguna partida acababa así.
- **Los jugadores simulados** se caen pronto, casi siempre en los primeros 500 m, antes de que llegue lo difícil, como con
  las betas 6 y 7: no llegan a donde se acaban los puntos. Rescatan 26 animales por minuto en vez de 70.

Fangusk jugó la beta 8: quedarse sin puntos «es un peligro real», la velocidad le parece bien a los 5 y a los 10 minutos
y el arranque sigue siendo amable. Los valores se quedan.

Con la beta 9 (ADR 0011): los huevos y pescados que flotan, los pájaros que restan 8, los peces que saltan del agua, las
plataformas que caen y el hielo y la arena:

| Quién | Partidas | Metros (mediana) | Metros (1.er y 3.er cuartil) | Duración (mediana) | Por barranco | Sin puntos | Siguen a los 10 min | Rescates por minuto | Restas por minuto |
|---|---|---|---|---|---|---|---|---|---|
| Piloto cuidadoso | 10 | 13783 | 13781–13783 | 600,0 s | 0,0 % | 0,0 % | 100,0 % | 12,2 | 0,0 |
| El piloto, a por todos los animales | 40 | 12205 | 7386–13588 | 552,3 s | 50,0 % | 10,0 % | 40,0 % | 17,8 | 3,6 |
| Jugador que a veces falla | 80 | 286 | 124–505 | 24,2 s | 100,0 % | 0,0 % | 0,0 % | 25,6 | 1,5 |
| El mismo, a por todos los animales | 80 | 286 | 131–511 | 24,2 s | 100,0 % | 0,0 % | 0,0 % | 26,9 | 1,9 |
| Jugador hábil | 40 | 450 | 184–713 | 35,4 s | 100,0 % | 0,0 % | 0,0 % | 26,0 | 1,5 |
| El hábil, a por todos los animales | 40 | 511 | 256–835 | 39,4 s | 100,0 % | 0,0 % | 0,0 % | 26,7 | 1,7 |

- **El piloto cuidadoso** sigue limpio 10 minutos, y en las pruebas largas también en la Nieve y el Desierto, con el
  hielo, la arena y los peces: el camino limpio sigue estando.
- **El que va a por todos los animales** se cae en la mitad de las partidas, por un cuarto con la beta 8: ir a por un
  animal sin mirar lo pone delante de un pájaro, una plataforma que cae o un salto que ya no da. Por eso se queda sin
  puntos menos (una de cada 10): antes se cae.

