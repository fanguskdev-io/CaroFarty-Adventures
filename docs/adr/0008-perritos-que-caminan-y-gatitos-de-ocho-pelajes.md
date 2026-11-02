# ADR-0008 · Perritos que caminan y gatitos de ocho pelajes

| | |
|---|---|
| **Estado** | aceptada: el qué lo decidió Fangusk el 2026-11-01 en la sesión de diseño con GameMaster, que lo transmitió con sus palabras · propuesta: cómo caminan (velocidad, paradas, por dónde), que se afina jugando |
| **Fecha** | 2026-11-01 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M2 · **Tarea:** ITM-18, ITM-19, GEN-21 |
| **Propone · decide** | Fangusk · Fangusk |
| **Reversibilidad** | alta · el paseo vive en datos (`items` en `files/ajustes.json`) y los pelajes son variantes de paleta |

## Contexto

Al diseñar los recolectables con GameMaster, Fangusk decidió: «A diferencia de los gatitos, los perritos se moverán por el
escenario y además podrán estar quietos». Y que los gatitos tengan ocho pelajes al azar en todos los escenarios: naranja,
gris azulado, calicó, crema, negro, blanco, chocolate y siamés.

## Decisión

- **Los perritos del suelo pasean** por su tramo de suelo, de un lado a otro, con paradas, y algunos no se mueven nunca.
  Nunca se caen por un barranco: se dan la vuelta antes del borde. Propuesta de cómo, en `files/ajustes.json`:
  - pasean a 24 px/s, que a Caro, a 160–360, le parece casi quieto;
  - caminan de 1 a 3 s y se paran de 0,5 a 1,5 s, y 3 de cada 10 no caminan;
  - como mucho a 2 tiles de su sitio, a 2 tiles o más de los bordes de su suelo y a 3 tiles o más de los huevos y
    pescados de ese suelo.
  Así siempre se recogen corriendo, sin saltar ni tocar nada que reste, y una prueba lo comprueba.
- **Ningún animal en el aire.** Preguntado por los que salían flotando en los arcos sobre los barrancos y en el premio de
  la elección, Fangusk respondió (a través de GameMaster): «En el aire no puede aparecer ningún animal, salvo que esté en
  una plataforma». Arcos y elección se sustituyen por **plataformas con premio**: un bloque de 2 o 3 tiles sobre un tramo
  largo de suelo, con hueco para pasar corriendo por debajo, y encima un gatito sentado o un perrito que pasea y se da la
  vuelta en sus bordes. El validador comprueba que varios saltos aterrizan encima. Los huevos y los pescados pueden seguir
  en el aire (ADR 0007).
- **Los gatitos se quedan quietos**, sentados, y cada uno lleva uno de los ocho pelajes, sorteado con la semilla: la misma
  partida sale con los mismos gatitos en cualquier teléfono. El arte los dibuja con una sola rejilla y una paleta por
  pelaje ([arte](../arte.md)); en el prototipo en gris, el color del cuadrado da una idea de cada uno.

- **Nueve razas de perrito, el olor y las caras** (Fangusk, también a través de GameMaster, el mismo día):
  - los perritos son de nueve razas al azar, shiba, pastor alemán, carlino, dálmata, golden, beagle, yorkshire, bulldog
    francés y corgi, con los mismos movimientos; cambian de forma, así que cada raza tiene sus dibujos. Se sortean con la
    semilla, como los pelajes;
  - encima del huevo y del pescado salen unas ondas de «mal olor», 6 px por encima de su centro;
  - Caro pone cara de contenta al «rescatar» un gatito o un perrito y de asco al coger un huevo o un pescado. Cuánto dura
    es propuesta de GameMaster: 0,4 s (`reactionSeconds` en `files/ajustes.json`).

- **Cambio del 2026-11-02** ([ADR 0009](0009-mas-plataformeo-plataformas-semisolidas-y-moviles.md)): la plataforma con
  premio deja de ser un bloque y pasa a ser una plataforma semisólida, como las demás, que se atraviesa saltando desde
  abajo. Fangusk: «Vale, acepto tu recomendación».

## Consecuencias

- Tareas en el backlog: el paseo de los perritos (ITM-18) y los pelajes de los gatitos (ITM-19). Su arte (camina y
  quieto del perrito; quieto y ocho paletas del gatito) va con ITM-17, en manos de GameMaster.
- El validador no cambia: lo que suma no decide si una pieza es justa, y el paseo se limita para que recoger un perrito
  nunca obligue a nada.
