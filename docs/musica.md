# La música generativa

La música de cada escenario se hace en el propio teléfono mientras se juega (AUD-19, AUD-24, AUD-25 y AUD-26): una
canción distinta en cada partida, con capas que entran con la velocidad, una versión tensa cuando quedan pocos puntos y
su final. Es el port a Kotlin del prototipo en Python que Fangusk aprobó el 2026-11-02
(`../backlog-juego-plataformas/diseno/musica/`, `sinte.py` y `generador.py`, y su sección «Para portarlo a Kotlin»), en
`shared/src/commonMain/kotlin/com/fanguskdev/carofarty/audio/music/`, código común a Android, iOS y el escritorio sin
nada de ninguna plataforma.

Hace lo mismo que el prototipo, nota por nota: con la misma semilla y las mismas decisiones compone las mismas notas que
su código, y el sonido que sale de ellas coincide con el del prototipo hasta una parte en cien millones. Lo que cambia
porque aquí la partida no se conoce de antemano, y por qué, está más abajo.

## Las piezas

| Fichero | Qué hace | En el prototipo |
|---|---|---|
| `Chip.kt` | El chip que se imita: 32 000 Hz, 6 voces de música y 2 de efectos, la tabla gaussiana, el códec BRR, las muestras con bucle, el ruido de 15 bits, el eco (EDL, EFB, EVOL y el FIR del manual escalado a 128) y el paso alto que quita la continua | `sinte.py` |
| `Recipes.kt` | Las recetas de las muestras: aditiva, FM, Karplus-Strong, colchón, membrana, metal, acorde muestreado, viento con soplo, pájaros y palo de lluvia | `muestra_*` |
| `Instruments.kt` | El instrumento como datos: sus muestras por registro (o por calidad de acorde), envolvente ADSR, volumen, vibrato, ruido, caída por altura, tono fijo y adorno | `Instrumento` |
| `Voice.kt` | Una voz en tiempo real: el registro de tono de 14 bits con su tope, la interpolación gaussiana, la envolvente, el vibrato y los adornos (cada 32 muestras), y la rampa de 8 ms que apaga una nota justo antes de la siguiente de su canal | `tocar`, `Instrumento.render` y el corte de `renderizar` |
| `Style.kt` | El escenario como datos: escala, tónica, tempo, compás, swing, acordes, Markov, frases, inicios, cadencias, ritmo armónico, instrumentos, rangos, motivo de Caro, motivos, eco, volúmenes y capas | `Escenario` |
| `Composer.kt` | Lo común: progresiones por Markov de 2.º orden, la melodía por celdas a partir del motivo de Caro, armonizar, llevar al rango, limpiar y humanizar | `Compositor` |
| `Forest.kt`, `Snow.kt`, `Desert.kt` | Cada escenario: sus instrumentos, sus datos y sus partes (bajo, colchón, ambiente, percusión, C2, adornos y versión tensa) | `BOSQUE` y `Bosque`, `NIEVE` y `Nieve`, `DESIERTO` y `Desierto` |
| `MusicEngine.kt` | Compás a compás: niveles, tensión, fin, secciones, tempo, capas, colas de notas por canal y la mezcla con el eco | `maquetas.plan` y `renderizar` |
| `Resampler.kt` | De 32 000 Hz a la frecuencia del teléfono: sinc polifásico con ventana de Kaiser, exacto para 48 000, 44 100 y 22 050 | `remuestrear` |
| `Effects.kt` | Las 2 voces de efectos y el aviso de pocos puntos | `sfx.aviso_pocos_puntos` |
| `MusicStream.kt` | La API para los reproductores de cada plataforma | — |
| `DetMath.kt`, `PyRandom.kt` | Seno, exponencial y logaritmo con solo sumas y productos, y el generador aleatorio de Python (Mersenne Twister) | `math` y `random` |

Un escenario nuevo es un `MusicStyle` (datos), sus `InstrumentRecipe` (datos) y un `Arrangement` con sus partes, como en el
prototipo; un instrumento nuevo, una receta. El motor no cambia.

