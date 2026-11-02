# Música y sonidos de la beta

Lo que suena en la beta del 2026-11-02 y cuándo. Es **provisional**: los bucles, los jingles y los efectos los generó un
agente de GameMaster esa noche (`../backlog-juego-plataformas/diseno/musica/beta/`) y nadie los había escuchado aún. La
música de verdad, generativa y con capas que nunca aburre, es de M4 ([diseño](diseno.md#la-música)). Cuándo suena cada
cosa es propuesta mía, para que Fangusk la cambie al oírla.

## Cuándo suena cada cosa

| Momento | Qué suena |
|---|---|
| Panel de inicio | El bucle del escenario elegido, para oírlo al elegir |
| Elegir escenario o pausar | `ui-pulsar`, y el bucle del escenario nuevo desde su principio |
| Reanudar tras la pausa | `ui-volver`, y la música sigue donde estaba |
| Empezar una partida | La música se calla, suena `jingle-inicio` (2,6 s) y el bucle vuelve desde su principio al acabar |
| Saltar | `salto-corto`, en el mismo instante del despegue |
| Aterrizar | `aterrizaje` |
| Rescatar un perrito o un gatito | `rescate-perrito` o `rescate-gatito` |
| Coger un huevo o un pescado | `huevo` o `pescado` |
| Superar el récord de distancia del escenario | `record`, una vez por partida |
| Caer por un barranco | `caida-barranco`, cuando ya no hay suelo que la recoja |
| Quedarse sin puntos | `fin-partida` |
| Fin de la partida | La música se para y, 0,6 s después, `jingle-fin` o, con récord nuevo, `jingle-record`; en el panel de fin no hay música hasta la siguiente partida |
| La app pasa a segundo plano | Todo se calla, y vuelve al volver |

`salto-largo` no se usa todavía: para saber si el salto será largo hay que esperar a ver si mantiene el dedo, unos 100 ms,
y el sonido llegaría tarde. GameMaster le ha propuesto a Fangusk tres opciones (en su resumen de la mañana): el corto
siempre, como ahora; el largo a partir de cierta velocidad; o el corto al despegar y, si a los 100 ms sigue con el dedo,
una cola que suba encima.

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
