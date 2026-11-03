# Música y sonidos

Lo que suena y cuándo. La música, los jingles y los efectos los diseñó GameMaster (`../backlog-juego-plataformas/diseno/
musica/`) y Fangusk los aprobó al oírlos el 2026-11-02: «me gustan los sonidos, tienes mi ok». En las partidas, la
música se hace en el teléfono mientras se juega, con el motor de GameMaster portado a Kotlin ([música](musica.md)): una
canción distinta en cada partida, con capas que entran con la velocidad. Alrededor de las partidas, los bucles de la
beta. Cuándo suena cada cosa es propuesta mía, para que Fangusk la cambie al oírla.

## Cuándo suena cada cosa

| Momento | Qué suena |
|---|---|
| Pantalla inicial, récords y cómo se juega | El bucle de la última fase elegida |
| Pulsar un botón: récords, cómo se juega, una pestaña, una página, la pausa, el sonido | `ui-pulsar` |
| Reanudar tras la pausa o volver al inicio | `ui-volver`; al reanudar, la música sigue donde estaba, y al volver, la de la fase desde su principio |
| Empezar una partida | La música se calla, suena `jingle-inicio` (2,6 s) y, al acabar, empieza la música de la partida: la canción que elige su semilla, al nivel 1, con el ambiente del escenario debajo |
| Durante la partida | El ambiente (AUD-17, de GameMaster): pájaros en el Bosque, viento en la Nieve y viento seco en el Desierto, en bucle, unos 16 dB por debajo de la música. Va en la música: se apaga con ella y se pausa con ella. Entra en 2 s y se va en 0,8 s desde el fin de la partida, antes del jingle |
| Correr más deprisa | La música sube de nivel con la velocidad (GameMaster, AUD-04): el 2 a 200 px/s, el 3 a 240, el 4 a 280 (entra la melodía) y el 5 a 320; cada capa entra en el compás siguiente |
| Quedarse con 3 puntos o menos | La propia música toca el aviso en el pulso siguiente y su versión tensa desde el compás siguiente; al recuperarse, vuelve |
| Saltar | `salto-corto`, en el mismo instante del despegue, y si a los 100 ms sigue con el dedo, `salto-largo` encima: la cola que sigue subiendo (Fangusk, opción C) |
| Aterrizar | `aterrizaje` |
| Rescatar un perrito o un gatito | `rescate-perrito` o `rescate-gatito`, un grado más alto de la pentatónica con cada rescate seguido (la racha de GameMaster: perritos y gatitos juntos; se rompe con un huevo o un pescado, con más de 2 s sin rescatar o al acabar la partida; tope en el quinto, una octava arriba) |
| Coger un huevo o un pescado | `huevo` o `pescado` |
| Tocar un pájaro (ENM-18) | `pajaro`: un «puf» de plumas, un golpe grave y un «¡cuac!» nasal, unos 3 dB por encima del huevo y del pescado porque quita más |
| Superar el récord de distancia del escenario | `record`, una vez por partida |
| Caer por un barranco | `caida-barranco`, cuando ya no hay suelo que la recoja |
| Quedarse sin puntos | `fin-partida` |
| Fin de la partida | La música se retira en el pulso siguiente (200 ms) y, 0,6 s después, `jingle-fin` o, con récord nuevo, `jingle-record`; en el panel de fin no hay música hasta la siguiente partida |
| La app pasa a segundo plano | Todo se calla, y vuelve al volver |

Los jingles son de cada escenario, con sus instrumentos (`jingle-inicio-bosque.wav`, `jingle-record-nieve.wav`…); si
falta el de un escenario, suena el genérico. El botón de sonido, junto al de pausa y en el inicio, lo calla todo; viene
encendido la primera vez y el juego recuerda cómo lo dejó ([pantallas](pantallas.md)).

La mezcla, la que propuso GameMaster para empezar: la música a 0,8 y los efectos de la partida, cada vez un poco distintos
(±40 cents de tono y ±1,5 dB al azar), para que una racha de ellos no suene a máquina; la cola del salto largo, con el
mismo tono que su salto corto, para que empalmen; los jingles y los botones, siempre iguales. En el escritorio solo cambia el volumen, no el tono.

## Con las demás apps (QA-07)

Mientras suena la música del juego, el juego se queda con el sonido del teléfono, el foco del audio de Android. Así la
música de otras apps se para al jugar, y una llamada que suena o una voz que habla se lo quitan y la partida se pausa con
su menú, como al salir de la app. Con la música del juego apagada en los ajustes, o con todo el sonido apagado, lo
suelta, y la música de otras apps suena con el juego: así Caro puede jugar con la suya. Entonces la llamada solo pausa
al cogerla. Lo eligió Fangusk: «Okey adelante». Si cambia el ajuste en mitad de una partida, lo coge o lo suelta en ese
momento.

## Cómo suena en cada teléfono

- **Android**: la música, con un AudioTrack al que un hilo propio, con prioridad de audio, le da las muestras: las del
  bucle una y otra vez, así que no tiene hueco, o las que hace la música de la partida, a la frecuencia del teléfono
  (48 000 o 44 100 Hz); los efectos, con SoundPool, que los empieza en un hilo propio con prioridad de audio: en el
  hilo del juego, en un Xiaomi Mi 10 cada salto y cada aterrizaje se comían dos o tres fotogramas ([rendimiento](rendimiento.md)).
- **iOS**: los bucles y los efectos con AVAudioPlayer; la música de la partida, con AVAudioEngine, en tres búferes de
  85 ms que se hacen de nuevo al acabar cada uno. Todo en una cola propia: en el hilo que dibuja, cada efecto lo paraba
  unos milisegundos y se notaba en la carrera. Respeta el interruptor de silencio, como cualquier juego.
- **Escritorio**: Clip de Java para los bucles y una línea de Java Sound con su hilo para la música de la partida.
- Los instrumentos de cada escenario se hacen una vez, en segundo plano, al abrir el juego; si una partida empieza
  antes de que estén, suena su bucle.
- Una partida nueva en el mismo escenario reinicia la pista sin volver a leerla. Si un teléfono no puede reproducir algo, el
  juego sigue en silencio, nunca se cierra. Las versiones de depuración se pueden abrir sin sonido para medir (`-silencio`
  en iOS, `--ez silencio true` en Android, `--silencio` en el escritorio).

## Los ficheros

En `shared/src/commonMain/composeResources/files/`: `musica/<escenario>.wav`, `musica/ambiente-<escenario>.wav` y
`sonidos/<nombre>.wav`, WAV de PCM de 16 bits en mono a 22 050 Hz, que leen las tres plataformas sin convertir. El
ambiente se mezcla en las muestras de la música de la partida, en el teléfono, así que no necesita un reproductor más. Los instala `tools/sonido.py --desde <entrega>`,
que comprueba el formato, y una prueba comprueba que están todos y se pueden leer. Ocupan unos 7,4 MB, casi todo los
bucles; si pesa demasiado en el APK, se pueden pasar a OGG más adelante.
