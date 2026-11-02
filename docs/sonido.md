# Música y sonidos de la beta

Lo que suena en la beta del 2026-11-02 y cuándo. Es **provisional**: los bucles, los jingles y los efectos los generó un
agente de GameMaster esa noche (`../backlog-juego-plataformas/diseno/musica/beta/`) y nadie los había escuchado aún. La
música de verdad, generativa y con capas que nunca aburre, es de M4 ([diseño](diseno.md#la-música)). Cuándo suena cada
cosa es propuesta mía, para que Fangusk la cambie al oírla.

## Cuándo suena cada cosa

| Momento | Qué suena |
|---|---|
| Pantalla inicial, récords y cómo se juega | El bucle de la última fase elegida |
| Pulsar un botón: récords, cómo se juega, una pestaña, una página, la pausa, el sonido | `ui-pulsar` |
| Reanudar tras la pausa o volver al inicio | `ui-volver`; al reanudar, la música sigue donde estaba, y al volver, la de la fase desde su principio |
| Empezar una partida | La música se calla, suena `jingle-inicio` (2,6 s) y el bucle vuelve desde su principio al acabar |
| Saltar | `salto-corto`, en el mismo instante del despegue, y si a los 100 ms sigue con el dedo, `salto-largo` encima: la cola que sigue subiendo (Fangusk, opción C) |
| Aterrizar | `aterrizaje` |
| Rescatar un perrito o un gatito | `rescate-perrito` o `rescate-gatito` |
| Coger un huevo o un pescado | `huevo` o `pescado` |
| Superar el récord de distancia del escenario | `record`, una vez por partida |
| Caer por un barranco | `caida-barranco`, cuando ya no hay suelo que la recoja |
| Quedarse sin puntos | `fin-partida` |
| Fin de la partida | La música se para y, 0,6 s después, `jingle-fin` o, con récord nuevo, `jingle-record`; en el panel de fin no hay música hasta la siguiente partida |
| La app pasa a segundo plano | Todo se calla, y vuelve al volver |

Los jingles son de cada escenario, con sus instrumentos (`jingle-inicio-bosque.wav`, `jingle-record-nieve.wav`…); si
falta el de un escenario, suena el genérico. El botón de sonido, junto al de pausa y en el inicio, lo calla todo; viene
encendido la primera vez y el juego recuerda cómo lo dejó ([pantallas](pantallas.md)).

La mezcla, la que propuso GameMaster para empezar: la música a 0,8 y los efectos de la partida, cada vez un poco distintos
(±40 cents de tono y ±1,5 dB al azar), para que una racha de ellos no suene a máquina; la cola del salto largo, con el
mismo tono que su salto corto, para que empalmen; los jingles y los botones, siempre iguales. En el escritorio solo cambia el volumen, no el tono.

## Cómo suena en cada teléfono

- **Android**: la música, con un AudioTrack al que un hilo propio le da las muestras una y otra vez, así que el bucle no
  tiene hueco; los efectos, con SoundPool.
- **iOS**: AVAudioPlayer, todo en una cola propia: en el hilo que dibuja, cada efecto lo paraba unos milisegundos y se
  notaba en la carrera. Respeta el interruptor de silencio, como cualquier juego.
- **Escritorio**: Clip de Java, en un hilo propio.
- Una partida nueva en el mismo escenario reinicia la pista sin volver a leerla. Si un teléfono no puede reproducir algo, el
  juego sigue en silencio, nunca se cierra. Las versiones de depuración se pueden abrir sin sonido para medir (`-silencio`
  en iOS, `--ez silencio true` en Android, `--silencio` en el escritorio).

## Los ficheros

En `shared/src/commonMain/composeResources/files/`: `musica/<escenario>.wav` y `sonidos/<nombre>.wav`, WAV de PCM de 16
bits en mono a 22 050 Hz, que leen las tres plataformas sin convertir. Los instala `tools/sonido.py --desde <entrega>`,
que comprueba el formato, y una prueba comprueba que están todos y se pueden leer. Ocupan unos 7,4 MB, casi todo los
bucles; si pesa demasiado en el APK, se pueden pasar a OGG más adelante.
