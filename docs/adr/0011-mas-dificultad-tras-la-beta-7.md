# ADR-0011 · Más difícil tras la beta 7: más restas, menos animales, plataformas pequeñas y peligros nuevos

| | |
|---|---|
| **Estado** | aceptada: el qué lo pidió Fangusk el 2026-11-03 tras jugar la beta 7, con sus palabras, y eligió las reglas y los peligros entre las propuestas de GameMaster · propuesta: los números y cómo se colocan, que se afinan con el piloto, el jugador simulado y jugando |
| **Fecha** | 2026-11-03 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M5 · **Tarea:** GDD-21, GDD-23, GEN-25, GEN-26, ENM-15, ENM-16, ENM-17, ENM-18, PLY-18, ART-27 |
| **Propone · decide** | Fangusk · Fangusk |
| **Reversibilidad** | alta · las reglas y las cuotas viven en datos (`score` y `course` en `files/ajustes.json`); los peligros nuevos, en el generador, el validador y la física |

## Contexto

Tras jugar la beta 7, Fangusk dijo: «está muy bien, pero sigue siendo muy fácil, hay muy pocos huevos y pescados en
comparación a animalitos y además los huevos y pescados que hay son muy fáciles de esquivar. Necesitamos aumentarle la
dificultad con más huevos y pescados, además de plataformas más pequeñas para que exija coordinación en el salto».

