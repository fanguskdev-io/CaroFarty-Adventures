# ADR-0003 · En horizontal, 320×180 como mínimo, tiles de 16 px y escalado entero

| | |
|---|---|
| **Estado** | propuesta |
| **Fecha** | 2026-11-01 · **Fase:** P0 · Borrador anticipado, sin ninguna puerta confirmada · **Hito:** M0 · **Tarea:** GDD-04 (base de TEC-04, PLY-13 y UI-14) |
| **Propone · decide** | agente (Claude Code, Opus 5.5) · Fangusk |
| **Reversibilidad** | media · el arte se dibuja para esta escala y el diseño de los tramos cuenta con esta altura |

## Contexto

Un plataformas horizontal como el dinosaurio de Chrome necesita ver lo que viene por delante. El pixel art de Super
Mario World se dibuja con tiles de 16 px en una pantalla de 256×224; en un móvil en horizontal esas proporciones dejarían
bandas a los lados o deformarían los píxeles. Y un píxel del juego tiene que medir siempre lo mismo en pantalla: si
unos miden 6 píxeles del teléfono y otros 7, el decorado tiembla al desplazarse.

## Decisión

- **Horizontal**, en cualquiera de los dos sentidos.
- **Tiles de 16×16 px y un área mínima de 320×180 px** (20×11,25 tiles, 16:9).
- **Escalado entero**: cada píxel del juego mide `s` píxeles del teléfono, con `s` el mayor entero que deja ver al menos
  320×180, es decir `s = ⌊min(ancho / 320, alto / 180)⌋`. El área visible crece hasta llenar la pantalla, sin bandas,
  hasta 448×270 px; más allá, bandas.

  | Pantalla en horizontal | Escala | Área visible |
  |---|---|---|
  | 1280×720 | ×4 | 320×180 |
  | Emulador Tester_API36, 2400×1080 | ×6 | 400×180 |
  | iPhone 17e, 2532×1170 | ×6 | 422×195 |
  | iPhone 17 Pro, 2622×1206 | ×6 | 437×201 |
  | Emulador Pixel 10 Pro XL, 2992×1344 | ×7 | 427×192 |
  | iPad Pro 13″, 2752×2064 | ×8 | 344×258 |

- **Dibujo píxel perfecto con desplazamiento suave**: el mundo se dibuja con coordenadas enteras en un búfer de baja
  resolución, un píxel más ancho que el área visible, que se escala sin suavizado. La parte fraccionaria de la cámara
  desplaza la imagen ya escalada (fracción × `s` píxeles del teléfono): el decorado avanza suave y todos los sprites siguen
  en la rejilla. La interfaz va en un segundo búfer que no se desplaza.
- **Encuadre**: Caro va fija a 96 px (6 tiles) del borde izquierdo. Por delante ve de 224 a 352 px según el teléfono:
  de 1,9 a 2,9 s de aviso a la velocidad inicial y de 1,1 a 1,8 s a la máxima.
- **Cámara vertical fija**: el suelo siempre está entre 2 y 6 tiles por encima del borde inferior del área mínima, así
  que el salto más alto (4,2 tiles) con Caro encima cabe sin mover la cámara. Una pantalla más alta enseña más cielo.

## Alternativas consideradas

| Alternativa | A favor | Por qué se descarta |
|---|---|---|
| 256×224, la pantalla de Super Mario World | Fidelidad total | En un móvil en horizontal deja bandas o deforma; los sprites se verían más pequeños |
| Escala no entera con vecino más próximo | Llena cualquier pantalla con el mismo tamaño de juego | Píxeles de tamaños distintos que tiemblan al desplazarse |
| Escalado con suavizado | Sin temblores | Emborrona el pixel art |
| Vertical | Se juega con una mano | Se ve muy poco de lo que viene: es lo que más importa en este juego |

## Consecuencias

- Todo el arte se dibuja a 1× para tiles de 16 px; Caro mide unos 16×24 px (se cierra en CHR-01 con las referencias).
- La generación tiene que respetar la banda de alturas del suelo ([generación](../generacion.md)).
- Hay que probar al menos 5 proporciones de pantalla (TEC-04, UI-14).
