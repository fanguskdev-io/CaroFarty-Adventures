# Generación del recorrido

Especificación del recorrido infinito (GDD-07) que siguen GEN-01 a GEN-11. La decisión y sus alternativas están en el
[ADR 0004](adr/0004-generacion-por-reglas-con-validador.md).

## Objetivo

Un recorrido sin fin que parezca diseñado: con ritmo (tensión y calma), que no se repita y que siempre sea justo. Nunca
puede aparecer un salto imposible ni un recolectable que reste imposible de esquivar.

## Las piezas

Un **tramo** es un trozo de recorrido de varios tiles hecho de:

- **Suelo**: una tirada de tiles a una altura, entre 2 y 6 tiles por encima del borde inferior del área mínima
  ([ADR 0003](adr/0003-horizontal-y-pixel-perfecto.md)).
- **Barranco**: un hueco sin suelo. Caer en él acaba la partida.
- **Plataforma**: semisólida, como en Mario ([ADR 0009](adr/0009-mas-plataformeo-plataformas-semisolidas-y-moviles.md)):
  se atraviesa saltando desde abajo y se aterriza encima; por los lados y por debajo no frena, y encima se corre como
  sobre el suelo. Las móviles van y vienen solas y llevan a Caro con ellas.
- **Recolectables**: colocados con un patrón.

En la primera versión la altura del suelo solo cambia al otro lado de un barranco: no hay paredes en mitad del suelo
contra las que chocar. Chocar contra el borde de un suelo más alto tras un salto corto es caer al barranco.

## Las reglas del terreno

| Regla | Valor inicial |
|---|---|
| Largo de una tirada de suelo | De lo justo para aterrizar con margen y volver a despegar (4 tiles al empezar y 8 a 360 px/s; desde la beta 8, con algo más de sitio para aterrizar a más velocidad, 11 a los 10 minutos y 14 hacia el minuto 30) hasta 10 tiles; más largo solo si lleva una plataforma con premio, o animales vigilados en el suelo, que piden sitio para aterrizar pasado el huevo o el pescado (12 tiles a 360 px/s, 18 hacia el minuto 30). Fangusk, tras la beta 4: «suelos más cortos» |
| Ancho de un barranco | De 2 tiles hasta una fracción de la longitud de la pulsación larga a la velocidad de ese momento ([movimiento](movimiento.md)): el 45 % al empezar, hasta el 75 % |
| Cambio de altura al otro lado de un barranco | Subida de hasta 2 tiles; bajada de hasta 3 |
| Tensión y calma | Tras 150 a 250 m de tensión, de 2 a 3 piezas tranquilas (GEN-24; antes, de 2 a 4): suelo llano de 6 a 10 tiles, nunca más corto que el suelo más corto de ese momento, o el barranco más estrecho a la misma altura, sin nada que reste. Llevan perritos y gatitos el 60 % al empezar y el 15 % a toda dificultad (todas hasta la beta 7, [ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md)). Hacia el minuto 30, tensiones más largas y calmas más cortas |
| Barrancos con plataformas | Uno de cada 2 retos de la tensión (GEN-24; antes, uno de cada 2 o 3): un barranco más ancho que el salto más largo de ese momento, que solo se cruza saltando a 1 o 2 plataformas al empezar y a 2 o 3 a toda dificultad. Desde la mitad de la dificultad, todas de 1 o 2 tiles, quietas o móviles, para aterrizar y volver a saltar enseguida (GEN-26, [ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md)). En el 60 % de los de 2 o 3, una escalera: cada una un tile más alta, o más baja, que lo anterior, hasta el suelo de después; en el resto, cada una de 1 tile más baja a 2 más alta. Cada salto entre ellas, de la mitad al todo del barranco más ancho de ese momento (del 45 al 75 % del salto más largo), y más si hace falta para que el conjunto pase del salto más largo; cada plataforma, de lo que mide el suelo más corto a un tile más. A veces espera encima un perrito o un gatito |
| Plataformas que caen | De las quietas de los barrancos con plataformas, desde el 20 % de la dificultad, una de cada 5, y 4 de cada 10 a toda dificultad: tiemblan 0,3 s cuando ella se pone encima y caen sin volver, con ella si sigue encima (ENM-16, [ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md)) |
| Hielo y arenas movedizas | En la Nieve, hielo; en el Desierto, arenas movedizas: al final de un suelo, antes del barranco siguiente, de 2 a 4 tiles con uno de suelo normal a cada lado; en el 15 % de los suelos de los retos al empezar y el 30 % a toda dificultad (PLY-18). Sobre el hielo corre un 12 % más deprisa y lo mantiene en el salto; sobre la arena se hunde 3 px y salta un 30 % más bajo ([movimiento](movimiento.md)) |
| Repetición | Nunca tres piezas difíciles iguales seguidas: mismo barranco, mismo escalón y lo que resta en el mismo sitio |
| Inicio de la partida | 30 tiles llanos, con una fila de perritos y gatitos, antes del primer barranco |