## La API

```kotlin
val banco = MusicBank(Scenario.Bosque)                  // los instrumentos: de 20 a 300 ms; uno por escenario
val musica = MusicStream(Scenario.Bosque, semilla, 48_000, banco)

musica.setLevel(2)          // 1 a 5: C0 siempre, C1 desde el 2, C2 desde el 3, C3 (la melodía) desde el 4
musica.setTense(true)       // quedan pocos puntos: el aviso en el pulso siguiente, la versión tensa en el compás siguiente
musica.setTense(false)      // se recupera: en el compás siguiente vuelve la música con una A
musica.end()                // fin de la partida: en el pulso siguiente, las capas se retiran en 200 ms
musica.render(buffer, 0, frames)   // mono, 16 bits, a 48 000 Hz; desde el hilo de audio
musica.isFinished           // ya solo da silencio: se puede parar el reproductor
```

- **Es de tirar** (pull): el reproductor de cada plataforma (AudioTrack en Android, AVAudioEngine o similar en iOS, Java
  Sound en el escritorio) pide muestras con `render` cuando las necesita. No depende de ninguna API de plataforma.
- **`render` no crea objetos**: todo se reserva al construir; ni componer una sección nueva crea basura. Se puede llamar
  desde el hilo de audio sin pausas del recolector.
- **Los controles se pueden llamar desde cualquier hilo** y se aplican al principio del siguiente `render`.
- **La semilla** decide la canción: una por partida. Con la misma semilla y los mismos controles en las mismas muestras
  sale lo mismo, muestra a muestra, en la JVM, en Android y en iOS (lo comprueba una prueba en las tres), sea cual sea el
  tamaño de los búferes.
- **El aviso de pocos puntos lo toca la música** con una de sus 2 voces de efectos, porque solo ella sabe dónde cae el
  pulso siguiente. El efecto de fin de partida y los jingles los sigue tocando el juego.
- **`tempoRises = false`** deja el tempo fijo (el `--tempo-fijo` del prototipo), para que Fangusk lo compare.

## Cómo se escucha

```bash
./gradlew :shared:jvmTest --tests '*MusicDemosTest'
```

deja en `shared/build/musica/` `bosque.wav`, `nieve.wav` y `desierto.wav` (32 000 Hz, mono, 16 bits, 88,4 s) y sus
`*-marcas.txt` (etiquetas de Audacity). Es la partida simulada de las maquetas del prototipo, con la misma semilla (7):
el nivel sube cada 20 s, a los 72 s quedan pocos puntos y a los 84 s se acaba. Solo la música: sin el efecto de fin ni
el jingle del final, que son del juego.

Las del Bosque y el Desierto tienen las mismas secciones, en los mismos instantes y con las mismas notas que las del
prototipo (`demos/`). La de la Nieve es igual hasta los 42,8 s; desde ahí suena una B donde el prototipo puso una A
(se explica más abajo).

## Cómo se comprueba

En `shared/src/commonTest` (JVM, Android y simulador de iOS):

| Prueba | Qué comprueba |
|---|---|
| `PortBasicsTest` | El generador aleatorio da los mismos números que Python 3.9 con las mismas semillas; seno, exponencial y logaritmo se desvían menos de 2·10⁻¹⁵ de los de la plataforma; las escalas |
| `ChipTest` | Σ\|FIR\| = 136 en el manual y ≤ 128 en el eco; EFB ≤ $50 y EDL válido en los tres escenarios; 6 + 2 voces; la tabla gaussiana (0,18 · 0,64 · 0,18 y −11 dB en el Nyquist); BRR; bucles; el ruido (periodo 32 767); el eco; el aviso; el limitador (nunca −1 dBFS); el remuestreador (exacto, sin desviar un tono, igual con cualquier búfer) |
| `RecipesTest` | Cada receta hace lo que debe (bucles que empalman, bordes en silencio, semillas CRC32) y cada escenario tiene sus instrumentos |
| `MusicStreamTest` | Una partida corta de cada escenario: pico ≤ −1 dBFS, sin continua (< −60 dBFS), ninguna nota por encima del tope del registro de tono ni cortada tarde; la misma semilla da lo mismo con búferes distintos y otra semilla, otra canción; la huella de 8 s de cada escenario es la misma en las tres plataformas; el aviso cae en un pulso y la versión tensa en el compás siguiente; las capas entran con el nivel y bajan con 2 compases de histéresis; tras el fin solo queda silencio |

