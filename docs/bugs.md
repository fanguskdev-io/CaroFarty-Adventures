# Lista de bugs

Los fallos encontrados y su revisión (QA-11). Cómo se apuntan, en [pruebas](pruebas.md). Se revisa al cerrar cada tarea
que toque lo afectado y, entera, al cerrar cada hito; para entregar, ninguno bloqueante ni crítico abierto.

## Gravedad

| Gravedad | Qué es |
|---|---|
| Bloqueante | No deja jugar, cierra la app o pierde los récords |
| Crítica | Rompe una regla del juego: un salto imposible, algo que resta inevitable, una caída sin barranco |
| Mayor | Se nota y molesta, pero se puede jugar: un tirón frecuente, un texto cortado, un toque que no responde |
| Menor | Un detalle |

## Abiertos

| N.º | Visto | Dónde | Qué pasa | Gravedad | Tarea |
|---|---|---|---|---|---|
| B-07 | 2026-11-01 | Lint de Android | Avisos sin efecto en el juego: `appCategory` solo cuenta desde Android 8, Android 16 ignora la orientación fija en pantallas grandes y falta el icono monocromo | Menor | UI-19 |
| B-09 | 2026-11-01 | Gradle | El plugin de recarga en caliente de Compose (`org.jetbrains.compose.hot`, de la plantilla, solo en el escritorio) usa `Configuration.setVisible`, que Gradle 10 quita | Menor | Al subir a Gradle 10: actualizar el plugin o quitarlo |

## Cerrados

| N.º | Visto | Dónde | Qué pasaba | Gravedad | Arreglo |
|---|---|---|---|---|---|
| B-01 | 2026-11-01 | Generador | Un obstáculo en el suelo justo después de un barranco podía no dejar sitio para saltarlo al aterrizar | Crítica | El validador exige aterrizar con todo el impulso para saltarlo por delante; el piloto juega 50 min sin tocar nada que reste |
| B-02 | 2026-11-01 | Generador | Lo que resta llegaba a 3,5 cada 100 m y no a los 8 de la especificación: nunca más de uno por pieza | Mayor | Hasta dos por pieza; medido, de 1,9 a 5,3 cada 100 m, porque el validador quita los que no puede hacer justos ([métricas](metricas.md)) |
| B-03 | 2026-11-01 | Pantallas de 1080 px de alto | En el salto más alto desde el suelo más alto, la cabeza de Caro salía por arriba | Menor | La cámara sube cuando hace falta (PLY-13) |
| B-04 | 2026-11-01 | iOS | La app pedía iOS 18.2 y la librería gráfica de Compose está compilada para 18.5 (aviso del enlazador) | Menor | Mínimo iOS 18.5: no deja fuera ningún iPhone que tuviera iOS 18 |
| B-05 | 2026-11-01 | Línea de depuración | A 60 FPS constantes marcaba 59 | Menor | Se redondea en vez de truncar |
| B-06 | 2026-11-01 | Línea de depuración | Dibujar la línea pesaba en lo que mide: el texto y Caro se pintaban píxel a píxel | Menor | Se pintan por tramos de una fila: de 5,9 a 4,5 ms por fotograma en el simulador de iOS |
| B-10 | 2026-11-01 | Física | Tras chocar de frente con la pared de un suelo más alto, al paso siguiente Caro podía atravesarla y caer por dentro: una caída sin barranco. Lo destapó la prueba de la pared al subir la velocidad | Crítica | La pared que está justo en su borde de delante también la frena |
| B-08 | 2026-11-01 | Escritorio | Los récords se guardaban sin forzar la escritura en disco: llegaban al cerrar bien la ventana, pero un cierre forzado los habría perdido | Menor | Se escriben en disco al guardarlos; están en `~/Library/Preferences/com.fanguskdev.carofarty.plist` |
