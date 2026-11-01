# ADR-0002 · El salto crece con el tiempo que se mantiene el dedo

| | |
|---|---|
| **Estado** | propuesta · Fangusk pidió que la altura y la longitud dependan «de la presión o del tiempo»; elegir el tiempo es del agente |
| **Fecha** | 2026-11-01 · **Hito:** M0 · **Tarea:** GDD-03 |
| **Propone · decide** | agente (Claude Code, Opus 5.5) · Fangusk |
| **Reversibilidad** | alta · los números viven en datos (TEC-07) |

## Contexto

La especificación 4 de Fangusk: «El personaje correrá solo tras el usuario dar el tap inicial y la única acción que hará
será saltar cuando el usuario haga tap en la pantalla; la altura y longitud del salto vendrá definida por la presión o
tiempo que el usuario mantenga el tap».

La presión no sirve en un móvil de hoy: los iPhone perdieron el 3D Touch en 2019 (desde entonces `UITouch.force` vale 0) y
en Android `getPressure()` depende del fabricante; muchos teléfonos devuelven siempre el mismo valor.

## Decisión

- **El primer toque solo arranca la carrera**; Caro corre sola desde ese momento.
- **Cada toque siguiente es un salto que empieza al apoyar el dedo**, sin esperar a levantarlo, para que responda al
  instante.
- **Mantener el dedo alarga el salto**: mientras está apoyado, como mucho 0,36 s y mientras Caro sube, la gravedad es
  menor (300 px/s² en vez de 1600 px/s²). Al soltar, la gravedad normal corta la subida. Como Caro corre a velocidad
  constante, más tiempo en el aire es también un salto más largo.
- Con los números de [movimiento](../movimiento.md), a la velocidad inicial un toque rápido sube unos 1,8 tiles y
  avanza 3; una pulsación larga sube 4,2 tiles y avanza 5,6.
- **Coyote time de 0,08 s** (se puede saltar justo después de dejar el borde) y **buffer de 0,1 s** (un toque justo antes
  de aterrizar salta al aterrizar).
- Solo cuenta un dedo: otro toque mientras Caro está en el aire no hace nada.

## Alternativas consideradas

| Alternativa | A favor | Por qué se descarta |
|---|---|---|
| Presión | Es lo que nombró Fangusk junto al tiempo | No existe en los iPhone actuales y no es fiable en Android; dos jugadoras con el mismo gesto saltarían distinto según el teléfono |
| Saltar al levantar el dedo, con la altura según la duración | El salto se decide entero de una vez | Añade la duración del toque al retraso: el salto llega tarde justo cuando más falta hace |
| Dos botones (salto corto y largo) | Preciso | Rompe «la única acción es saltar al tocar» |

## Consecuencias

- La primera partida tiene que enseñar «mantén para saltar más» sin texto largo (GDD-17).
- Los valores finales se ajustan jugando (PLY-15); si la presión llegara a interesar, podría sumarse al tiempo solo en
  los teléfonos que la midan de verdad, sin cambiar el gesto.