En `shared/src/jvmTest`:

| Prueba | Qué comprueba |
|---|---|
| `PrototypeFidelityTest` | Las notas de la partida simulada (instante, canal, instrumento, altura, duración y volumen de cada una: 1 577 en el Bosque, 1 083 en la Nieve y 1 525 en el Desierto) son las del prototipo. Las huellas salen de `tools/musica_referencia.py`, que mueve el código del prototipo como lo mueve el motor |
| `MusicCleanTest` | Sin chasquidos, con la comprobación de `verificar.py`: canal a canal, ningún salto aislado fuera del arranque de una nota; en la mezcla, nada en los cambios de sección, las entradas de capa, el aviso y el fin; empieza y acaba en silencio |
| `MusicVarietyTest` | 30 minutos de cada escenario, con los umbrales de nivel propuestos para AUD-04, sin repetir ninguna sección de 8 compases nota a nota (126 secciones en el Bosque, 141 en la Nieve y 117 en el Desierto) |
| `MusicDeterminismTest` | Un minuto de cada escenario, renderizado dos veces con búferes distintos, sale con la misma huella |
| `MusicCostTest` | Lo que cuesta (abajo) y que `render` no crea ni un byte en una partida entera con todos los caminos (capas, secciones, tensión y vuelta, nivel 5, bajada, fin y silencio) |
| `MusicDemosTest` | Escribe las demos |

Además, durante el port, la música entera de las tres partidas se comparó con la del prototipo: la mayor diferencia fue
de 2·10⁻⁸ (−150 dB), lo que va de guardar las muestras en 32 bits en vez de 64.

## Lo que cuesta

Medido en la JVM de un MacBook Pro con Apple Silicon, en `MusicCostTest`, en varias ejecuciones:

| | Bosque | Nieve | Desierto |
|---|---|---|---|
| 60 s de música a 48 000 Hz | 0,16 a 0,24 s | 0,15 a 0,19 s | 0,16 a 0,20 s |
| Hacer los instrumentos (`MusicBank`) | 30 a 160 ms | 80 a 270 ms | 20 a 30 ms |
| Memoria creada por `render` en una partida de 200 s (la segunda vez) | 0 bytes | 0 bytes | 0 bytes |

Es decir, entre un 0,3 % y un 0,4 % de un núcleo. En el simulador de iOS, con la versión de pruebas sin optimizar, 120 s
de música cuestan unos 11 s (también en tiempo real de sobra). La medida que vale es la de sus teléfonos, con la versión
optimizada (AUD-29). La memoria: las muestras de un banco ocupan 1,0 MB en el Bosque, 1,4 MB en la Nieve y 0,5 MB en el
Desierto, y una partida, alrededor de medio megabyte más (0,2 MB más a 44 100 Hz, por las 441 fases del remuestreo).

La prueba de memoria corre en una JVM aparte con el primer compilador (C1): el compilador optimizador de la JVM (C2) a
veces crea unos pocos bytes al desoptimizar un método, haga lo que haga el código, y taparía lo que se mide. Con el
intérprete solo (`-Xint`) tampoco se crea nada a partir de la segunda vez que se pasa por cada camino; la primera, la
JVM enlaza el código y eso sí crea objetos, una vez por proceso.

## Lo que cambia respecto al prototipo, y por qué

