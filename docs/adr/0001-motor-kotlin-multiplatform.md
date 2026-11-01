# ADR-0001 · Kotlin Multiplatform y Compose Multiplatform, con un motor 2D propio

| | |
|---|---|
| **Estado** | aceptada: el qué, porque Fangusk creó CaroFarty con el asistente de Kotlin Multiplatform de Android Studio, como sus otras apps; el cómo, el motor propio sobre Canvas, con su visto bueno a M0: «tienes mi ok para M0» (Fangusk, 2026-11-01) |
| **Fecha** | 2026-11-01 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M0 · **Tarea:** TEC-01 |
| **Propone · decide** | agente (Claude Code, Opus 5.5) · Fangusk |
| **Reversibilidad** | baja una vez escrito el juego: cambiar de motor es reescribirlo |

## Contexto

El juego es un plataformas de desplazamiento horizontal infinito en pixel art para Android e iOS
([página de diseño](../diseno.md)). Lo que necesita es poco y concreto: un bucle de paso fijo, dibujar tiles y sprites a
baja resolución escalados sin suavizado, saber cuándo se apoya y se levanta un dedo, y sonido sintetizado en tiempo real.
No necesita física general, 3D, editor de escenas ni red.

Fangusk creó el proyecto con el asistente de Kotlin Multiplatform (Compose Multiplatform para Android, iOS y escritorio),
el mismo camino que MyLetters2.0 y BakiFace.

## Decisión

- **Kotlin Multiplatform y Compose Multiplatform**, con todo el juego en `shared/commonMain`: reglas, generación del
  recorrido, dibujo y síntesis del sonido. El código de cada plataforma se limita a lo que es suyo: la salida de audio
  (AudioTrack en Android, AVAudioEngine en iOS), el guardado y el ciclo de vida de la app.
- **Un motor 2D propio y pequeño sobre `Canvas`**: el mundo se dibuja con coordenadas enteras en un búfer de baja
  resolución (`ImageBitmap`) que luego se escala por un número entero y sin suavizado (`FilterQuality.None`), como
  explica el [ADR 0003](0003-horizontal-y-pixel-perfecto.md). Solo lo que el juego usa: nada de física general.
- **El escritorio se queda como herramienta**: capturas de cada pantalla sin abrir ventanas y pruebas rápidas. No se
  entrega.

## Alternativas consideradas

| Alternativa | A favor | Por qué se descarta |
|---|---|---|
| Godot 4 | Editor 2D excelente, píxel perfecto de serie, exporta a Android e iOS | Otro lenguaje (GDScript o C#) y otra cadena de herramientas fuera del ecosistema de Fangusk; tira el proyecto que ya existe |
| KorGE | Motor de juegos en Kotlin y multiplataforma | Un segundo framework con su propio audio y sus propios recursos; su integración con iOS y con Compose está menos rodada |
| libGDX | Maduro y usable desde Kotlin | En iOS depende de RoboVM/MobiVM, una cadena envejecida |
| Unity | El estándar del sector, muchos plugins | Pesado, C# y licencias para un juego de una sola pantalla de juego |

## Consecuencias y riesgos

- **Rendimiento del Canvas** con muchos sprites: búfer de baja resolución, atlas de imágenes, cero asignaciones de memoria
  por fotograma (TEC-17) y perfilado en los teléfonos (MOB-06).
- **Audio en tiempo real**: la música es generativa y se sintetiza mientras se juega; pide una salida de baja latencia en
  cada plataforma (AUD-25) y medir el coste (AUD-29).
- **Construir un motor cuesta**: se mantiene mínimo y probado con tests comunes que corren en la JVM y en el simulador de
  iOS.
