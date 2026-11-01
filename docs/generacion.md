# Generación del recorrido

Especificación del recorrido infinito (GDD-07) que siguen GEN-01 a GEN-11. La decisión y sus alternativas están en el
[ADR 0004](adr/0004-generacion-por-reglas-con-validador.md).

## Objetivo

Un recorrido sin fin que parezca diseñado: con ritmo (tensión y respiro), que no se repita y que siempre sea justo. Nunca
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
| Respiro | Cada 150 a 250 m, un tramo llano de al menos 12 tiles sin nada que reste |
| Inicio de la partida | 30 tiles llanos antes del primer barranco |

## Los patrones de recolectables

| Patrón | Qué pide |
|---|---|
| Fila en el suelo | Perritos y gatitos que se recogen corriendo |
| Arco sobre un barranco | Perritos y gatitos a lo largo de un salto medio: premian el salto bien medido |
| Obstáculo en el suelo | Un huevo frito o un pescado en el camino que hay que saltar, como los cactus del dinosaurio |
| Elección | Un perrito alto y un pescado bajo, o al revés: hay que decidir cuánto mantener el dedo |
| Techo | Un huevo frito alto sobre un barranco corto: pide un salto corto |

La frecuencia de cada patrón cambia con la dificultad; los valores y la frecuencia de cada recolectable se cierran en
GDD-10.

## El validador

Antes de que un tramo entre en el mundo, el validador simula los saltos con el mismo código de física del juego: todas las
duraciones de pulsación, de fotograma en fotograma, desde todos los puntos de despegue del borde. El tramo vale si:

1. todos sus barrancos se pueden cruzar aterrizando con al menos 1 tile de margen;
2. todos los recolectables que restan se pueden esquivar con algún salto que también cruce;
3. los recolectables de los arcos se pueden recoger con algún salto.

Si no vale se genera otro; tras 8 intentos sale un tramo llano y seguro. El mismo validador recorre en las pruebas miles de
tramos con semillas distintas (GEN-10, GEN-11).

## La dificultad

Crece con la distancia: la velocidad ([movimiento](movimiento.md)), la fracción del salto largo que piden los barrancos y
los recolectables que restan cada 100 m (de 2 al empezar a 8). Los respiros se mantienen siempre. La curva fina se cierra
en GDD-08.

## Los escenarios

Ella elige el escenario antes de cada partida y el recorrido se genera entero en él, sin transiciones
([ADR 0006](adr/0006-tres-escenarios-a-elegir.md)): el generador recibe el escenario y usa sus piezas, su paleta y sus
fondos (GEN-08). En la primera versión los tres comparten estas reglas de terreno; una mecánica propia por escenario
sería una decisión de Fangusk, y entraría como reglas o patrones de ese escenario sin tocar el validador.

## Semilla y memoria

- Dos fuentes de azar con semilla, terreno y recolectables (GEN-05): la misma semilla da la misma partida en cualquier
  teléfono.
- Se genera hasta dos pantallas por delante de la cámara y se recicla lo que queda atrás, sin crear objetos en cada
  fotograma (TEC-17).
- Las coordenadas del mundo crecen sin fin: se recolocan cerca del origen cada cierto tiempo (GEN-09).
