# CaroFarty · página de diseño

> **Pendiente de que Fangusk la valide** (GDD-01): es el criterio de salida de M0. Lo marcado como propuesta lo decidió el
> agente y se puede cambiar.

## Visión

Un plataformas infinito en pixel art que Fangusk le regala a su esposa: Caro corre sola y ella solo tiene que saltar.
Sin anuncios ni conexión ([ADR 0005](adr/0005-un-regalo-sin-conexion.md)).

Los pilares, que deciden qué entra y qué no:

1. **Saltar se siente bien**: el salto responde al instante y obedece al dedo.
2. **Partidas cortas y rejugables**: de abrir la app a jugar, un toque; de perder a volver a jugar, menos de 2 s.
3. **El encanto de Super Mario World, con arte y música propios** ([guía de propiedad intelectual](propiedad-intelectual.md)).
4. **Nunca aburre**: ni el recorrido ni la música se repiten igual.

## Lo que pidió Fangusk (2026-11-01)

1. El juego va a ser un plataformas horizontal infinito, al estilo del videojuego del dinosaurio de Google Chrome. El
   personaje recogerá ciertos objetos que le sumarán puntos y otros le restarán puntos; si estos puntos llegan a 0 o el
   personaje cae por un barranco por un mal salto, se acaba la partida.
2. Habrá inicialmente 3 escenarios, uno de bosque, otro de nieve y otro de desierto. Los escenarios deben basarse en los
   del videojuego Super Mario World, de Super Nintendo.
3. El estilo del videojuego será pixel art (como Super Mario World).
4. El personaje correrá solo tras el tap inicial y la única acción que hará será saltar cuando el usuario haga tap en la
   pantalla; la altura y longitud del salto vendrán definidas por la presión o el tiempo que el usuario mantenga el tap.
5. Hay que diseñar a la protagonista, Caro, y los recolectables.
6. Los recolectables que suman serán perritos y gatitos.
7. Los recolectables que restan serán huevos fritos y pescado.
8. Hay que crear una música dinámica acorde al escenario y teniendo en cuenta que no puede aburrir, porque la partida
   puede ser «infinita».

Y en sus respuestas del mismo día: para ella ahora y quizá publicarlo después; se empieza con 10 puntos; dos récords, la
distancia y los puntos; sin fecha de entrega.

## El bucle de juego

Tocar para empezar → Caro corre sola → saltar los barrancos y elegir qué recoger → la partida acaba al quedarse sin puntos
o al caer → se ven los récords → un toque y otra partida.

## Las reglas

| Regla | Valor | Estado |
|---|---|---|
| Puntos al empezar | 10 | Fangusk |
| Perrito | +1 | Propuesta, a confirmar jugando el prototipo |
| Gatito | +1 | Propuesta, a confirmar jugando el prototipo |
| Huevo frito | −2 | Propuesta, a confirmar jugando el prototipo |
| Pescado | −3 | Propuesta, a confirmar jugando el prototipo |
| Fin de la partida | Los puntos llegan a 0 (nunca bajan de 0) o Caro cae por un barranco | Fangusk |
| Récords | La distancia recorrida (1 tile = 1 m) y los puntos máximos alcanzados en la partida | Fangusk |

Un ejemplo: empieza con 10; coge seis perritos y gatitos (16) y choca con un pescado (13); a los 742 m cae por un barranco.
Su partida queda en 742 m y 16 puntos máximos, y cada récord se compara por separado.

Lo que no está en las especificaciones no entra: ni vidas, ni enemigos, ni power-ups, ni monedas (siguen aparcados en
el backlog por si Fangusk los quiere).

## Cómo se controla

Un solo gesto ([ADR 0002](adr/0002-salto-por-tiempo-de-pulsacion.md), propuesta): el primer toque arranca la carrera; cada
toque siguiente es un salto que empieza al apoyar el dedo, y mantenerlo apoyado lo hace más alto y más largo. Se mide el
tiempo y no la presión, porque los iPhone actuales no la miden y en Android no es fiable. Los números están en
[movimiento](movimiento.md).

## La pantalla

En horizontal, pixel art con tiles de 16 px, un área mínima de 320×180 escalada por un número entero y la cámara que
sigue a Caro con desplazamiento suave ([ADR 0003](adr/0003-horizontal-y-pixel-perfecto.md), propuesta).

## El recorrido

Infinito, generado por reglas por delante de Caro y comprobado por un validador de saltos antes de aparecer: nunca hay un
salto imposible ([ADR 0004](adr/0004-generacion-por-reglas-con-validador.md), propuesta;
[especificación](generacion.md)). La dificultad sube con la distancia, con respiros cada cierto tiempo.

## Los escenarios

Bosque → Nieve → Desierto → Bosque…, cambiando cada 600 m (alrededor de un minuto de carrera) con un tramo de
transición. Toman el estilo de Super Mario World; como el juego no tiene un mundo de nieve ni uno de desierto propiamente
dichos, se aplica su estilo a esos temas. El arte se diseña a partir de las referencias de Fangusk (M2).

## La música

Música generativa y adaptativa por escenario (M4): compuesta por reglas a partir de motivos propios, con variaciones para
que nunca suene igual, capas que entran con la velocidad y transiciones al cambiar de escenario. Se sintetiza en el
propio teléfono con sonido de 16 bits (instrumentos al estilo de la Super Nintendo y su eco característico) y es
composición original.

## Alcance y orden de trabajo

La primera versión son las 8 especificaciones; el resto queda fuera hasta que Fangusk decida. El orden, con sus criterios
de salida en [proceso](proceso.md):

| Hito | Qué |
|---|---|
| M0 | Esta página y las decisiones (ADR) |
| M1 | Prototipo en gris en Android e iOS: carrera, salto variable, barrancos, recolectables, fin de partida y generación validada |
| M2 | Caro, los recolectables y el Bosque con el arte final |
| M3 | La Nieve, el Desierto y las transiciones |
| M4 | Música dinámica y efectos |
| M5 | Menús, récords, pausa, ajustes, icono e instalación en sus teléfonos |

## Pendiente de Fangusk

- Validar esta página y aceptar o cambiar las propuestas de los ADR 0002 a 0004.
- Confirmar los valores de los recolectables cuando juegue el prototipo.
- Los teléfonos donde se jugará y la cuenta de Apple, al llegar a la instalación (TEC-13).
- Las referencias de Caro, los recolectables y los escenarios, al empezar M2.

## Documentos

- [Decisiones (ADR)](adr/README.md)
- [Movimiento](movimiento.md) · [Generación del recorrido](generacion.md) · [Proceso y criterios de salida](proceso.md)
- [Dispositivos de prueba](dispositivos.md) · [Propiedad intelectual](propiedad-intelectual.md)
- El backlog compartido: `../backlog-juego-plataformas` (lo creó la sesión GameMaster; el estado de cada tarea se lleva allí)
