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
| Lo que se entrega (definiciones y paletas) | `arte/` en la raíz del repositorio, una carpeta por grupo: `arte/paletas/`, `arte/caro/`, `arte/recolectables/`, `arte/bosque/`, `arte/nieve/`, `arte/desierto/`, `arte/ui/` |
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
| `corre` | Corriendo, hasta 240 px/s | 6 a 8, en bucle | La pone la distancia: un ciclo cada 3 tiles, así que con 6 fotogramas cada uno dura 50 ms a 160 px/s; nunca menos de 40 ms, para que no se vea frenética |
| `corre-rapido` | Desde 240 px/s, con zancadas más largas | 6, en bucle | Un ciclo cada 5 tiles: 56 ms por fotograma a 240 px/s y 37 a 360, que se quedan en 40 como mínimo (propuesta de GameMaster en su guía 2) |

`corre` y `corre-rapido` pueden llevar `"ciclo-tiles"`: los tiles que avanza Caro en un ciclo entero de la animación. Si
no lo llevan, 3 y 5. Con más fotogramas hace falta un ciclo más largo, porque el mínimo de 40 ms por fotograma haría
patinar los pies: con 8 fotogramas y 3 tiles, ya desde 150 px/s.
| `salta` | Subiendo | 1 o 2 | Se queda en el último hasta el ápice |
| `apice` | Arriba del todo | 1 | Mientras apenas sube o baja |
| `cae` | Bajando | 1 o 2 | Se queda en el último |
| `aterriza` | Al tocar suelo | 1 o 2 | 0,1 s en total |
| `al-barranco` | Cayendo por un barranco | 2, en bucle | 100 ms |
| `sin-puntos` | Al quedarse sin puntos | 3 o 4 | 150 ms, y se queda en el último |
| `record` | En el panel de fin, con récord nuevo | 4, en bucle | 120 ms |

Las caras: `corre`, `corre-rapido`, `salta`, `apice`, `cae` y `aterriza` tienen una variante `-contenta` (al rescatar un
gatito o un perrito) y otra `-asco` (al coger un huevo o un pescado), con los mismos fotogramas y duraciones; el juego la
pone 0,4 s después de recoger algo, sin cortar el paso (CHR-07).

**Recolectables** (`arte/recolectables/recolectables.json`), cada uno un bucle de 2 a 4 fotogramas de 120 a 200 ms:

- `perrito-<raza>-camina`, de lado y mirando a la derecha (el juego lo voltea para la izquierda), y
  `perrito-<raza>-quieto`, para cada una de las nueve razas de la lista `razas` (shiba, pastor-aleman, carlino, dalmata,
  golden, beagle, yorkshire, bulldog-frances y corgi): los perritos del suelo pasean o se paran
  ([ADR 0008](adr/0008-perritos-que-caminan-y-gatitos-de-ocho-pelajes.md)) y cada raza tiene su paleta, la variante
  `perrito` → `<raza>`;
- `gatito-quieto`, sentado, con sus ocho pelajes como variantes de paleta;
- `huevo` y `pescado` (el huevo que chisporrotea);
- si se quiere, `recogido`, el efecto al tocarlo, de 3 fotogramas de 50 ms.

**Efectos** (`arte/efectos/efectos.json`): `olor`, las ondas de mal olor sobre el huevo y el pescado, de 16 × 8 con el
ancla abajo en el centro, `[8, 8]`.

## Las variantes de paleta

Un mismo dibujo con otros colores: los ocho pelajes de los gatitos y la ropa de Caro en cada escenario, las dos cosas
decisión de Fangusk.

- Cada variante es un `.gpl` completo, con los mismos índices y el mismo número de colores que la paleta base, para que se
  pueda abrir y ver en Aseprite.
- El JSON del sprite dice cuáles hay, con su nombre y su paleta:

