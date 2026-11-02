# Decisiones (ADR)

Cada decisión que costaría deshacer tiene aquí su registro: el contexto, la decisión, las alternativas reales con su
motivo de descarte y lo que cuesta cambiarla.

**Aceptada** quiere decir que la dictó Fangusk, con sus palabras; **Propuesta**, que la tomó el agente y el código la sigue
mientras Fangusk no diga otra cosa. Solo Fangusk pasa una propuesta a aceptada.

Fangusk validó M0, con sus ADR, el 2026-11-01: «tienes mi ok para M0».

Son un **borrador anticipado** del Mayeutic Harness ([docs/harness/STATUS.md](../harness/STATUS.md)): el proyecto está en P0
y estas decisiones existen antes de G4 porque Fangusk pidió construir ya. Las revisa la puerta que las pide.

| ADR | Decisión | Estado | Tarea |
|---|---|---|---|
| [0001](0001-motor-kotlin-multiplatform.md) | Kotlin Multiplatform y Compose Multiplatform, con un motor 2D propio sobre Canvas | Aceptada | TEC-01 |
| [0002](0002-salto-por-tiempo-de-pulsacion.md) | El salto crece con el tiempo que se mantiene el dedo, no con la presión | Aceptada | GDD-03 |
| [0003](0003-horizontal-y-pixel-perfecto.md) | En horizontal, 320×180 como mínimo, tiles de 16 px y escalado entero | Aceptada | GDD-04 |
| [0004](0004-generacion-por-reglas-con-validador.md) | Un recorrido generado por reglas y comprobado por un validador de saltos | Aceptada | GDD-07 |
| [0005](0005-un-regalo-sin-conexion.md) | Un regalo sin conexión: sin anuncios, analítica ni nada que salga del teléfono | Aceptada | PRD-01 |
| [0006](0006-tres-escenarios-a-elegir.md) | Tres escenarios distintos y ella elige dónde jugar | Aceptada; los récords por escenario, propuesta | GDD-12 |
| [0007](0007-la-dificultad-velocidad-y-obstaculos-en-el-aire.md) | La velocidad sigue subiendo y hay obstáculos en el aire: huevos y pescados en el camino del salto y bloques sólidos | Aceptada; los números y la colocación, propuesta | GDD-08, GDD-22 |
| [0008](0008-perritos-que-caminan-y-gatitos-de-ocho-pelajes.md) | Los perritos pasean o se quedan quietos; los gatitos, quietos y con ocho pelajes al azar; ningún animal en el aire, salvo en una plataforma | Aceptada; cómo pasean y las plataformas con premio, propuesta | ITM-18, ITM-19, GEN-21 |
| [0009](0009-mas-plataformeo-plataformas-semisolidas-y-moviles.md) | Más plataformeo: plataformas semisólidas y móviles, suelos más cortos y barrancos que solo se cruzan con plataformas | Aceptada; los números, propuesta | PLY-17, ITM-20, GEN-22, ART-26 |