Las medidas lo confirmaban: con la beta 7, a toda dificultad salían unos 8,6 animales cada 100 m por 2,5 huevos o
pescados, y quien juega rescataba unos 70 animales por minuto y tocaba 1 o 2 restas; ninguna partida acababa sin puntos
([métricas](../metricas.md#el-equilibrio)).

## Decisión

Lo que pidió Fangusk y lo que eligió entre las propuestas de GameMaster:

- **Más huevos y pescados y menos animales**, que la proporción cambie de verdad: como orientación, la mitad de animales y
  el triple de restas a toda dificultad.
- **Más difíciles de esquivar**: más en el camino del salto, parejas alto/bajo que obligan a medir la altura, sobre las
  plataformas donde se aterriza, entre plataformas seguidas y animales vigilados más a menudo (uno de cada 3, con más
  dilemas).
- **Plataformas más pequeñas**: de 1–2 tiles en los retos obligatorios a partir de dificultad media, y móviles de 1–2.
- **Reglas**: empezar con 5 puntos en vez de 10; el huevo resta 3 y el pescado 5, en vez de 2 y 3. Que quedarse sin
  puntos pase de verdad; el piloto cuidadoso sigue encontrando siempre un camino limpio y el inicio sigue suave.
- **Cuatro peligros nuevos**: pescados que saltan del agua de los barrancos del Bosque y de la Nieve, como los peces de
  Mario; plataformas que se caen al pisarlas (tiemblan unos 0,3 s y caen, sin volver); huevos y pescados que flotan
  arriba y abajo (8–16 px cada 1,5 s más o menos); y el terreno de cada escenario: hielo que resbala en la Nieve (algo más
  rápida, +10–15 %, y saltos más largos) y arenas movedizas en el Desierto (se hunde unos píxeles y salta un 30 % más
  bajo, antes de barrancos o bloques). En el Bosque, de momento, nada.

Después, tras dejar 30 minutos la app de medida en su Nothing: «la velocidad creo debería de ajustarse a mayor; una
partida continua de 30 minutos debería de ser muy difícil entre obstáculos y velocidad, pero sin embargo actualmente se
ve asequible». Estuvo de acuerdo con lo que le propuso GameMaster (GDD-23), con cifras orientativas:

- **La velocidad sin techo a los 2 minutos**: tras el arranque de siempre (de 160 a 360 px/s en unos 2 minutos), que siga
  subiendo despacio hasta unos 560 px/s hacia el minuto 30, con un tope.
- **La dificultad sigue creciendo con el tiempo**: más huevos y pescados, plataformas de 1 tile y móviles, los peligros
  nuevos, calmas más cortas y barrancos más justos, hasta que hacia el minuto 30 sea muy difícil.
- **Más visión a esas velocidades**: Caro más cerca del borde izquierdo, por ejemplo a 64 px.
- **Siempre justo**, con cada salto comprobado a la velocidad de ese momento: el piloto cuidadoso todavía llega, raspando, y
  un jugador normal no llega a los 30 minutos. **El arranque, igual de suave.**

Y, ya con la beta 8 en marcha: «quiero que sea un riesgo real quedarse sin puntos». GameMaster propuso el objetivo: en la
dificultad media y alta, que quien se deja tocar algunas restas gane más o menos lo que pierde; que el jugador simulado
que va a por todos los animales se quede sin puntos en el 25 al 40 % de las partidas; que el piloto cuidadoso siga
teniendo un camino limpio; y que en el primer minuto se gane algo de colchón. La palanca, muchos menos animales, de unos
62 rescates por minuto a 15 o 20, y vigilados uno de cada 2 o 3, con más dilemas. Otra, que los puntos bajen solos y
los animales los recarguen, solo con el sí de Fangusk.

Y pidió **pájaros** que restan como los huevos y los pescados (ENM-18): uno por escenario, cuervo en el Bosque,
frailecillo en la Nieve y buitre en el Desierto, que **restan 8 puntos**, por encima de los 3 que le recomendó GameMaster.
«Los pájaros no pueden estar estancos en el aire, tienen que tener su animación de desplazarse», y de dos tipos: «que
algunos lo hagan así y otros que simplemente estén paseando por el aire». Los primeros vuelan de frente desde la derecha
hacia Caro, más rápido que el suelo, a tres alturas (bajo, hay que saltarlo; medio, medir el salto o pasar por debajo sin
saltar; alto, solo da si salta alto), rectos u ondulando; los otros van y vienen sobre un tramo del aire y se dan la
vuelta en los extremos. Al tocarlos, el retroceso y el parpadeo de siempre. Salen desde dificultad media, cada vez más a
menudo, nunca en el arranque, y el validador tiene en cuenta su movimiento.

Cómo, propuesta del agente. Se hace en dos betas: la 8 con lo que pidió directamente y las reglas, y la 9 con los
peligros nuevos, según llega su arte. La beta 8 ([generación](../generacion.md)):

- **Muchos menos animales**: los barrancos de la tensión piden 2 grupos de animales cada 100 m al empezar, en vez de 8,
  y 3 a toda dificultad, pero de 1 animal y casi todos vigilados; las calmas, las plataformas de los barrancos y el suelo
  tras ellas los llevan en el 60 % de los casos al empezar y en el 15 % a toda dificultad; y las plataformas con premio
  son el 35 % de los grupos al empezar y el 10 % al final. Antes, un animal vigilado obligaba a poner una fila en casi
  cada pieza; ahora solo se vigila donde ya hay animales.
- **Más que restar**: hasta 3 por pieza (un techo, uno en el suelo si cabe y otro en el camino del salto); en el aire,
  más huevos y pescados que bloques (un cuarto de bloques en vez de la mitad); una pareja alto/bajo, a toda dificultad,
  en 8 de cada 10 de los del camino del salto; en cada salto entre plataformas, uno en el aire; y donde aterriza el salto
  sobre el barranco o sobre una plataforma ancha, uno que hay que dejar atrás aterrizando más lejos.
- **Vigilados**: un animal solo en una plataforma, tanto como una fila; las filas, de 2 a 4 animales al empezar, de 1 a
  2 desde la mitad de la dificultad y de uno solo al final; y 8 de cada 10 se intentan como dilema, en vez de un tercio.
  Así, a toda dificultad, uno de cada 2 animales está vigilado y casi siempre es un dilema: rescatarlo cuesta 2 o 4
  puntos.
- **Plataformas pequeñas**: desde la mitad de la dificultad, todas las de los retos obligatorios miden 1 o 2 tiles, quietas
  o móviles. El validador las prueba de otra manera: cualquier aterrizaje encima vale, y el salto siguiente tiene que ser
  justo desde la punta donde caiga primero y desde la del borde, con lo que queda y el coyote time. El piloto aterriza en
  ellas y vuelve a saltar enseguida.
- **Las viñetas de «Cómo se juega»** empiezan con 20 puntos, para que el huevo y el pescado de la suya no las acaben.

- **La velocidad hasta el minuto 30**: tras la subida de siempre, desde los 3000 m otra lenta hacia los 560 px/s, que no
  pasa nunca: 382 a los 5 minutos, 462 a los 10 y 553 a los 30 ([movimiento](../movimiento.md)). Pasados los 360 px/s,
  Caro se acerca al borde izquierdo, hasta 64 px a 560.
- **La dificultad hasta el minuto 30**: desde los 3000 m, sobre unos 50 km, barrancos un poco más justos, tensiones más
  largas y calmas más cortas, más barrancos con plataformas y más de ellas, casi todas de 1 tile y móviles, y más parejas
  alto/bajo. Cada salto se sigue comprobando a la velocidad de ese momento, con algo más de sitio para aterrizar cuanto
  más deprisa va.

Los cuatro peligros y los pájaros, en la beta 9.

## Alternativas

- **Todo en una beta**: Fangusk esperaría a los cuatro peligros para probar lo que pidió él. Descartada por el agente.
- **Subagentes en paralelo**: los peligros y el equilibrio tocan el mismo generador y el mismo validador; repartirlos
  daría cambios difíciles de casar. Descartada por el agente.

## Consecuencias

- Salen 3,9 animales cada 100 m al empezar y 1,2 a toda dificultad, por 11,5 y 8,6 con la beta 7; lo que resta, de 1 a
  3,8 (de 1 a 2,6). Desde los 1000 m hay más que restar que que sumar ([métricas](../metricas.md#el-equilibrio)).
- El piloto cuidadoso sigue sin caer ni tocar nada que reste en 10 minutos, y 30 minutos sin caer. Rescata 12 animales
  por minuto, por 84 con la beta 7.
- El piloto que va a por todos los animales, sin fallar nunca un salto, se queda sin puntos en 3 de cada 10 partidas de
  10 minutos: junta unos 45 puntos en los 3 primeros y los pierde en los dilemas. Con la beta 7, en ninguna.
- Los jugadores simulados que fallan se siguen cayendo en los primeros 500 m, antes de llegar a donde se acaban los
  puntos. Si jugando se queda corto o se pasa, se ajusta con los grupos de animales (`positivesPer100m`,
  `positivesPer100mEnd`) y los dilemas (`guardedDilemmaShare`) en `files/ajustes.json`.
- Hacer cada pieza cuesta unos 2 ms en la JVM. El validador prueba todas las pulsaciones de un salto a la vez, con la
  carrera y el dedo apoyado simulados una sola vez, y deja de seguir un salto que ya ha caído por debajo de donde podría
  aterrizar.