```json
"variantes": {
  "gatito": {
    "naranja": "gatito-naranja",
    "gris-azulado": "gatito-gris-azulado",
    "calico": "gatito-calico",
    "crema": "gatito-crema",
    "negro": "gatito-negro",
    "blanco": "gatito-blanco",
    "chocolate": "gatito-chocolate",
    "siames": "gatito-siames"
  }
}
```

  y en `caro.json`, `"variantes": {"bosque": "caro-bosque", "nieve": "caro-nieve", "desierto": "caro-desierto"}`.
- El script comprueba que cada variante tiene los mismos colores que la base. El juego sortea el pelaje de cada gatito con
  la semilla y viste a Caro según el escenario.

**Escenarios** (`arte/<escenario>/tiles.json` y sus fondos): los tiles del suelo, con al menos dos variantes de la
superficie y del relleno, los bordes izquierdo y derecho (los del barranco tienen que leerse a cualquier velocidad) y sus
esquinas; el `bloque` en el aire, de 16 × 16, que encaje en filas de 1 a 3 y no se confunda con el suelo ni con los
recolectables (ART-25); los fondos lejano y medio; el cielo; y decoración opcional, que puede animarse con 2 a 4 fotogramas
de 250 ms.

## El script

`tools/arte.py` (TEC-11) convierte lo de `arte/` en lo que carga el juego. Necesita Python 3 con Pillow.

```bash
python3 tools/arte.py                  # lee arte/ y escribe shared/src/commonMain/composeResources/files/arte/
python3 tools/arte.py --comprobar      # no escribe: falla si lo generado no está al día (lo usa la puerta)
python3 tools/arte.py --desde OTRA/arte --hacia /tmp/arte    # probar una entrega sin instalarla
```

Comprueba lo que promete el formato y dice qué y dónde si algo no lo cumple: cada fila mide el ancho del fotograma, cada
carácter es un índice de su paleta, cada variante tiene tantos colores como su base y los PNG de los fondos no usan
colores fuera de su paleta. Genera, por cada JSON de sprites, una hoja PNG con la paleta base y otra por variante, con
los fotogramas en el mismo sitio en todas, y por cada escenario su cielo y sus fondos; y un `indice.json` con lo que el
juego necesita de cada hoja: tamaño, ancla, columnas, animaciones (fotogramas, ms y las filas vacías bajo el dibujo),
variantes y razas. La compilación lo ejecuta sola antes de preparar los recursos (la tarea `generarArte` de
`shared/build.gradle.kts`), así que cambiar un sprite y compilar basta para verlo; si la entrega no cumple el formato, la
compilación falla diciendo qué y dónde. Y la puerta de calidad (`scripts/verificar.sh`) comprueba que lo generado está al
día.

## La interfaz

El arte de los menús y del marcador (UI-02), en `arte/ui/` con la paleta `ui.gpl`, lo hizo GameMaster con las maquetas
que eligió Fangusk ([pantallas](pantallas.md)):

- **`ui.json`**: los colores del texto (`texto`: `color`, `sombra`, `secundario`, `aviso` y `record`, en `#rrggbb`) y las
  `piezas`, cada una con su `tamaño` y su `rejilla`. Las que se estiran llevan `9-slice` (las columnas y filas de cada
  lado, izquierda, arriba, derecha y abajo, que quedan como están mientras el medio se repite: nunca se escalan) y
  `contenido` (lo que dejar en cada lado para lo que va dentro: el texto, la vista de una tarjeta, una escena); la cinta,
  además, `alto-fijo`. Los botones pequeños, sin 9-slice, dicen dónde va su icono (`icono`). Son el panel, el botón, la
  pestaña, el marco, la tarjeta, la cinta, el botón pequeño, cada uno con su versión pulsada o inactiva si la tiene, y
  las dos nubes del fondo.
