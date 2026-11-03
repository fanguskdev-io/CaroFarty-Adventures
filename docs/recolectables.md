# Recolectables

La ficha de cada recolectable (GDD-10). Lo que dictó Fangusk: suman los perritos y los gatitos; restan los huevos fritos,
el pescado y, desde la beta 9, los pájaros; se empieza con 5 puntos y con 0 se acaba la partida. **Los valores los aceptó
Fangusk** después de jugar el prototipo: «con respecto a los recolectables acepto tu propuesta» (2026-11-01), y tras la
beta 7 los endureció: 5 puntos al empezar en vez de 10, el huevo a −3 y el pescado a −5, y los pájaros a −8
([ADR 0011](adr/0011-mas-dificultad-tras-la-beta-7.md)). Viven en `files/ajustes.json` (`score`). Su aspecto lo
diseñaron Fangusk y GameMaster en M2, en `arte/recolectables/` ([arte](arte.md)); sin ese arte, el juego los dibuja como
en el prototipo en gris, cuadrados de 12 × 12 px con una letra.

## Las fichas

| | Perrito | Gatito | Huevo frito | Pescado | Pájaro |
|---|---|---|---|---|---|
| **Puntos** | +1 | +1 | −3 | −5 | −8 (Fangusk, tras la beta 7) |
| **Dónde sale** | En filas por el suelo y encima de las plataformas con premio, nunca en el aire | Igual que el perrito | Obstáculo en el suelo, techo sobre un barranco o en el aire, en el camino de un salto; algunos flotan arriba y abajo (ENM-17) | Igual que el huevo frito, y en el Bosque y la Nieve también saltando del agua de los barrancos (ENM-15) | En el aire sobre los barrancos, desde la mitad de la dificultad: de frente o paseando (ENM-18) |
| **Cuántos** (cada 100 m) | 2,1 al principio, 0,6 a toda dificultad (7,1 a 8,8 con la beta 7) | 1,8 al principio, 0,5 a toda dificultad (7,5 a 8,9 con la beta 7) | 0,6 al principio, 1,9 a toda dificultad | 0,3 al principio, 1,6 a toda dificultad | 0,2 desde los 2 km |
| **Caja de choque** | 16 × 16 px, generosa | 16 × 16 px, generosa | 10 × 10 px, estricta | 10 × 10 px, estricta | 10 × 10 px, estricta |
| **Se mueve** (ADR 0008) | En el suelo, pasea por su tramo o se queda quieto; nunca se cae por un barranco | Quieto, sentado | Quieto, o flota | Quieto, flota o salta del agua | Siempre: vuela hacia ella, recto u ondulando, o va y viene y se da la vuelta (ADR 0011) |
| **Variantes** (ADR 0008) | Nueve razas al azar: shiba, pastor alemán, carlino, dálmata, golden, beagle, yorkshire, bulldog francés y corgi | Ocho pelajes al azar: naranja, gris azulado, calicó, crema, negro, blanco, chocolate y siamés | — | — | Uno por escenario: cuervo en el Bosque, frailecillo en la Nieve y buitre en el Desierto |
| **Aspecto** (M2) | De perfil con la cabeza en tres cuartos; camina o se para | Una hogaza sentada, con su reposo animado | Chisporrotea, con ondas de mal olor encima | Colea, con ondas de mal olor encima | Aletea de frente; el que pasea planea y aletea para girar |
| **Caro al cogerlo** (CHR-07) | Cara de contenta, 0,4 s | Igual | Cara de asco, 0,4 s, un retroceso de 0,3 s y 1 s parpadeando en el que los huevos y pescados no restan | Igual | Igual |
| **Cómo se distingue** (propuesta) | Un animal de colores cálidos, que se mueve (el rabo) | Un animal de colores cálidos, que se mueve (las orejas o el rabo) | Comida quieta, blanca y amarilla | Comida quieta, gris azulada | Un ave oscura que vuela |
| **Al recogerlo** (M4) | Un sonido alegre y corto | El suyo, distinto del perrito | Un sonido seco | Más seco aún | El del pescado, hasta que tenga el suyo |
| **En el prototipo** | «P» marrón con borde verde, que se mueve | «G» con borde verde, del color de su pelaje | «H» crema con borde rojo | «F» azul con borde rojo (de *fish*: la P ya es del perrito) | «A» gris con borde rojo |

Las frecuencias están medidas en 20 partidas de 5 km ([métricas](metricas.md)). Perritos y gatitos salen a partes
iguales; de lo que resta, la mitad son huevos fritos, 4 de cada 10 pescados y el resto pájaros, que son lo que más quita.

## Las reglas para que se lean de un vistazo

A toda velocidad, un recolectable cruza la pantalla en unos 2 segundos, así que el arte final tiene que cumplir:

1. **Lo que suma y lo que resta no se confunden ni de reojo**: los que suman son animales, con colores cálidos y algo de
   movimiento; los que restan son comida quieta, con colores fríos o blancos.
2. **Contorno oscuro** en los cuatro, como Caro, para que destaquen sobre los tres escenarios.
3. **Silueta propia**, de al menos 10 × 10 px en un dibujo de 16 × 16. Las cajas de choque perdonan a favor de ella:
   generosas para lo que suma y estrictas para lo que resta (guía 1 de GameMaster).
4. **Ningún parecido** con objetos de otros juegos ([propiedad intelectual](propiedad-intelectual.md)).

## Los patrones

Cómo se colocan está en [generación](generacion.md): filas en el suelo, plataformas con un premio encima, obstáculos en
el suelo que hay que saltar, techos sobre los barrancos que piden un salto más bajo y huevos y pescados en el camino del
salto que cualquiera haría primero. Los animales siempre están posados, en el suelo o en una plataforma; en el aire solo
hay lo que resta. El validador garantiza que lo que resta siempre se puede esquivar con margen; en las
pruebas, un piloto automático juega 50 minutos sin tocar ninguno.

## Un ejemplo de partida

Empieza con 5. En el primer minuto recoge 12 perritos y gatitos (17) y choca con un huevo frito (14). A los 2 minutos
lleva 23 y le dan dos pescados seguidos (13). A los 1250 m cae por un barranco: su partida queda en 1250 m y 23 puntos
máximos.
