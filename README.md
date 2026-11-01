# CaroFarty

Un plataformas de desplazamiento horizontal infinito en pixel art para Android e iOS: Caro corre sola, salta barrancos y
recoge perritos y gatitos (que suman) esquivando huevos fritos y pescado (que restan). Es un regalo de Fangusk para su
esposa. La [página de diseño](docs/diseno.md) cuenta lo que pidió, las reglas y las decisiones.

> En construcción: M0 (diseño y decisiones) está validado y el prototipo en gris (M1) ya se juega en Android e iOS, a la
> espera de que Fangusk lo pruebe. Después vienen Caro, los recolectables y el Bosque con su arte (M2).

## Estructura

```
shared/         el juego entero, común a Android, iOS y el escritorio (Kotlin Multiplatform + Compose Multiplatform)
  src/commonMain/kotlin/com/fanguskdev/carofarty/
    engine/       motor 2D: bucle de paso fijo, escalado píxel perfecto y el dedo
    game/         reglas: Caro, el recorrido y su validador, los recolectables, los puntos y el piloto automático
    audio/        síntesis de sonido y música generativa (M4)
    storage/      lo que se guarda en el teléfono: los récords
    ui/           la pantalla de juego: la cámara, el dibujo, el marcador, los paneles y la línea de depuración
  src/androidMain, src/iosMain, src/jvmMain   lo que es de cada plataforma
androidApp/     la app de Android
iosApp/         la app de iOS (proyecto de Xcode)
desktopApp/     herramienta de desarrollo: una ventana para probar rápido; no se entrega
docs/           diseño, decisiones (ADR) y guías
scripts/        la puerta de calidad y el ayudante del backlog
```

`audio/` tiene de momento las piezas básicas del sintetizador. Los ajustes del juego (velocidades, salto, recorrido y
puntos) están en `shared/src/commonMain/composeResources/files/ajustes.json`.

Las versiones viven en `gradle/libs.versions.toml`: Kotlin 2.4.21, Compose Multiplatform 1.12.2, el plugin de Android
9.5.0 y Gradle 9.8.2, las estables del 2026-11-01.

## Cómo se ejecuta

- Android: `./gradlew :androidApp:installDebug` con un emulador o un teléfono conectado (para jugar, mejor
  `:androidApp:installRelease`, optimizada con R8 y firmada con la clave de depuración).
- iOS: abrir `iosApp/iosApp.xcodeproj` en Xcode y ejecutar el esquema `iosApp`, o por consola
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build`.
- Escritorio: `./gradlew :desktopApp:run`.

La versión de depuración trae herramientas: la línea de depuración (un toque con un segundo dedo) y un piloto automático
que juega solo. Cómo arrancar con ellas, en [pruebas](docs/pruebas.md); qué mide la línea, en
[rendimiento](docs/rendimiento.md).

## Cómo se comprueba

```bash
bash scripts/verificar.sh
```

Termina en «La puerta pasa» si pasa todo: sin `!!`, ktlint, sin avisos del compilador, pruebas en la JVM, en Android y en
el simulador de iOS, lint de Android, los dos APK y la app de iOS ([proceso](docs/proceso.md)).

## El arnés

El proyecto usa el Mayeutic Harness para el desarrollo, como los demás de Fangusk: dónde está y qué sigue, en
[docs/harness/STATUS.md](docs/harness/STATUS.md). La CI `mayeutic` (`.github/workflows/mayeutic.yml`) corre la misma puerta
sin la parte de iOS cuando el repositorio esté en GitHub. Lo que el arnés comprueba del proyecto (pide PyYAML):

```bash
python3 ../mayeutic-harness/tools/proyecto.py comprobar .
```

## El backlog

El tablero compartido está en `../backlog-juego-plataformas` (lo creó la sesión GameMaster). Para cambiar el estado de una
tarea:

```bash
python3 scripts/backlog.py estado "En curso" TEC-04
```

## Licencias

El código es de Fangusk. Todo el arte, la música y los sonidos se hacen desde cero para este juego
([propiedad intelectual](docs/propiedad-intelectual.md)).