- **Los juegos de sprites**, con el formato de siempre: `iconos.json` (los de los botones y los dedos del tutorial, con
  `yema`, la punta del dedo), `cifras.json` (los números flotantes: `caracteres` y `avance`, en dorado para sumar y en
  violeta para restar), `medallas.json`, `puntos.json` (los de página) y `medidor.json` (vacío y lleno).
- **`titulo.png`**, el título ya dibujado, con los colores de `titulo.gpl`: el script comprueba que no usa otros.

El script lo convierte en `ui-<pieza>.png`, `ui-iconos.png` y los demás, y `ui-titulo.png`, con su sección `ui` en el
índice.

## Cómo lo coloca el juego

- **Lo que falte se dibuja en gris**: el juego arranca con todo, parte o nada del arte (TEC-18).
- **Caro**: su ancla va en el centro de sus pies. La pose sale de lo que hace: `espera` antes del primer toque; `corre` y,
  desde 240 px/s, `corre-rapido`, con la zancada al ritmo de la distancia; `salta` mientras sube a más de 80 px/s,
  `apice` arriba y `cae` al bajar (al caerse de un borde sin saltar, `cae` directamente); `aterriza` los 0,1 s del
  aterrizaje; `al-barranco` cuando ya no hay suelo que la recoja; `sin-puntos` al quedarse sin puntos y, si ha batido un
  récord, `record` en cuanto acaba. La ropa es la variante del escenario.
- **Los animales** pisan el suelo o la plataforma con la última fila de su dibujo, sea cual sea su fotograma: el juego
  descuenta las filas vacías que tengan debajo. Los perritos caminan o se paran según su paseo y miran hacia donde van;
  cada animal anima un poco a destiempo de los demás.
- **El olor**: su ancla, 6 px por encima del centro del huevo o del pescado.
- **El suelo**: `superficie` en la fila de arriba y `relleno` debajo, alternando la 1 y la 2 de forma fija por columna y
  fila; `borde-izq`/`relleno-izq` y `borde-der`/`relleno-der` en la columna de un extremo, pero solo en la parte que queda
  al aire: junto a un suelo más alto, el lado tapado lleva relleno normal. Los bloques, el tile `bloque`.
- **Los fondos**: el cielo, quieto y apoyado abajo; el lejano al 30 % de la cámara y el medio al 60 %, repetidos de lado y
  con su borde de abajo en la línea del suelo más bajo (32 px sobre el borde de la pantalla), menos las
  `filas-bajo-el-suelo`, que se ven dentro de los barrancos.
- **Las variantes de los fondos** (`"variantes"` en `fondos.json`, por capa: `{"medio": {"cabana":
  "fondo-medio-cabana.png"}, "lejano": {...}}`): la misma tira con algo más, del mismo tamaño y con los mismos colores, que
  el juego pone de vez en cuando en lugar de una vuelta de la tira. Fangusk, de la cabaña de la nieve: «intercala
  aleatoriamente entre cercana y lejana». El juego saca dónde está lo añadido comparando la variante con su base, y lo
  planea con la semilla, con la regla que propuso el agente de GameMaster: una sola a la vez; la primera, de 3 a 8 s
  después de empezar; cuando una sale de la pantalla, una pausa de 8 a 16 s; cerca o lejos al azar, nunca tres veces
  seguidas en la misma capa; y siempre en la vuelta siguiente de la tira, que entra por la derecha. Sale una cada unos 20 s.
- **La decoración** (`decoracion-<nombre>` en `tiles.json`, opcional): si el dibujo llega a la fila de abajo de su tile,
  se planta en el suelo, encima de más o menos una de cada seis piezas de superficie que no sean borde (la mata del
  bosque, el matojo del desierto); si no, llena el aire en mosaico a tresbolillo, con la fase cambiada en cada pieza,
  delante de los fondos y detrás del suelo, de Caro y de los recolectables, moviéndose con el suelo (los copos de la
  nieve). Las dos, detrás de lo que se recoge.

