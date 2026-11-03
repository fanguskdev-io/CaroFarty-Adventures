# Licencias de terceros

El inventario de LEG-05: lo que lleva el juego que no hemos hecho nosotros y lo que piden sus licencias. Sale de las
dependencias de `gradle/libs.versions.toml` y del árbol que resuelve Gradle para el APK de lanzamiento
(`./gradlew :androidApp:dependencies --configuration releaseRuntimeClasspath`), revisado el 2026-11-03.

## Lo que es nuestro

- **La fuente pixel** de todos los textos (`PixelFont.kt`): dibujada en el código, sin fuente de terceros.
- **El arte**: Caro, los animales, los escenarios, la interfaz y el icono, dibujados por GameMaster con Fangusk
  ([arte](arte.md)).
- **La música y los sonidos**: hechos con el sintetizador propio, el de GameMaster portado a Kotlin
  ([música](musica.md), [sonido](sonido.md)).

Nada de eso lleva licencia de terceros ([propiedad intelectual](propiedad-intelectual.md)).

## Las librerías

| Librería | Versión | Para qué | Licencia |
|---|---|---|---|
| Kotlin: biblioteca estándar y Kotlin/Native | 2.4.21 | El lenguaje, en Android y en iOS | Apache 2.0 (JetBrains) |
| kotlinx.coroutines | 1.11.0 | Generar el recorrido en segundo plano y el audio | Apache 2.0 (JetBrains) |
| kotlinx.serialization (JSON) | 1.12.0 | Leer los ajustes y el índice del arte | Apache 2.0 (JetBrains) |
| Compose Multiplatform: runtime, foundation, ui, animation y resources | 1.12.2 | Dibujar el juego y leer sus recursos | Apache 2.0 (JetBrains, a partir de Jetpack Compose de Google) |
| Lifecycle y SavedState de JetBrains | 2.11.0 | Pausar al salir de la app | Apache 2.0 (JetBrains y The Android Open Source Project) |
| AndroidX: activity-compose 1.13.0, core-ktx 1.19.1 y las suyas (lifecycle, savedstate, collection, annotation, window, emoji2, profileinstaller, startup, tracing y otras) | Las que resuelve Gradle | La app de Android | Apache 2.0 (The Android Open Source Project) |
| Guava ListenableFuture, JSpecify y JetBrains Annotations | Las que resuelve Gradle | Las piden las de AndroidX | Apache 2.0 |
| Skiko y Skia | Las de Compose Multiplatform | Dibujar en iOS (Skia es el motor gráfico) | Skiko, Apache 2.0 (JetBrains); Skia, BSD de 3 cláusulas (Google) |

No van en la app: kotlin-test y las de las pruebas, ktlint y los plugins de Gradle, que solo sirven para construirla.

## Lo que piden

- **Apache 2.0**: quien reparte el programa compilado incluye una copia de la licencia y los avisos (NOTICE) que traigan
  esas librerías. No pide nombrarlas en pantalla, pero es lo habitual.
- **BSD de 3 cláusulas** (Skia, solo en iOS): reproducir su aviso de copyright, sus condiciones y su descargo en la
  documentación o en los materiales que acompañan a la app.

Así que los créditos (UI-16) llevan una línea con cada librería y su licencia, y el texto de la Apache 2.0 y el aviso
de Skia van dentro de la app, para leerlos sin internet ([ADR 0005](adr/0005-un-regalo-sin-conexion.md)): están en
`shared/src/commonMain/composeResources/files/licencias/`, con la lista de las librerías, y se leen desde «Textos» en los
créditos ([pantallas](pantallas.md#créditos-y-licencias)). El texto de los créditos lo eligió Fangusk: «A FANGUSK.DEV
GAME».

Si cambian las librerías, se cambian aquí, en `librerias.txt` y en `Texts.libraries`, la lista de los créditos.
