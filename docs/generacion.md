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
| Largo de una tirada de suelo | De lo justo para aterrizar con margen y volver a despegar (4 tiles al empezar, 8 a toda velocidad) hasta 10 tiles; más largo solo si lleva una plataforma con premio. Fangusk, tras la beta 4: «suelos más cortos» |
| Ancho de un barranco | De 2 tiles hasta una fracción de la longitud de la pulsación larga a la velocidad de ese momento ([movimiento](movimiento.md)): el 45 % al empezar, hasta el 75 % |
| Cambio de altura al otro lado de un barranco | Subida de hasta 2 tiles; bajada de hasta 3 |
| Tensión y calma | Tras 150 a 250 m de tensión, de 2 a 4 piezas tranquilas: suelo llano de 6 a 10 tiles o el barranco más estrecho a la misma altura, con perritos y gatitos y nada que reste |
| Barrancos con plataformas | Uno de cada 2 o 3 retos de la tensión: un barranco más ancho que el salto más largo de ese momento, que solo se cruza saltando a 1, 2 o 3 plataformas (más a menudo varias con la dificultad). Cada salto entre ellas, de 1 tile hasta el 55 % del salto más largo; cada plataforma, de lo que mide el suelo más corto a un tile más, de 1 tile más baja a 2 más alta que lo anterior. A veces espera encima un perrito o un gatito |
| Repetición | Nunca tres piezas difíciles iguales seguidas: mismo barranco, mismo escalón y lo que resta en el mismo sitio |
| Inicio de la partida | 30 tiles llanos, con una fila de perritos y gatitos, antes del primer barranco |

## Los patrones de recolectables

| Patrón | Qué pide |
|---|---|
| Fila en el suelo | Perritos y gatitos que se recogen corriendo. Los perritos pasean por su suelo o se quedan quietos ([ADR 0008](adr/0008-perritos-que-caminan-y-gatitos-de-ocho-pelajes.md)): como mucho a 2 tiles de su sitio, a 2 tiles o más de los bordes y a 3 tiles o más de lo que resta, así que siempre se recogen corriendo |
| Obstáculo en el suelo | Un huevo frito o un pescado en el camino que hay que saltar, como los cactus del dinosaurio |
| Plataforma con premio | Una plataforma semisólida de 2 o 3 tiles sobre un tramo largo de suelo (antes un bloque; Fangusk aceptó el cambio, [ADR 0009](adr/0009-mas-plataformeo-plataformas-semisolidas-y-moviles.md)), con hueco para pasar corriendo por debajo, y encima un gatito sentado o un perrito que pasea y se da la vuelta en sus bordes. Sin saltar se pasa por debajo; manteniendo el dedo se sube, también atravesándola desde abajo, se recoge y se baja al mismo suelo. Lo lleva una de cada tres piezas de tensión, con el suelo alargado lo justo para que quepa a cualquier velocidad (si no cabe, la pieza se hace otra vez: nada de suelos largos y vacíos), y el validador comprueba que varios saltos aterrizan encima. Los animales nunca flotan: Fangusk, «En el aire no puede aparecer ningún animal, salvo que esté en una plataforma» ([ADR 0008](adr/0008-perritos-que-caminan-y-gatitos-de-ocho-pelajes.md)) |
| Techo | Un huevo frito o un pescado alto sobre un barranco: pide un salto que no suba demasiado |
| En el camino del salto | Un huevo frito o un pescado sobre el barranco, donde pasaría el salto que cualquiera haría primero (cerca de su punto más alto, un poco por encima o por debajo): hay que adelantar, retrasar o medir el salto de otra manera. Lo pidió Fangusk ([ADR 0007](adr/0007-la-dificultad-velocidad-y-obstaculos-en-el-aire.md)) |
| Bloque | Un bloque sólido de 1 o 2 tiles sobre el barranco, cuyo borde de abajo alcanza la cabeza de Caro en ese mismo salto: lo corta. Desde abajo frena la subida, de costado la para en el aire y encima se puede correr. Sobre un suelo deja siempre al menos 28 px por debajo, para que se pueda pasar corriendo. También lo pidió Fangusk |

Los dos últimos, los obstáculos en el aire, empiezan cuando la dificultad pasa del 5 %. Al principio lleva uno el 35 % de
los barrancos y, a toda dificultad, el 90 %; la mitad son bloques. Cada uno tiene cuatro intentos de colocarse donde la
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
barranco no las necesita y se hace otro. Desde 250 px/s, en la mitad de esos barrancos una plataforma se mueve, de lado o
arriba y abajo; como Caro no puede pararse a esperarla, los saltos para subirse y para bajarse se prueban con ella en 6
momentos de su recorrido, y si alguno no es justo se queda quieta.

Así nunca hace falta un salto perfecto: hay margen en el momento y en la duración. Un obstáculo en el suelo se valida igual,
con 0,4 s de carrera para tomarlo. Una plataforma con premio solo entra si al menos cuatro saltos del suelo aterrizan
encima y el barranco anterior sigue siendo justo con ella. Los bloques entran en la misma simulación: el validador prueba
los saltos contra ellos con la física del juego, y un salto que se da con uno no cuenta como bueno.

Si un tramo no es justo se genera otro; tras 8 intentos sale uno fácil, un barranco de 2 tiles a la misma altura y sin
nada que reste; un barranco con plataformas que no sale justo en 8 intentos deja su sitio a un barranco de siempre. En las
pruebas, un piloto automático que salta como una persona cuidadosa (y espera, si puede, a un salto que le deje sitio para
el siguiente) recorre 39 km en 30 minutos sin caer, juega 5 partidas de 10 minutos sin caer ni tocar nada que reste, y se
generan 10.000 tramos seguidos sin un error, a 1,3 ms cada uno (GEN-10, GEN-11).

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
