# Recolectables

La ficha de cada recolectable (GDD-10). Lo que dictó Fangusk: suman los perritos y los gatitos; restan los huevos fritos
y el pescado; se empieza con 10 puntos y con 0 se acaba la partida. **Los valores son una propuesta** hasta que Fangusk
los confirme jugando el prototipo (QA-03), y viven en `files/ajustes.json` (`score`). Su aspecto final sale de sus
referencias en M2; en el prototipo en gris son cuadrados de 12 × 12 px con una letra.

## Las fichas

| | Perrito | Gatito | Huevo frito | Pescado |
|---|---|---|---|---|
| **Puntos** | +1 | +1 | −2 | −3 |
| **Dónde sale** | En filas por el suelo, en arcos sobre los barrancos y en lo alto de la elección | Igual que el perrito | Obstáculo en el suelo o techo sobre un barranco | Igual que el huevo frito |
| **Cuántos** (cada 100 m) | 7,9 a 8,4 | 7,9 a 8,9 | 1,2 al principio, 3,2 a toda velocidad | 0,8 al principio, 2,1 a toda velocidad |
| **Cómo se distingue** (propuesta) | Un animal de colores cálidos, que se mueve (el rabo) | Un animal de colores cálidos, que se mueve (las orejas o el rabo) | Comida quieta, blanca y amarilla | Comida quieta, gris azulada |
| **Al recogerlo** (M4) | Un sonido alegre y corto | El suyo, distinto del perrito | Un sonido seco | Más seco aún: es lo que más resta |
| **En el prototipo** | «P» marrón con borde verde | «G» naranja con borde verde | «H» crema con borde rojo | «F» azul con borde rojo (de *fish*: la P ya es del perrito) |

Las frecuencias están medidas en 20 partidas de 5 km ([métricas](metricas.md)). Perritos y gatitos salen a partes
iguales; de lo que resta, 6 de cada 10 son huevos fritos y 4 pescados, porque el pescado quita más.

## Las reglas para que se lean de un vistazo

A toda velocidad, un recolectable cruza la pantalla en unos 2 segundos, así que el arte final tiene que cumplir:

1. **Lo que suma y lo que resta no se confunden ni de reojo**: los que suman son animales, con colores cálidos y algo de
   movimiento; los que restan son comida quieta, con colores fríos o blancos.
2. **Contorno oscuro** en los cuatro, como Caro, para que destaquen sobre los tres escenarios.
3. **Silueta propia** en los 12 × 12 px de su caja (el dibujo puede ser algo mayor, la caja manda en el choque).
4. **Ningún parecido** con objetos de otros juegos ([propiedad intelectual](propiedad-intelectual.md)).

## Los patrones

Cómo se colocan está en [generación](generacion.md): filas en el suelo, arcos sobre los barrancos, obstáculos en el suelo
que hay que saltar, techos sobre los barrancos que piden un salto más bajo y la elección, un perrito o un gatito en lo alto
de un salto largo sobre un obstáculo. El validador garantiza que lo que resta siempre se puede esquivar con margen; en las
pruebas, un piloto automático juega 50 minutos sin tocar ninguno.

## Un ejemplo de partida

Empieza con 10. En el primer minuto recoge 12 perritos y gatitos (22) y choca con un huevo frito (20). A los 2 minutos
lleva 31 y le dan dos pescados seguidos (25). A los 1250 m cae por un barranco: su partida queda en 1250 m y 31 puntos
máximos.