## Los patrones de recolectables

| Patrón | Qué pide |
|---|---|
| Fila en el suelo | Perritos y gatitos que se recogen corriendo: los barrancos de la tensión piden 2 grupos de animales cada 100 m al empezar (8 hasta la beta 7) y 3 a toda dificultad, pero más pequeños, casi todos vigilados: de 2 a 4 animales al empezar, de 1 a 2 desde la mitad de la dificultad y uno solo al final (ADR 0011). Los perritos pasean por su suelo o se quedan quietos ([ADR 0008](adr/0008-perritos-que-caminan-y-gatitos-de-ocho-pelajes.md)): como mucho a 2 tiles de su sitio, a 2 tiles o más de los bordes y a 3 tiles o más de lo que resta, así que siempre se recogen corriendo |
| Obstáculo en el suelo | Un huevo frito o un pescado en el camino que hay que saltar, como los cactus del dinosaurio |
| Plataforma con premio | Una plataforma semisólida de 2 o 3 tiles sobre un tramo largo de suelo (antes un bloque; Fangusk aceptó el cambio, [ADR 0009](adr/0009-mas-plataformeo-plataformas-semisolidas-y-moviles.md)), con hueco para pasar corriendo por debajo, y encima un gatito sentado o un perrito que pasea y se da la vuelta en sus bordes. Sin saltar se pasa por debajo; manteniendo el dedo se sube, también atravesándola desde abajo, se recoge y se baja al mismo suelo. La llevan el 35 % de los grupos de animales de la tensión al empezar y el 10 % a toda dificultad (antes, una de cada tres piezas), con el suelo alargado lo justo para que quepa a cualquier velocidad (si no cabe, la pieza se hace otra vez: nada de suelos largos y vacíos), y el validador comprueba que varios saltos aterrizan encima. Los animales nunca flotan: Fangusk, «En el aire no puede aparecer ningún animal, salvo que esté en una plataforma» ([ADR 0008](adr/0008-perritos-que-caminan-y-gatitos-de-ocho-pelajes.md)) |
| Animal vigilado | Un huevo frito o un pescado junto a un animal, para que a veces haya que elegir no rescatarlo (GEN-23, [ADR 0010](adr/0010-animales-vigilados.md)). En el suelo, donde aterriza el salto natural sobre el barranco: en un dilema, justo detrás del primer animal de la fila, con las cajas casi tocándose, así que rescatarlo es tocar los dos; si pide habilidad, delante de él, donde caería el salto natural, y el animal 2,5 a 4 tiles más allá, así que un salto más largo lo rescata limpio. En una plataforma con premio, siempre de habilidad: el huevo donde se aterriza y el animal al fondo; en una plataforma de un barranco, como en el suelo. Pocos al principio (en el 5 % de las filas) y más con la distancia, hasta todas las filas y todos los animales solos en una plataforma. A toda dificultad, desde la beta 8, uno de cada 2 animales, y casi 9 de cada 10 de ellos, dilemas: rescatarlo cuesta 2 o 4 puntos, y quien va a por todos se puede quedar sin puntos (uno de cada 5 y 4 de cada 10 con la beta 7, [ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md)) |
| Donde se aterriza | Un huevo frito o un pescado al principio de lo que hay tras un barranco o un salto entre plataformas, donde cae el salto natural: hay que aterrizar más lejos (GEN-25, ADR 0011). Más a menudo con la dificultad; nunca en una plataforma pequeña |
| Pareja alto/bajo | Un huevo frito o un pescado un poco por encima del camino del salto natural y otro un poco por debajo: el salto tiene que pasar entre los dos, midiendo su altura (GEN-25). A toda dificultad, 8 de cada 10 de los del camino del salto |
| Techo | Un huevo frito o un pescado alto sobre un barranco: pide un salto que no suba demasiado |
| En el camino del salto | Un huevo frito o un pescado sobre el barranco, donde pasaría el salto que cualquiera haría primero (cerca de su punto más alto, un poco por encima o por debajo): hay que adelantar, retrasar o medir el salto de otra manera. Lo pidió Fangusk ([ADR 0007](adr/0007-la-dificultad-velocidad-y-obstaculos-en-el-aire.md)) |
| Bloque | Un bloque sólido de 1 o 2 tiles sobre el barranco, cuyo borde de abajo alcanza la cabeza de Caro en ese mismo salto: lo corta. Desde abajo frena la subida, de costado la para en el aire y encima se puede correr. Sobre un suelo deja siempre al menos 28 px por debajo, para que se pueda pasar corriendo. También lo pidió Fangusk |
| Huevo o pescado que flota | De los del camino del salto, el techo y las parejas, algunos suben y bajan de 8 a 16 px cada 1,5 s más o menos; en una pareja, a la vez o al contrario, y el hueco entre los dos se abre y se cierra (ENM-17, [ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md)). Desde el 20 % de la dificultad, uno de cada 4, y 6 de cada 10 a toda dificultad |
| Pájaro de frente | Viene desde la derecha, de 80 a 140 px/s más rápido que el suelo, y se cruza con ella donde despega o sobre el barranco: bajo, hay que saltarlo; a la altura de un salto, hay que medirlo o pasar por debajo antes de saltar; alto, solo da un salto alto. La mitad ondulan. Resta 8 (ENM-18, Fangusk). Desde la mitad de la dificultad, sobre el 8 % de los barrancos, el 20 % a toda dificultad y el 40 % hacia el minuto 30, y sobre uno de los saltos entre plataformas; nunca de un salto que deje de ser justo, ni el de antes ni el de después |
| Pájaro que pasea | Va y viene sobre el barranco de 30 a 50 px/s, alrededor del salto evidente, y se da la vuelta en los extremos; siempre encima del barranco, así que solo se cruza con ella en el aire. Seis de cada 10 pájaros lo intentan |
| Pez que salta | En el Bosque y la Nieve: sale del agua del barranco cuando ella se acerca y está en lo alto de su salto cuando ella pasa por ese punto del salto evidente, un poco por encima o por debajo; entra y sale del agua dentro del barranco. Resta como un pescado (ENM-15). Desde un cuarto de la dificultad, en uno de cada 4 barrancos, casi la mitad a toda dificultad |

