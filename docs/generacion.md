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
- **Recolectables**: colocados con un patrón.

En la primera versión la altura del suelo solo cambia al otro lado de un barranco: no hay paredes en mitad del suelo
contra las que chocar. Chocar contra el borde de un suelo más alto tras un salto corto es caer al barranco.

## Las reglas del terreno

| Regla | Valor inicial |
|---|---|
| Largo de una tirada de suelo | De 3 a 20 tiles |
| Ancho de un barranco | De 2 tiles hasta una fracción de la longitud de la pulsación larga a la velocidad de ese momento ([movimiento](movimiento.md)): el 45 % al empezar, hasta el 75 % |
| Cambio de altura al otro lado de un barranco | Subida de hasta 2 tiles; bajada de hasta 3 |
| Tensión y calma | Tras 150 a 250 m de tensión, de 3 a 8 piezas tranquilas (unos 10 s): suelo llano o el barranco más estrecho a la misma altura, con perritos y gatitos y nada que reste |
| Repetición | Nunca tres piezas difíciles iguales seguidas: mismo barranco, mismo escalón y lo que resta en el mismo sitio |
| Inicio de la partida | 30 tiles llanos, con una fila de perritos y gatitos, antes del primer barranco |

## Los patrones de recolectables

| Patrón | Qué pide |
|---|---|
| Fila en el suelo | Perritos y gatitos que se recogen corriendo |
| Arco sobre un barranco | Perritos y gatitos a lo largo de un salto medio: premian el salto bien medido |
| Obstáculo en el suelo | Un huevo frito o un pescado en el camino que hay que saltar, como los cactus del dinosaurio |
| Elección | Un perrito o un gatito en lo alto de un salto largo sobre un huevo frito o un pescado del suelo: un toque rápido salva el obstáculo y pasa por debajo; mantener el dedo lo recoge. La lleva la mitad de los obstáculos |
| Techo | Un huevo frito o un pescado alto sobre un barranco: pide un salto que no suba demasiado |

Los que restan aparecen más a medida que sube la dificultad; las cifras medidas y la ficha de cada recolectable están en
[recolectables](recolectables.md).

## El validador

Antes de que un tramo entre en el mundo, el validador simula los saltos con el mismo código de física del juego
(`JumpLab`): cada momento de despegue de los últimos 0,2 s de carrera antes del borde y del coyote time, de fotograma en
fotograma, con cada duración de pulsación. Un barranco es **justo** si al menos 3 de esos momentos tienen al menos 4
pulsaciones distintas que:

1. aterrizan al otro lado con al menos 1 tile de margen;
2. dejan por delante al menos 0,3 s de carrera para preparar el siguiente salto;
3. no tocan nada que reste.

Así nunca hace falta un salto perfecto: hay margen en el momento y en la duración. Un obstáculo en el suelo se valida igual,
con 0,4 s de carrera para tomarlo. Los arcos se colocan sobre uno de los saltos buenos y la elección en lo alto de uno
largo, así que se pueden recoger por construcción.

Si un tramo no es justo se genera otro; tras 8 intentos sale uno fácil, un barranco de 2 tiles a la misma altura y sin
nada que reste. En las pruebas, un piloto automático que salta como una persona cuidadosa recorre más de 20 km sin caer y
se generan 10.000 tramos seguidos sin un error (GEN-10, GEN-11).

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
