# ADR-0007 · La dificultad: una velocidad que sigue subiendo y obstáculos en el aire

| | |
|---|---|
| **Estado** | aceptada: el qué lo dictó Fangusk el 2026-11-01 al jugar el prototipo en gris · propuesta: los números de la velocidad y cómo se colocan los obstáculos, que se afinan jugando |
| **Fecha** | 2026-11-01 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M1 · **Tarea:** GDD-08, GDD-19 |
| **Propone · decide** | Fangusk · Fangusk |
| **Reversibilidad** | alta · la velocidad vive en datos (TEC-07) y los obstáculos son patrones del generador |

## Contexto

El prototipo en gris corría de 120 a 200 px/s. Fangusk lo jugó el 2026-11-01: «En la primera version el juego es
sencillo, basicamente lo que buscabamos pero se hace lento, parece un paseo, estaría mejor que la velocidad fuese subiendo
conforme avanza el escenario para darle dificultad». Y propuso más: «Creo que una buena idea para añadirle dificultad es
incorporar obstáculos en el aire que dificulten el salto y hagan que el usuario deba tener un mejor timing».

Las especificaciones dejan fuera los enemigos y fijan cómo se pierde: con 0 puntos o cayendo por un barranco
([diseño](../diseno.md)).

## Decisión

- **La velocidad sube durante toda la partida.** Empieza a 160 px/s, sube deprisa el primer minuto (277 px/s al minuto) y
  después cada vez más despacio hacia 360, que no llega a alcanzar. A tope deja 0,6 s para reaccionar en la pantalla más
  estrecha, como el dinosaurio de Chrome. Los barrancos se miden en tiempo de carrera, así que crecen con ella y el
  validador los sigue comprobando con el mismo salto ([movimiento](../movimiento.md), [métricas](../metricas.md)).
- **Obstáculos en el aire, de dos clases** (Fangusk: «La opcion 1 y 2 son buenas»):
  1. **Huevos fritos y pescados en el camino del salto**: un salto a destiempo los toca y resta puntos. Son los mismos
     recolectables que restan, colocados donde pasaría el salto más evidente, de modo que hay que adelantarlo, retrasarlo
     o medirlo de otra manera.
  2. **Bloques sólidos que frenan el salto**: si Caro se da con uno desde abajo, deja de subir y cae; si choca con su
     costado en el aire, se queda pegada y cae, y si debajo hay un barranco se acaba la partida. Encima de un bloque se
     puede correr.
- **El validador comprueba los dos** con la misma física del juego: cada pieza sigue teniendo saltos que la cruzan con
  margen en el momento y en la duración (3 momentos de despegue con 4 pulsaciones buenas cada uno).

## Alternativas

- **Solo subir el tope a 280–320 px/s**, como proponía la guía 1 de GameMaster. Descartada: Fangusk pidió que la velocidad
  siga subiendo mientras se avanza.
- **Algo que vuela y acaba la partida**, como el pájaro del dinosaurio. Fangusk no lo eligió, y cambiaría cómo se pierde.

## Consecuencias

- Tareas nuevas en el backlog: el diseño de los obstáculos en el aire (GDD-19), el choque con los bloques (PLY-16), su
  colocación en el generador (GEN-18) y su dibujo en cada escenario (ART-25, en M2, con el resto del arte).
- La corrección de esquinas (PLY-05) gana sentido con los bloques: rozar uno por muy poco no debería frenar el salto.
- Los números se afinan jugando (PLY-15) en `files/ajustes.json`.