Los dos últimos, los obstáculos en el aire, empiezan cuando la dificultad pasa del 5 %. Al principio lleva uno el 35 % de
los barrancos y, a toda dificultad, el 90 %; un cuarto son bloques (la mitad hasta la beta 7). Desde la beta 8 también
lleva uno cada salto entre plataformas, con la misma probabilidad, y una pieza puede llevar hasta tres que restan. Cada uno tiene cuatro intentos de colocarse donde la
pieza siga siendo justa; si no lo consigue, la pieza sale sin él. Todo vive en `files/ajustes.json` (`airFromDifficulty`,
`airChanceStart`, `airChanceEnd`, `airBlockShare`).

Los que restan aparecen más a medida que sube la dificultad; las cifras medidas y la ficha de cada recolectable están en
[recolectables](recolectables.md).

## El validador

Antes de que un tramo entre en el mundo, el validador simula los saltos con el mismo código de física del juego
(`JumpLab`): cada momento de despegue de los últimos 0,2 s de carrera antes del borde y del coyote time, de fotograma en
fotograma, con cada duración de pulsación. Un barranco es **justo** si al menos 3 de esos momentos tienen al menos 4
pulsaciones distintas que:

1. aterrizan al otro lado con al menos 1 tile de margen;
2. dejan por delante al menos 0,2 s de carrera para preparar el siguiente salto;
3. no tocan nada que reste.

En un barranco con plataformas cada salto, del suelo a la primera plataforma, de una a otra y de la última al suelo, tiene
que ser justo con la misma regla; y además ningún salto del suelo de antes llega al de después sin ellas: si llega, el
barranco no las necesita y se hace otro. Desde 225 px/s, en el 60 % de esos barrancos una plataforma se mueve, de lado o
arriba y abajo (GEN-24; antes, desde 250 px/s y en la mitad); como Caro no puede pararse a esperarla, los saltos para
subirse y para bajarse se prueban con ella en 6 momentos de su recorrido, y si alguno no es justo se queda quieta.

