# CaroFarty

Un plataformas de desplazamiento horizontal infinito en pixel art para Android e iOS: Caro corre sola, salta barrancos y
recoge perritos y gatitos (que suman) esquivando huevos fritos y pescado (que restan). Es un regalo de Fangusk para su
esposa. La [página de diseño](docs/diseno.md) cuenta lo que pidió, las reglas y las decisiones.

> En construcción: está terminando M0 (diseño y decisiones). El siguiente hito es el prototipo en gris (M1).

## Estructura

```
shared/         el juego entero, común a Android, iOS y el escritorio (Kotlin Multiplatform + Compose Multiplatform)
  src/commonMain/kotlin/com/fanguskdev/carofarty/
    engine/       motor 2D: bucle de paso fijo, entrada, cámara y dibujo píxel perfecto (M1)
    game/         reglas: Caro, el recorrido, los recolectables y los puntos (M1)
    audio/        síntesis de sonido y música generativa (M4)
    storage/      lo que se guarda en el teléfono: récords y ajustes
    ui/           pantallas: marcador, fin de partida y menús
  src/androidMain, src/iosMain, src/jvmMain   lo que es de cada plataforma
androidApp/     la app de Android
iosApp/         la app de iOS (proyecto de Xcode)
desktopApp/     herramienta de desarrollo: una ventana para probar rápido; no se entrega
docs/           diseño, decisiones (ADR) y guías
scripts/        la puerta de calidad y el ayudante del backlog
```

Las carpetas `engine/`, `game/` y `ui/` llegan con el prototipo; `audio/` tiene de momento las piezas básicas del
sintetizador.

Las versiones viven en `gradle/libs.versions.toml`: Kotlin 2.4.21, Compose Multiplatform 1.12.2, el plugin de Android
9.5.0 y Gradle 9.8.2, las estables del 2026-11-01.

## Cómo se ejecuta

- Android: `./gradlew :androidApp:installDebug` con un emulador o un teléfono conectado (para jugar, mejor
  `:androidApp:installRelease`, optimizada con R8 y firmada con la clave de depuración).
- iOS: abrir `iosApp/iosApp.xcodeproj` en Xcode y ejecutar el esquema `iosApp`, o por consola
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build`.
- Escritorio: `./gradlew :desktopApp:run`.

## Cómo se comprueba

```bash
bash scripts/verificar.sh
```

Termina en «La puerta pasa» si pasa todo: sin `!!`, ktlint, sin avisos del compilador, pruebas en la JVM, en Android y en
el simulador de iOS, lint de Android, los dos APK y la app de iOS ([proceso](docs/proceso.md)).

## El backlog

El tablero compartido está en `../backlog-juego-plataformas` (lo creó la sesión GameMaster). Para cambiar el estado de una
tarea:

```bash
python3 scripts/backlog.py estado "En curso" TEC-04
```

## Licencias

El código es de Fangusk. Todo el arte, la música y los sonidos se hacen desde cero para este juego
([propiedad intelectual](docs/propiedad-intelectual.md)).
