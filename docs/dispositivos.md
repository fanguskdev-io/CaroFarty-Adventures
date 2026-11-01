# Dispositivos de prueba

Dónde se prueba el juego (MOB-01). Las escalas salen del [ADR 0003](adr/0003-horizontal-y-pixel-perfecto.md).

## Para desarrollar

| Dispositivo | Pantalla en horizontal | Escala y área visible | Uso |
|---|---|---|---|
| Emulador Android `Tester_API36` (Android 16) | 2400×1080 | ×6 · 400×180 | El de cada prueba: el área mínima de alto |
| Emulador Android `Pixel_10_Pro_XL` (Android 17) | 2992×1344 | ×7 · 427×192 | Pantalla grande y escala impar |
| Simulador iPhone 17e (iOS 26.5) | 2532×1170 | ×6 · 422×195 | El iPhone más pequeño que trae Xcode |
| Simulador iPhone 17 Pro (iOS 26.5) | 2622×1206 | ×6 · 437×201 | Isla dinámica y esquinas redondeadas |
| Simulador iPad Pro 13″ | 2752×2064 | ×8 · 344×258 | La proporción más cuadrada |

Falta un emulador de Android de pantalla pequeña (720 px de alto, escala ×4); se crea al llegar a UI-14.

## Donde se jugará

**Pendiente de Fangusk**: el modelo y la versión de Android o iOS de los teléfonos donde se jugará, y si hay un iPhone,
si se paga el Apple Developer Program ([ADR 0005](adr/0005-un-regalo-sin-conexion.md)). Se fija al llegar a la instalación
(TEC-13).

El 2026-11-01 había un teléfono Samsung conectado por USB que aún no había autorizado la depuración.