Con algo que restar en el suelo donde se aterriza, el aterrizaje cuenta si deja por delante los 0,4 s de carrera para
saltarlo, o si cae pasado todo ello con su sitio para el siguiente salto: así es como se dejan atrás los animales
vigilados. En una plataforma pequeña vale cualquier aterrizaje encima, y el salto siguiente tiene que ser justo desde donde cae
primero y desde su mismo borde, con lo que queda de plataforma y el coyote time (GEN-26). Un salto que ya cae por debajo
de donde podría aterrizar deja de seguirse: el validador va más deprisa.

Un animal vigilado solo entra si el validador dice lo que tiene que ser: de habilidad, si al menos 3 momentos
tienen al menos 4 pulsaciones que lo rescatan sin tocar el huevo o el pescado, siguiendo la carrera después de aterrizar;
un dilema, si ninguna lo hace.

Así nunca hace falta un salto perfecto: hay margen en el momento y en la duración. Un obstáculo en el suelo se valida igual,
con 0,4 s de carrera para tomarlo. Una plataforma con premio solo entra si al menos cuatro saltos del suelo aterrizan
encima y el barranco anterior sigue siendo justo con ella. Los bloques entran en la misma simulación: el validador prueba
los saltos contra ellos con la física del juego, y un salto que se da con uno no cuenta como bueno.

Lo que se mueve solo, los huevos y pescados que flotan, los pájaros y los peces que saltan (ADR 0011), está donde dice lo
lejos que ha llegado Caro y no el reloj: en cada intento el validador lo ve donde estaría con ella ahí, así que el camino
limpio lo es llegue cuando llegue. Un pájaro tiene que dejar justos todos los saltos de su pieza, no solo el suyo, porque
puede cruzarse con ella donde aterriza otro. Las plataformas que caen se prueban como las pequeñas, desde donde cae
primero y desde el borde, cayéndose a su tiempo; y el hielo y la arena con la misma física del juego: el barranco que
sigue a uno tiene que ser justo despegando de él.

Si un tramo no es justo se genera otro; tras 8 intentos sale uno fácil, un barranco de 2 tiles a la misma altura y sin
nada que reste; un barranco con plataformas que no sale justo en 8 intentos deja su sitio a un barranco de siempre. En las
pruebas, un piloto automático que salta como una persona cuidadosa (y espera, si puede, a un salto que le deje sitio para
el siguiente) recorre 39 km en 30 minutos sin caer, juega 5 partidas de 10 minutos sin caer ni tocar nada que reste, y se
generan 10.000 tramos seguidos sin un error, a 2,3 ms cada uno desde la beta 9 (GEN-10, GEN-11). Con los animales
vigilados, y con los peligros de la beta 9 en los tres escenarios, el piloto cuidadoso sigue sin tocar nada que reste: es
la prueba de que siempre hay un camino limpio (GEN-23, ADR 0011).

## La dificultad

Crece con la distancia: la velocidad ([movimiento](movimiento.md)), la fracción del salto largo que piden los barrancos y
los recolectables que restan cada 100 m (de 2 al empezar a 8). Las calmas se mantienen siempre. Las cifras y la curva están
en [métricas](metricas.md).

## Los escenarios

Ella elige el escenario antes de cada partida y el recorrido se genera entero en él, sin transiciones
([ADR 0006](adr/0006-tres-escenarios-a-elegir.md)): el generador recibe el escenario y usa sus piezas, su paleta y sus
fondos (GEN-08). En la primera versión los tres comparten estas reglas de terreno; una mecánica propia por escenario
sería una decisión de Fangusk, y entraría como reglas o patrones de ese escenario sin tocar el validador.

## Semilla y memoria

- Dos fuentes de azar con semilla, terreno y recolectables (GEN-05): la misma semilla da la misma partida en cualquier
  teléfono.
- Se genera en segundo plano hasta 1024 px por delante de Caro (unas dos pantallas y media) y se suelta lo que queda
  256 px por detrás; el hilo del juego no crea objetos en cada fotograma (TEC-17).
- Las coordenadas del mundo crecen sin fin, pero en doble precisión: tras 24 horas a 200 px/s van por 17 millones de
  píxeles y aún distinguen milmillonésimas de píxel. No hace falta recolocarlas cerca del origen; `FarFromTheOriginTest`
  comprueba que el salto es idéntico lejos del origen (GEN-09).
