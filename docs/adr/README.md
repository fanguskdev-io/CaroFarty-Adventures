# Decisiones (ADR)

Cada decisión que costaría deshacer tiene aquí su registro: el contexto, la decisión, las alternativas reales con su
motivo de descarte y lo que cuesta cambiarla.

**Aceptada** quiere decir que la dictó Fangusk, con sus palabras; **Propuesta**, que la tomó el agente y el código la sigue
mientras Fangusk no diga otra cosa. Solo Fangusk pasa una propuesta a aceptada.

Son un **borrador anticipado** del Mayeutic Harness ([docs/harness/STATUS.md](../harness/STATUS.md)): el proyecto está en P0
y estas decisiones existen antes de G4 porque Fangusk pidió construir ya. Las revisa la puerta que las pide.

| ADR | Decisión | Estado | Tarea |
|---|---|---|---|
| [0001](0001-motor-kotlin-multiplatform.md) | Kotlin Multiplatform y Compose Multiplatform, con un motor 2D propio sobre Canvas | Aceptada en el qué; propuesta en el cómo | TEC-01 |
| [0002](0002-salto-por-tiempo-de-pulsacion.md) | El salto crece con el tiempo que se mantiene el dedo, no con la presión | Propuesta | GDD-03 |
| [0003](0003-horizontal-y-pixel-perfecto.md) | En horizontal, 320×180 como mínimo, tiles de 16 px y escalado entero | Propuesta | GDD-04 |
| [0004](0004-generacion-por-reglas-con-validador.md) | Un recorrido generado por reglas y comprobado por un validador de saltos | Propuesta | GDD-07 |
| [0005](0005-un-regalo-sin-conexion.md) | Un regalo sin conexión: sin anuncios, analítica ni nada que salga del teléfono | Aceptada en el qué; propuesta en el cómo | PRD-01 |
| [0006](0006-tres-escenarios-a-elegir.md) | Tres escenarios distintos y ella elige dónde jugar | Aceptada en el qué; propuesta en el cómo | GDD-12 |
