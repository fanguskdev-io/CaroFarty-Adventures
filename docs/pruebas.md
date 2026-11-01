# Pruebas

Qué se prueba en cada tarea y en cada hito, dónde y cómo se apuntan los fallos (QA-01). Los fallos y su revisión están en
la [lista de bugs](bugs.md); los criterios de salida de cada hito, en [proceso](proceso.md).

## En cada tarea

1. **La puerta**: `bash scripts/verificar.sh` termina en «La puerta pasa» (ktlint, las pruebas en la JVM, en Android y en
   el simulador de iOS, el lint de Android, los APK y la app de iOS compilada).
2. **A mano**, lo que cambia la tarea, en el emulador de Android y en el simulador de iOS, con capturas si se ve.

## Las pruebas automáticas (TEC-15)

| Qué | Prueba | Dónde |
|---|---|---|
| El salto: la tabla por fotogramas, el coyote time, el buffer, las paredes y los bloques en el aire | `CaroTest` | Común |
| La precisión lejos del origen (GEN-09) | `FarFromTheOriginTest` | Común |
| El generador: reglas, semilla, recolectables, la elección, tensión y calma, nunca tres difíciles iguales seguidas, obstáculos en el aire que dejan pasar por debajo y un piloto 3 × 4 min sin caer | `CourseGeneratorTest` | Común |
| La partida: el primer toque, los puntos, el fin por caída y por quedarse sin puntos | `GameRunTest` | Común |
| Los récords por escenario | `RecordsTest` | Común |
| El paseo de los perritos: dentro de su tramo, con paradas, igual en cualquier teléfono | `WalkTest` | Común |
| El motor: el paso fijo, el escalado, el toque | `FixedStepTest`, `PixelViewportTest`, `TouchInputTest` | Común |
| La cámara (PLY-13) y el medidor de fotogramas (MOB-02) | `CameraTest`, `FpsMeterTest` | Común |
| El sintetizador y el almacén | `SynthTest`, `MemoryStoreTest` | Común |
| Las partidas largas: 10.000 tramos, 30 min sin caer y 50 min sin tocar lo que resta | `LongRunsTest` | JVM |
| La generación en segundo plano | `BufferedPiecesTest` | JVM |
| Las métricas y la curva de dificultad (GDD-06, GDD-08) | `MetricsReportTest` | JVM |
| Las pantallas en 6 proporciones, píxel perfecto, y los dos finales | `SnapshotsTest` | JVM |
| Un bloque y un huevo en el aire dibujados en una escena fija | `BlockSnapshotTest` | JVM |

Las comunes corren tres veces: en la JVM, en las pruebas de host de Android y en el simulador de iOS; las de la JVM, solo
allí, porque tardan o porque dibujan con Skia. Las capturas de `SnapshotsTest` quedan en `shared/build/capturas/` y las
métricas en `shared/build/metricas/`.

## Las herramientas de la versión de depuración

- **La línea de depuración**: un toque con un segundo dedo la abre y la cierra; con `--ez debug true` (Android) o `-debug`
  (iOS) sale abierta. Lo que mide está en [rendimiento](rendimiento.md).
- **El piloto automático**: con `--ez autopilot true` (Android), `-autopilot` (iOS) o `--autopilot` (escritorio) juega solo
  y vuelve a empezar al acabar. Sirve para ver partidas donde no se puede tocar y para medir partidas largas.
- **Con la línea abierta**: antes de empezar, la esquina de abajo a la izquierda cambia de escenario; durante la partida,
  un toque en los puntos quita 5, para ver el fin sin puntos.

```bash
adb shell am start -n com.fanguskdev.carofarty/.MainActivity --ez debug true --ez autopilot true
```

```bash
xcrun simctl launch booted com.fanguskdev.carofarty.CaroFarty -debug -autopilot
```

## En cada hito

### M1 · Prototipo en gris