- **El tiempo real.** El prototipo compone la partida entera sabiendo de antemano cuándo sube el nivel; el juego no lo
  sabe. Por eso:
  - **Cada compás se decide 16 ms antes de empezar** (512 muestras), con lo que el juego haya pedido hasta entonces: a
    un compás le hace falta saber sus notas un poco antes porque la humanización adelanta algunas hasta 5 ms y la nota
    anterior de su canal tiene que apagarse en los 8 ms previos. Un cambio de nivel en esos 16 ms entra en el compás
    siguiente. (En la demo los niveles se dan 16 ms antes para que los compases sean los del prototipo: en el Bosque
    empieza uno justo a los 20 s.)
  - **La sección se compone al empezar**, siempre con sus 8 compases; si se corta antes (entra la melodía o la tensión),
    sus notas se sueltan en el corte, como en el prototipo, salvo que el corte ya haya pasado al decidirlo (entonces
    se sueltan en ese momento, como mucho unos 50 ms tarde).
  - **La sección siguiente se elige al acabar la anterior**, como en el prototipo, pero el prototipo evita una A justo
    antes de que entre la melodía porque sabe cuándo entrará; aquí se evita la A en el nivel 3, que es el que precede a
    la melodía. En la demo del Bosque y del Desierto coincide con el prototipo; en la de la Nieve no (a los 42,8 s el
    nivel ya es 3 pero la melodía aún tardaría dos secciones), y suena una B en vez de una A.
  - **El bajo que camina de la Nieve** necesita saber al empezar la sección a qué acorde va la siguiente: lo mira en una
    copia del generador aleatorio (la elección será esa salvo que el nivel cambie entre medias).
- **Lo que el prototipo no tiene porque sus partidas solo suben:** al volver de la tensión (`setTense(false)`) empieza
  una A en el compás siguiente; para bajar de nivel hacen falta 2 compases (la histéresis de su README).
- **† El nivel 5** sube el tempo al tope (+12 %) y pone en marcha el ciclo de intensidad de 44 compases del prototipo
  (`INTENSIDAD_CICLO`, que su código define pero no usa), empezando por su pico para no dar un bajón. Es una propuesta
  para las partidas largas: la decide Fangusk al oírla. La percusión a doble densidad del nivel 5 que cita su README no
  está en su código, y no se ha portado.
- **El volumen** es fijo por escenario, calibrado como el prototipo normaliza sus demos (la partida de la demo a −20 dBFS
  de RMS salvo que el pico pase de −1,3 dBFS): el Bosque y el Desierto quedan a −20 dBFS y la Nieve a −21,4. Encima, un
  limitador suave que solo toca los picos por encima de −2 dBFS y nunca llega a −1 dBFS (el prototipo normalizaba cada
  fichero; aquí la canción no se conoce de antemano). El aviso suena 1 dB por debajo de la música, como en las demos.
- **Los primeros 10 ms entran con un fundido**, como en las demos, para que el reproductor nunca arranque en un escalón.
- **El aviso es una muestra hecha al empezar** y la toca una voz de efectos; el prototipo también lo hacía así.
- **Las matemáticas** son las propias (`DetMath`) y no las de la plataforma, para que todas den lo mismo bit a bit; la
  envolvente baja multiplicando muestra a muestra en vez de elevar una potencia en cada una (coinciden hasta 10⁻¹¹), y
  las muestras se guardan en 32 bits.

## Lo que falta

- ~~Los reproductores de cada plataforma~~: hechos (2026-11-02). `GameAudio` le da a cada partida su `MusicStream` con
  la semilla de la partida, el nivel por la velocidad (AUD-04: 200, 240, 280 y 320 px/s) y la versión tensa con 3
  puntos o menos; lo tocan el AudioTrack de Android, el AVAudioEngine de iOS y una línea de Java Sound en el escritorio
  ([sonido](sonido.md)).
- **† La capa de premio C4** y **las reglas de partidas muy largas** (ambiente cada 5 minutos, menos densidad desde los
  10), que tampoco tiene el prototipo.
- **Medir el coste en sus teléfonos** (AUD-29).
