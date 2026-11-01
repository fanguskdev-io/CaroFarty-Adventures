# Escenarios

La ficha de cada escenario (GDD-12). Lo que dictó Fangusk: tres escenarios distintos, Bosque, Nieve y Desierto, basados en
los de Super Mario World, y ella elige en cuál jugar antes de cada partida
([ADR 0006](adr/0006-tres-escenarios-a-elegir.md)). Todo lo demás es **propuesta**: el arte sale de las referencias que
Fangusk pasará al empezar M2 (PRD-11), siempre dibujado desde cero ([propiedad intelectual](propiedad-intelectual.md)).

## Lo que comparten

- **Las reglas del recorrido**: las mismas en los tres ([generación](generacion.md)); cambian las piezas de arte, la paleta,
  los fondos y la música (GEN-08). Una particularidad jugable de un escenario la decidiría Fangusk.
- **Las capas**, de atrás adelante: el cielo, un fondo lejano que se mueve al 30 % de la velocidad, uno medio al 60 % y el
  suelo en tiles de 16 px con sus detalles. Los recolectables y Caro, siempre por delante y con contorno oscuro para que se
  lean sobre cualquier fondo.
- **Una música por escenario** (M4), que no se repite igual en una partida larga.
- **Los récords**: uno de distancia y otro de puntos por escenario (propuesta pendiente de Fangusk).

## Bosque

| | |
|---|---|
| De Super Mario World | El estilo de sus bosques: copas redondas y frondosas, luz verdosa, un fondo lejano de árboles en capas |
| Ambiente (propuesta) | Un bosque alegre de día: árboles de copa redonda, troncos, arbustos y flores; barrancos con raíces en los bordes |
| Paleta (propuesta) | Verdes vivos y marrones cálidos, cielo claro; en el prototipo, la gama gris verdosa |
| Música (propuesta) | La más alegre y saltarina de las tres: tonalidad mayor, unos 140 pulsos por minuto, flautas y marimbas |
| Particularidad jugable | Ninguna en la primera versión |
| Cuándo | M2, el primero con el arte final, junto a Caro y los recolectables |

## Nieve

| | |
|---|---|
| De Super Mario World | No tiene un mundo de nieve, solo algunos niveles con hielo: se aplica su estilo (contornos, pocos tonos, colores saturados) a un paisaje nevado. Para el tema, no para el dibujo, Super Mario Bros. 3 tiene un mundo de hielo y Super Mario Maker 2 un estilo de nieve (guía 1 de GameMaster) |
| Ambiente (propuesta) | Pinos nevados, montañas al fondo, copos que caen despacio y suelo de nieve con bordes de hielo |
| Paleta (propuesta) | Blancos azulados, azules y algún acento cálido para que Caro y los recolectables destaquen; en el prototipo, la gama gris azulada |
| Música (propuesta) | Más tranquila y brillante: unos 120 pulsos por minuto, campanillas y celesta, con el eco largo de la época |
| Particularidad jugable | Ninguna en la primera versión. Si Fangusk la quisiera, una idea sería hielo que resbala, sin tocar el validador |
| Cuándo | M3 |

## Desierto

| | |
|---|---|
| De Super Mario World | Tampoco tiene un desierto: se aplica su estilo a dunas, rocas y cactus. Para el tema, Super Mario Bros. 3 tiene un mundo de desierto y Super Mario Maker 2 un estilo de desierto |
| Ambiente (propuesta) | Dunas al fondo, cactus y rocas, un sol grande y la calima del horizonte |
| Paleta (propuesta) | Arenas, ocres y naranjas con un cielo turquesa; en el prototipo, la gama gris arena |
| Música (propuesta) | Con más percusión y una escala con sabor propio: unos 130 pulsos por minuto, cuerdas pulsadas y tambores de mano |
| Particularidad jugable | Ninguna en la primera versión. Una idea, si Fangusk la quisiera: arenas que frenan un poco al aterrizar |
| Cuándo | M3, con la pantalla para elegir escenario |

## En el prototipo en gris (M1)

Los tres existen ya con la misma lógica y una paleta gris distinta cada uno. La versión de depuración deja cambiar de
escenario antes de empezar tocando la esquina inferior izquierda con las herramientas de depuración abiertas; la pantalla
para elegirlo llega en M3 (UI-20).
