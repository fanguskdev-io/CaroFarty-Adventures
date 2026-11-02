# Rendimiento

Los presupuestos de rendimiento (MOB-02) y cómo se miden. La línea de depuración los enseña en pantalla mientras se juega
(un toque con un segundo dedo la abre; cómo arrancar con ella y con el piloto automático, en [pruebas](pruebas.md)).

## Los presupuestos

| Qué | Presupuesto | Dónde se ve |
|---|---|---|
| Fluidez | 60 fotogramas por segundo estables | **FPS**, la media de los últimos 30 fotogramas |
| Tirones | Ningún fotograma de más de 25 ms jugando y menos del 1 % de fotogramas lentos en una partida larga | **PEOR**, el fotograma más largo de los últimos 2 s, y **LENTOS**, la parte de fotogramas de más de 25 ms desde que se abrió el juego |
| Coste de un fotograma | La lógica por debajo de 2 ms y el dibujo por debajo de 8 ms, la mitad de los 16,7 ms de un fotograma | **LÓGICA** y **DIBUJO**, en milisegundos y suavizados |
| Arranque | Menos de 3 s desde el icono hasta poder jugar | **ARRANQUE**, desde que arranca el juego hasta el primer fotograma; el arranque completo, con `adb shell am start -W` en Android y en Xcode en iOS |
| Memoria | Que no crezca en una partida larga | `adb shell dumpsys meminfo com.fanguskdev.carofarty` en Android; la memoria del proceso en el simulador o en Xcode |
| Recolector de basura | Sin pausas que se noten: el bucle del juego no crea objetos en cada fotograma (TEC-17) | **PEOR** y **LENTOS**; en Android, `adb logcat` sin pausas del recolector |
| Batería | Una partida de 10 minutos gasta lo que un juego sencillo | El perfilador de energía de Android Studio y el de Xcode, en sus teléfonos |

## Lo medido (2026-11-01)

Con el piloto automático, en las versiones de depuración:

| | Emulador Android 16 (2400×1080) | Simulador iPhone 17 Pro (iOS 26.5) |
|---|---|---|
| FPS | 60 | 60 |
| PEOR | 17 ms | 17 ms |
| LENTOS | 0,3 % en el primer minuto, contando el arranque | 0,0 % en 3 minutos |
| LÓGICA | 0,1 ms | 0,1 ms |
| DIBUJO | 0,9 a 1,4 ms | 4,3 ms |
| ARRANQUE | 0,2 a 0,3 s; desde el icono, en frío, 1,2 s (`am start -W`) | 0,1 a 0,2 s |
| Memoria en 10 minutos | 79 MB estables (de 72 al minuto 0 a 79 al 10) | 180 MB estables |
| Fotogramas en 10 minutos | 35.766 (59,6 por segundo) y 1,1 % lentos según `dumpsys gfxinfo` | Al minuto 10: 60 FPS a 199 px/s y 8.635 m |
| Recolector | Ninguna pausa en el registro en 3 minutos | Sin fotogramas lentos |

Cómo leerlo:

- **El emulador y el simulador no son teléfonos**: el emulador dibuja por software en la CPU del Mac y, si a la vez corre
  el simulador, pierde fotogramas (con los dos a la vez marcó un 3 % de lentos; solo, un 0,3 %). El simulador corre en la
  CPU del Mac con una versión de depuración sin optimizar, que es la que más dibujo marca.
- **La medida que vale es la de sus teléfonos**, cuando Fangusk diga cuáles son (TEC-13): el más lento de ellos tiene que
  cumplir la tabla de presupuestos. Hasta entonces, estas cifras dan margen de sobra: el fotograma usa menos de un tercio
  de su tiempo.
- **El dibujo de iOS** es el número a vigilar en M2, cuando entre el arte final: hoy el mundo se pinta por software en un
  búfer pequeño que luego se escala ([ADR 0003](adr/0003-horizontal-y-pixel-perfecto.md)).

## Con el arte y el sonido (2026-11-02, beta)

Con el piloto automático, en las versiones de depuración, en el Bosque:

| | Emulador Android 16 (2400×1080) | Simulador iPhone 17 Pro (iOS 26.5) |
|---|---|---|
| FPS | 11 a 27 | 60 |
| PEOR | 117 a 333 ms | 17 ms |
| LENTOS | 70 a 75 % | 0,0 % en minuto y medio |
| LÓGICA | 0,3 a 1,0 ms | 0,0 a 1,1 ms |
| DIBUJO | 3,7 a 6,0 ms | 4,5 a 4,7 ms |
| ARRANQUE | 1,0 s | 0,1 a 0,5 s |

Cómo leerlo:

- **El emulador ya no sirve para medir la fluidez**: Android 16 exige GLES 3.1 y el emulador lo da por software («API level
  36 requires GLES 3.1, forcing software rendering»). Con el arte, cada fotograma escala a pantalla completa el cielo, los
  dos fondos y el mundo, y eso por software no llega. Lo que cuesta en la CPU, la lógica y el dibujo, sigue muy por debajo
  del presupuesto; la medida de verdad es la de sus teléfonos (TEC-13).
- **El sonido daba tirones en iOS**: cada efecto bloqueaba unos milisegundos el hilo que dibuja (con sonido, 56 FPS, 45 ms
  de peor fotograma y un 7,8 % de lentos; sin él, 60, 17 ms y 0 %). Desde que va por su propia cola, con sonido mide lo
  mismo que sin él ([sonido](sonido.md)).
- **Cuánto pinta la GPU**: el cielo, el fondo lejano, el medio, los copos en la nieve y el mundo cubren casi toda la
  pantalla cada uno. Si un teléfono lento no llegara a 60, lo primero sería no pintar lo que tapan los fondos opacos (el
  lejano del Bosque es opaco entero y tapa el cielo).
