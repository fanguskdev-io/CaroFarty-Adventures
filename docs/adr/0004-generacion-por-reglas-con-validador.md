# ADR-0004 · Un recorrido generado por reglas y comprobado por un validador de saltos

| | |
|---|---|
| **Estado** | propuesta |
| **Fecha** | 2026-11-01 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M0 · **Tarea:** GDD-07 (base de GEN-01 a GEN-11) |
| **Propone · decide** | agente (Claude Code, Opus 5.5) · Fangusk |
| **Reversibilidad** | media · los tramos hechos a mano podrían añadirse después como una regla más |

## Contexto

La partida es infinita y tiene que parecer diseñada: con ritmo, sin repetirse y siempre justa. Un salto imposible en el
minuto ocho de una buena partida es lo peor que le puede pasar a este juego.

## Decisión

- El recorrido se arma por delante de la cámara con **tramos generados por reglas**: suelo a distintas alturas,
  barrancos y patrones de recolectables, con una dificultad que crece con la distancia y respiros cada cierto tiempo.
- **Cada tramo pasa por un validador** que simula los saltos con el mismo código de física del juego, con todas las
  duraciones de pulsación y todos los puntos de despegue. Si algún barranco no se puede cruzar con margen, o algún
  recolectable que resta no se puede esquivar, el tramo se vuelve a generar; tras varios intentos sale uno llano y seguro.
- **Aleatoriedad con semilla** y dos fuentes separadas, terreno y recolectables: la misma semilla da la misma partida en
  cualquier teléfono, y cambiar dónde caen los recolectables no cambia el terreno.
- Los detalles están en la [especificación de la generación](../generacion.md).

## Alternativas consideradas

| Alternativa | A favor | Por qué se descarta |
|---|---|---|
| Tramos hechos a mano (30–40 por escenario) con un editor como LDtk o Tiled | Cada tramo es un pequeño nivel cuidado | Mucho trabajo de diseño y una cadena de importación; en una partida larga los tramos se reconocen |
| Azar sin validador | Lo más sencillo | Tarde o temprano produce un salto imposible |
| Un nivel largo diseñado entero | Control total | No es infinito |

## Consecuencias

- Los tests pueden generar y jugar miles de tramos con un piloto automático (GEN-10, GEN-11).
- La variedad depende de cuántas reglas y patrones haya: se irán añadiendo sin tocar el validador.
