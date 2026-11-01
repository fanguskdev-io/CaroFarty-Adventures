# Cómo se entrega el arte

El formato en que llega el pixel art al juego (TEC-11, ART-02). El diseño de Caro, los recolectables y los escenarios lo
hacen Fangusk y la sesión GameMaster a partir de sus referencias; aquí se integra. Todo se dibuja desde cero
([propiedad intelectual](propiedad-intelectual.md)).

## La idea

El arte se **define en datos**: rejillas de texto en las que cada carácter es un píxel y dice qué color de una paleta fija
lleva. Un script del proyecto las convierte en las hojas PNG y sus metadatos que carga el juego, así que cambiar un sprite
y verlo en el juego no tiene pasos a mano. Las rejillas se leen y se comparan en git; las paletas fijas garantizan que
nada se sale de los colores elegidos. Aseprite es opcional para retocar: el script también convierte un PNG con los
colores de su paleta de vuelta a rejilla.

## Dónde

| Qué | Carpeta |
|---|---|
| Lo que se entrega (definiciones y paletas) | `arte/` en la raíz del repositorio, una carpeta por grupo: `arte/paletas/`, `arte/caro/`, `arte/recolectables/`, `arte/bosque/`, `arte/nieve/`, `arte/desierto/` |
| Vistas previas en PNG para que Fangusk las mire, si se hacen | `arte/<grupo>/previas/`, que el juego no lee |
| Lo que genera el script y carga el juego | `shared/src/commonMain/composeResources/files/arte/`: no se edita a mano |

Una entrega se avisa cuando está completa; hasta entonces no se integra ni se versiona.

## Las paletas

- Formato **GIMP (`.gpl`)**: lo abren Aseprite, LibreSprite y Lospec, y cada color lleva nombre («piel», «vestido
  sombra»…).
- Una para Caro y los recolectables (`arte/paletas/personajes.gpl`) y una por escenario (`bosque.gpl`, `nieve.gpl`,
  `desierto.gpl`), de 24 a 32 colores (ART-04).
- El orden de los colores es su índice: el primero es el `0`. La transparencia no es un color de la paleta.

## Las definiciones

Un JSON por grupo de sprites, con sus rejillas como listas de cadenas:

```json
{
  "paleta": "personajes",
  "tamaño": [24, 32],
  "ancla": [12, 32],
  "animaciones": {
    "corre": {
      "ms": 67,
      "fotogramas": [
        ["........................", "...........0000.........", "..."],
        ["..."]
      ]
    }
  }
}
```

- Cada cadena es una fila de píxeles, de arriba abajo, y todas miden el ancho del fotograma.
- `.` es transparente; `0`–`9`, `a`–`z` y `A`–`Z` son los índices 0 a 61 de la paleta.
- `ms` es lo que dura cada fotograma; una animación puede dar una lista con la duración de cada uno.
- `ancla` es el punto del sprite que el juego coloca en su sitio, en píxeles desde la esquina de arriba a la izquierda.
- Sin semitransparencias ni antialiasing: cada píxel es un color de la paleta o nada, para que se vea nítido a ×4, ×6 o ×8.

## Medidas

| Qué | Fotograma | Ancla | Notas |
|---|---|---|---|
| Caro | 24 × 32 px | `[12, 32]`: el suelo entre sus pies | Un cuerpo de unos 16 × 26 px deja sitio al pelo, los brazos y el vestido al saltar. Su caja de choque es de 10 × 20 px sobre el ancla y se ajusta al dibujo (CHR-08) |
| Recolectables | 16 × 16 px | `[8, 8]`: el centro | La caja de choque es de 16 × 16 px para lo que suma y de 10 × 10 para lo que resta (`items` en `files/ajustes.json`); la silueta, de al menos 10 × 10 |
| Tiles del suelo | 16 × 16 px | — | Encajan entre sí en horizontal y en vertical: los suelos miden de 3 a 20 tiles de largo y de 2 a 6 de alto |
| Fondos | Tiras de 256 o 512 px de ancho y hasta 270 de alto | — | Se repiten en horizontal sin costura: uno lejano (se mueve al 30 %) y uno medio (al 60 %) |
| Cielo | 1 × 270 px | — | Un degradado en bandas de colores de la paleta, sin mezclas |

Lo que se ve de una vez va de 320 × 180 a 448 × 270 px del juego, y el suelo queda entre 32 y 96 px por encima del borde
de abajo. A toda velocidad un recolectable cruza la pantalla en unos 2 segundos: las reglas para que se lean de un
vistazo están en [recolectables](recolectables.md).

## Las animaciones

Los nombres son los que busca el juego; las duraciones, una propuesta para ajustar al verlas.

**Caro** (`arte/caro/caro.json`):

| Nombre | Cuándo | Fotogramas | Duración |
|---|---|---|---|
| `espera` | Antes del primer toque | 4, en bucle | 150–250 ms |
| `corre` | Corriendo | 6 a 8, en bucle | La pone la distancia: un ciclo cada 3 tiles, así que a 120 px/s cada fotograma de 6 dura 67 ms y a 200 px/s, 40 ms |
| `corre-rapido` | Desde unos 170 px/s, si se hace | 6, en bucle | Igual que `corre` |
| `salta` | Subiendo | 1 o 2 | Se queda en el último hasta el ápice |
| `apice` | Arriba del todo | 1 | Mientras apenas sube o baja |
| `cae` | Bajando | 1 o 2 | Se queda en el último |
| `aterriza` | Al tocar suelo | 1 o 2 | 0,1 s en total |
| `al-barranco` | Cayendo por un barranco | 2, en bucle | 100 ms |
| `sin-puntos` | Al quedarse sin puntos | 3 o 4 | 150 ms, y se queda en el último |
| `record` | En el panel de fin, con récord nuevo | 4, en bucle | 120 ms |

**Recolectables** (`arte/recolectables/recolectables.json`): `perrito`, `gatito`, `huevo` y `pescado`, cada uno un bucle
de 2 a 4 fotogramas de 120 a 200 ms (la cola que se mueve, el huevo que chisporrotea), y si se quiere, `recogido`, el
efecto al tocarlo, de 3 fotogramas de 50 ms.

**Escenarios** (`arte/<escenario>/tiles.json` y sus fondos): los tiles del suelo, con al menos dos variantes de la
superficie y del relleno, los bordes izquierdo y derecho (los del barranco tienen que leerse a cualquier velocidad) y sus
esquinas; el `bloque` en el aire, de 16 × 16, que encaje en filas de 1 a 3 y no se confunda con el suelo ni con los
recolectables (ART-25); los fondos lejano y medio; el cielo; y decoración opcional, que puede animarse con 2 a 4 fotogramas
de 250 ms.
