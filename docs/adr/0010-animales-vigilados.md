# ADR-0010 · Animales vigilados: huevos y pescados junto a los animales

| | |
|---|---|
| **Estado** | aceptada: el qué lo pidió Fangusk el 2026-11-02 tras jugar la beta 6 en su Nothing, y GameMaster lo transmitió con sus palabras; Fangusk estuvo de acuerdo con la propuesta de GameMaster de cómo hacerlo · propuesta: los números (cuántos, dónde exactamente, cuántos dilemas), que se afinan con el piloto y jugando |
| **Fecha** | 2026-11-02 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M5 · **Tarea:** GEN-23 |
| **Propone · decide** | Fangusk · Fangusk |
| **Reversibilidad** | alta · las cuotas viven en datos (`course` en `files/ajustes.json`: `guardedShareStart`, `guardedShareEnd`, `guardedDilemmaShare`); con 0 no sale ninguno |

## Contexto

Tras jugar la beta 6, Fangusk pidió, a la vez que algo más de plataformeo
([ADR 0009](0009-mas-plataformeo-plataformas-semisolidas-y-moviles.md), GEN-24): «además añadiría más huevos fritos y
pescado, no solo en zonas flotantes, sino también cerca de los animalitos, para que el jugador tenga que a veces elegir no
rescatar a un animalito con tal de no coger un pescado o un huevo».

Hasta entonces lo que resta iba sobre todo en el aire (en el camino del salto y en los techos de la
[ADR 0007](0007-la-dificultad-velocidad-y-obstaculos-en-el-aire.md)): con los suelos cortos de la ADR 0009, un huevo o un
pescado en el suelo casi nunca tenía la carrera que pide el validador para saltarlo.

## Decisión

Lo que propuso GameMaster y aceptó Fangusk:

- **Un huevo frito o un pescado donde aterriza el salto natural para rescatar un animal**: sobre la plataforma con premio o
  justo detrás del gatito.
- **Unos 2/3 se resuelven con habilidad y 1/3 son un dilema puro**: o se rescata el animal con un salto bien medido, o hay
  que elegir entre el animal y no restar.
- **El validador garantiza siempre un camino sin coger nada que reste.**
- **Pocos al principio y más con la distancia**, hasta uno de cada 3 o 4 animales. Los huevos y pescados del aire siguen.

Cómo, propuesta del agente ([generación](../generacion.md)):

- **En el suelo**, en la fila de animales de una pieza de tensión, donde cae el salto sobre el barranco. En un dilema, el
  primer animal donde aterriza el salto natural y el huevo o el pescado justo detrás, con las cajas casi tocándose (de 14 a
  18 px entre sus centros): rescatarlo es tocar los dos. Si pide habilidad, el huevo o el pescado donde caería el salto
  natural y el animal de 2,5 a 4 tiles más allá: un salto más largo lo rescata limpio. Los demás de la fila, detrás.
- **En una plataforma con premio**, siempre de habilidad, porque saltando desde abajo a través de la plataforma también se
  rescata el animal: el huevo o el pescado donde se aterriza y el animal al fondo, quieto.
- **En una plataforma de un barranco** con un animal encima, como en el suelo: el sitio donde se aterriza, dilema o
  habilidad; y los saltos para subirse y para bajarse siguen siendo justos.
- **El validador**: el aterrizaje sobre el barranco también cuenta si cae pasado lo que resta, con sitio para el siguiente
  salto. Ese es el camino limpio, y tiene que ser justo con la regla de siempre. Un animal vigilado solo entra si la
  simulación dice lo que tiene que ser: de habilidad si al menos 3 momentos tienen al menos 4 pulsaciones que lo rescatan
  sin tocar nada que reste, contando la carrera después de aterrizar; un dilema si ninguna lo hace. Si no sale, la fila
  queda como estaba y la pieza se hace otra vez.
- **Cuántos**: una fila de animales de una pieza de tensión lleva uno vigilado en el 5 % de los casos al empezar y en
  todos a toda dificultad; un animal solo en una plataforma, en las tres cuartas partes de eso. Con lo que no cabe, apenas
  ninguno en los primeros 600 m y uno de cada 5 animales a toda dificultad. Un tercio se intentan como dilema, y el que no
  cabe se le debe al siguiente: salen dilemas unos 4 de cada 10.
- **Más huevos y pescados en general**: lo que restan las piezas de tensión cada 100 m pasa de 2 a 8 a de 2,5 a 10,
  aparte de los vigilados. Con lo que cabe, se queda en 1 a 2,5 cada 100 m, poco más que antes (0,9 a 2,3): los barrancos
  con plataformas, ahora la mitad de los retos, no llevan obstáculos en el aire.
- **El sitio**: el suelo de una pieza con animales vigilados es algo más largo, lo que pide aterrizar pasado el huevo con
  sitio para el siguiente salto (11 o 12 tiles a toda velocidad, contra 10), y su barranco un tile más estrecho que el más
  ancho de ese momento. En esa pieza no va además otro huevo en el suelo.
- **El piloto de las pruebas** aprende a no aterrizar delante de un huevo sin la carrera para saltarlo: si no le queda
  otra, espera al último momento del coyote time, que lo puede llevar más allá. Así, el piloto cuidadoso juega 4 minutos
  en tres partidas sin tocar nada que reste: es la prueba de que el camino limpio existe.

## Alternativas

- **Solo en el aire, como antes**: descartada por Fangusk, «no solo en zonas flotantes».
- **El huevo al final del suelo, en el borde del siguiente barranco**: el salto limpio sería el del barranco siguiente, que
  aún no existe cuando se coloca; el validador no podría garantizarlo. Descartada por el agente.
- **Suelos largos para saltar el huevo con 0,4 s de carrera**: volverían los suelos largos y llanos que Fangusk quitó en la
  ADR 0009. Descartada por el agente.

## Consecuencias

- El equilibrio se mide en [métricas](../metricas.md#el-equilibrio): con un piloto cuidadoso y con un jugador simulado que
  falla, que puede ir además a por todos los animales.
- Los animales siguen sumando mucho más de lo que restan los huevos y pescados: las partidas acaban por un barranco, no
  por quedarse sin puntos. El dilema cuenta para los puntos máximos, no para seguir vivo.