- [ ] El primer toque arranca la carrera sin saltar; un toque rápido salta poco y mantener el dedo salta más.
- [ ] Se puede saltar justo después de dejar el borde (coyote time) y un toque justo antes de aterrizar salta al tocar
  suelo (buffer).
- [ ] Caer por un barranco acaba con «TE CAÍSTE POR UN BARRANCO»; quedarse sin puntos, con «TE QUEDASTE SIN PUNTOS».
- [ ] La velocidad sube durante toda la partida; los huevos y pescados en el aire y los bloques piden afinar el salto, y
  un bloque frena la subida, para en el aire de costado y deja correr por encima.
- [ ] Perritos y gatitos suman; huevos fritos y pescado restan; los puntos nunca bajan de 0; con 3 o menos, el marcador
  se pone en rojo.
- [ ] Los récords se guardan al cerrar y abrir la app; con uno nuevo sale «¡NUEVO RÉCORD!».
- [ ] La pausa: el botón, la app en segundo plano (vuelve en pausa) y el botón atrás de Android.
- [ ] Del fin de una partida a la siguiente, un toque y menos de 2 s.
- [ ] Las pantallas: el emulador (20:9), el iPhone con isla dinámica y el iPad; nada cortado y píxel perfecto.
- [ ] El rendimiento, con el piloto 10 min: 60 FPS, menos del 1 % de fotogramas lentos y la memoria sin crecer.
- [ ] Fangusk juega el prototipo y da su visto bueno (QA-02, QA-03).

### M2 · Caro, recolectables y Bosque

- [ ] Caro y los cuatro recolectables se distinguen de un vistazo a toda velocidad ([recolectables](recolectables.md)).
- [ ] Las animaciones de Caro: correr, saltar, caer, pararse sin puntos.
- [ ] El Bosque: capas a distinta velocidad, sin costuras al repetirse, legible con los recolectables delante.
- [ ] La lista de comprobación de [propiedad intelectual](propiedad-intelectual.md) en cada pieza.
- [ ] El rendimiento de M1 se mantiene con el arte final.
- [ ] Fangusk lo juega y lo valida (QA-04).

### M3 · Nieve y Desierto

- [ ] Los tres escenarios jugables y la pantalla para elegirlos, que recuerda el último.
- [ ] 30 minutos de piloto automático en cada escenario sin fallos ni tirones.
- [ ] Los récords de cada escenario por separado.

### M4 · Música dinámica y efectos

- [ ] 30 minutos de música por escenario sin cortes, clics ni repeticiones evidentes.
- [ ] Los efectos al recoger, al saltar y al acabar, sin tapar la música.
- [ ] El sonido respeta el modo silencio del iPhone y el volumen del teléfono, y se para al salir de la app.
- [ ] Que la música no canse, a juicio de Fangusk.

### M5 · Menús, pulido y entrega

- [ ] El título, la pausa, los récords, los ajustes y el icono.
- [ ] La primera vez que se abre: sin récords ni datos previos.
- [ ] En modo avión: todo funciona igual ([ADR 0005](adr/0005-un-regalo-sin-conexion.md)).
- [ ] Instalada en sus teléfonos y probada allí de principio a fin.

## En qué teléfonos

Los de desarrollo están en [dispositivos](dispositivos.md): cada tarea, en el emulador `Tester_API36` y el simulador del
iPhone 17 Pro; cada hito, además, en el resto de la lista. Los teléfonos donde jugará ella los decide Fangusk (TEC-13), y
cada hito desde M2 se prueba también allí.

## Cómo se apunta un fallo

Cada fallo va a la [lista de bugs](bugs.md) con un número, dónde se vio (dispositivo y versión), los pasos para que
ocurra, lo esperado y lo que pasa, su gravedad y su estado. Si hace falta trabajo, se enlaza su tarea del backlog. Lo que
diga Fangusk al probar va con sus palabras.
