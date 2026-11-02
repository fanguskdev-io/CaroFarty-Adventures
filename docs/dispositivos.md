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

Lo dijo Fangusk el 2026-11-02 (a través de GameMaster):

| Dispositivo | Pantalla en horizontal | Escala y área visible | Uso |
|---|---|---|---|
| **Nothing Phone (3)**, Android | 2800×1260, 6,67″, AMOLED con refresco adaptativo hasta 120 Hz | ×7 · 400×180 | Su móvil de pruebas: la fluidez a 120 Hz (TEC-05), la carrera nueva y la legibilidad del arte (ART-18) |

No tiene iPhone: iOS se sigue compilando y probando en el simulador, y lo que pida un iPhone de verdad (firmarla con su
equipo, TEC-13 en iOS, MOB-04) queda aparcado hasta que él diga. El Samsung que estaba conectado por USB el 2026-11-01 no es
su móvil de pruebas.
