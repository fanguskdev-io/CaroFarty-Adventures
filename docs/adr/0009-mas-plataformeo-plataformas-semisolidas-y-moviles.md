# ADR-0009 · Más plataformeo: plataformas semisólidas y móviles, suelos más cortos

| | |
|---|---|
| **Estado** | aceptada: el qué lo decidió Fangusk el 2026-11-02 tras jugar la beta 4 en su Nothing, y GameMaster lo transmitió con sus palabras · propuesta: los números (largos, anchos, cada cuánto, alturas y velocidades de las móviles), que se afinan jugando |
| **Fecha** | 2026-11-02 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M5 · **Tarea:** PLY-17, ITM-20, GEN-22, ART-26 |
| **Propone · decide** | Fangusk · Fangusk |
| **Reversibilidad** | media · las reglas viven en datos (`course` en `files/ajustes.json`), pero las plataformas tocan la física, el generador, el validador y el piloto |

## Contexto

Tras jugar la beta 4, Fangusk dijo: «en general está bastante bien». Y pidió más plataformeo: «Caro pasa mucho tiempo
andando sobre suelo plano rescatando gatitos y perritos. El suelo podría estar más fraccionado e incluyendo plataformas
sobre las que saltar de forma obligatoria para salvar distancias imposibles en salto, como se hace en Super Mario».

## Decisión

Lo que eligió Fangusk:

- **Plataformas semisólidas, como en Mario**: se atraviesan saltando desde abajo y se aterriza encima. Por los lados y por
  debajo no frenan. Encima se corre como sobre el suelo.
- **También móviles, desde ya**: además de las fijas, otras que suben y bajan o van de lado, y que aparecen cuando la
  partida ya va rápida. Si Caro está encima, la llevan con ellas.
- **«Bastante más» plataformeo**: suelos más cortos, de 3 a 10 tiles en vez de hasta 20; más barrancos y más cambios de
  altura; uno de cada 2 o 3 retos con plataformas obligatorias, barrancos más anchos que el salto largo de ese momento que
  solo se cruzan saltando a 1–3 plataformas; y las calmas con perritos y gatitos siguen, pero más cortas.
- **El validador sigue garantizando que todo es justo** con la física real, también con las móviles.

Cómo, propuesta del agente ([generación](../generacion.md)):

- **La física**: una plataforma es como el techo de un bloque sin sus lados ni su parte de abajo: sostiene a Caro si sus
  pies llegan desde arriba. Una móvil que baja la lleva pegada (hasta 2 px por paso) y una que va de lado le suma su
  velocidad. Su movimiento va y viene suavizado en los extremos, con solo sumas y productos, así que es idéntico en todos
  los teléfonos. El recorrido lleva su propio reloj, y el validador y el piloto simulan los saltos sobre ese mismo reloj.
- **Los suelos**: de lo justo para aterrizar con margen y volver a despegar (4 tiles al empezar, 8 a toda velocidad) a 10
  tiles. La carrera que se pide tras aterrizar baja de 0,3 a 0,2 s, la misma ventana en la que se prueban los despegues.
- **Los barrancos con plataformas**: uno de cada 2 o 3 retos, con 1 a 3 plataformas, más a menudo varias con la
  dificultad; el barranco, más ancho que el salto más largo de ese momento más un tile; cada salto, justo con la regla de
  siempre; y si algún salto llega al otro lado sin ellas, se hace otro.
- **Las plataformas con premio** de la [ADR 0008](0008-perritos-que-caminan-y-gatitos-de-ocho-pelajes.md) pasan a ser
  plataformas semisólidas, con los mismos tiles. Era una propuesta del agente que cambiaba una decisión anterior de
  Fangusk (el mismo bloque que los obstáculos); GameMaster se la explicó (rescatar en una plataforma será algo más fácil y
  el reto queda en los barrancos) y la aceptó: «Vale, acepto tu recomendación». Los bloques sólidos del aire de la
  [ADR 0007](0007-la-dificultad-velocidad-y-obstaculos-en-el-aire.md) se quedan como estaban.
- **Las calmas**: de 2 a 4 piezas en vez de 3 a 8, con suelos llanos de 6 a 10 tiles.
- **Las móviles**: desde 250 px/s, en la mitad de los barrancos con plataformas, una de ellas se mueve de 1 a 3 tiles de
  lado o arriba y abajo, ida y vuelta en 2,4 a 3,6 s. Como Caro no puede pararse a esperarla, el validador prueba los
  saltos para subirse y para bajarse con la plataforma en 6 momentos de su recorrido; si alguno no es justo, se queda
  quieta. Nunca lleva un animal encima, que se quedaría en el aire.
- **El arte**: los tiles `plataforma-izq`, `plataforma` y `plataforma-der` de cada escenario, de 16 × 16, con la cara de
  arriba que se pisa, y `plataforma-movil-izq`, `plataforma-movil` y `plataforma-movil-der` para las que se mueven
  (nombres de GameMaster). Las dibujó GameMaster: un tablón con musgo en el Bosque, con nieve y carámbanos en la Nieve y
  atado con cuerdas en el Desierto; y, para las móviles, una hoja dorada que aletea, un trineo con destellos y una
  alfombra voladora con flecos, de dos fotogramas. Sin su arte, el juego las pinta como una tabla gris.

## Alternativas

- **Plataformas sólidas, como los bloques**: descartada por Fangusk, que eligió las semisólidas.
- **Solo fijas, las móviles más adelante**: descartada por Fangusk, «también móviles desde ya».

## Consecuencias

- El piloto automático tiene que saber saltar a las plataformas y elegir saltos que dejen sitio para el siguiente; con
  él, las pruebas largas siguen sin caer (39 km en 30 minutos) y el generador hace cada pieza en 1,3 ms.
- Las móviles piden que el validador pruebe que se cruzan lleguen cuando lleguen, porque Caro no puede pararse a
  esperarlas (ITM-20). Hacer y comprobar cada pieza cuesta más: 1,4 ms en la JVM y unos 40 en la versión de pruebas de
  iOS, sin optimizar; se hacen en segundo plano y la partida usa una por segundo, más o menos.
