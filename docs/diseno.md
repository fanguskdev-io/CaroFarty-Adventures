# CaroFarty · página de diseño

> **Validada por Fangusk el 2026-11-01**: «tienes mi ok para M0». Lo que sigue marcado como propuesta (los valores de los
> recolectables y los récords por escenario) espera a que lo confirme.

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
distancia y los puntos; sin fecha de entrega. Y una aclaración: «Vale una pequeña aclaración sobre los escenarios, no tienen
que ser el mismo y que vaya cambiando cada 600 metros. Son 3 escenarios distintos, el usuario elige donde jugar sin en
Bosque, Nieve o Desierto» ([ADR 0006](adr/0006-tres-escenarios-a-elegir.md)).

## El bucle de juego

Elegir dónde jugar (Bosque, Nieve o Desierto) → tocar para empezar → Caro corre sola → saltar los barrancos y elegir qué
recoger → la partida acaba al quedarse sin puntos o al caer → se ven los récords → un toque y otra partida, en el mismo
escenario o en otro.

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
| Récords por escenario o generales | Uno de distancia y uno de puntos por escenario | Propuesta, pendiente de Fangusk |

Un ejemplo: empieza con 10; coge seis perritos y gatitos (16) y choca con un pescado (13); a los 742 m cae por un barranco.
Su partida queda en 742 m y 16 puntos máximos, y cada récord se compara por separado.

Lo que no está en las especificaciones no entra: ni vidas, ni enemigos, ni power-ups, ni monedas (siguen aparcados en
el backlog por si Fangusk los quiere).

## El fin de la partida

Las reglas son de Fangusk: se acaba al llegar a 0 puntos o al caer por un barranco, sin vidas ni segunda oportunidad. Lo
que se ve y se oye es propuesta (GDD-14); el prototipo en gris ya hace lo que se ve:

1. **Al instante**: el mundo se para. Si cae, Caro sale por debajo de la pantalla; si se queda sin puntos, se para en
   seco. Con el arte (M2), una animación para cada caso; con el sonido (M4), un efecto corto distinto para cada causa y la
   música que se apaga.
2. **El panel**: «FIN DE LA PARTIDA», la causa («TE CAÍSTE POR UN BARRANCO» o «TE QUEDASTE SIN PUNTOS»), la distancia y
   los puntos máximos de la partida al lado de los récords del escenario y, si ha batido alguno, «¡NUEVO RÉCORD!» (con
   su fanfarria en M4).
3. **Otra partida**: un toque, en cuanto pasan 0,6 s (para que un salto que llega tarde no la empiece sin querer). Arranca
   ya corriendo y en el mismo escenario: del fallo a volver a jugar, menos de 2 s. Desde el panel se podrá cambiar de
   escenario cuando exista la pantalla para elegirlo (M3).

## Cómo se controla

Un solo gesto ([ADR 0002](adr/0002-salto-por-tiempo-de-pulsacion.md)): el primer toque arranca la carrera; cada
toque siguiente es un salto que empieza al apoyar el dedo, y mantenerlo apoyado lo hace más alto y más largo. Se mide el
tiempo y no la presión, porque los iPhone actuales no la miden y en Android no es fiable. Los números están en
[movimiento](movimiento.md).

## La pantalla

En horizontal, pixel art con tiles de 16 px, un área mínima de 320×180 escalada por un número entero y la cámara que
sigue a Caro con desplazamiento suave ([ADR 0003](adr/0003-horizontal-y-pixel-perfecto.md)).

## El recorrido

Infinito, generado por reglas por delante de Caro y comprobado por un validador de saltos antes de aparecer: nunca hay un
salto imposible ([ADR 0004](adr/0004-generacion-por-reglas-con-validador.md);
[especificación](generacion.md)). La dificultad sube con la distancia, con respiros cada cierto tiempo.

## Los escenarios

Tres escenarios distintos, Bosque, Nieve y Desierto, y ella elige en cuál jugar antes de cada partida. La partida entera,
por larga que sea, transcurre en el escenario elegido, con su decorado, su paleta y su música
([ADR 0006](adr/0006-tres-escenarios-a-elegir.md), dictado por Fangusk). En la primera versión los tres comparten las
reglas del recorrido; una particularidad jugable de alguno sería una decisión de Fangusk.

Toman el estilo de Super Mario World; como el juego no tiene un mundo de nieve ni uno de desierto propiamente dichos, se
aplica su estilo a esos temas. El arte se diseña a partir de las referencias de Fangusk (M2).

## La música

Música generativa y adaptativa por escenario (M4): compuesta por reglas a partir de motivos propios, con variaciones para
que nunca suene igual y capas que entran con la velocidad, una música para cada escenario. Se sintetiza en el
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
| M3 | La Nieve, el Desierto y la pantalla para elegir escenario |
| M4 | Música dinámica y efectos |
| M5 | Menús, récords, pausa, ajustes, icono e instalación en sus teléfonos |

## Pendiente de Fangusk

- Confirmar los valores de los recolectables cuando juegue el prototipo.
- Si los récords van por escenario o son uno general (propuesta: por escenario).
- Los teléfonos donde se jugará y la cuenta de Apple, al llegar a la instalación (TEC-13).
- Las referencias de Caro, los recolectables y los escenarios, al empezar M2.

## Documentos

- [Decisiones (ADR)](adr/README.md)
- [Movimiento](movimiento.md) · [Métricas y curva de dificultad](metricas.md) · [Generación del recorrido](generacion.md)
- [Recolectables](recolectables.md) · [Escenarios](escenarios.md) · [Proceso y criterios de salida](proceso.md)
- [Pruebas](pruebas.md) · [Lista de bugs](bugs.md) · [Rendimiento](rendimiento.md) · [Cómo se entrega el arte](arte.md)
- [Dispositivos de prueba](dispositivos.md) · [Propiedad intelectual](propiedad-intelectual.md)
- El backlog compartido: `../backlog-juego-plataformas` (lo creó la sesión GameMaster; el estado de cada tarea se lleva allí)
