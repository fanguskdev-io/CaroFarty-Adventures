# CaroFarty · para agentes

Lo que no se deduce del código. Si algo de aquí discrepa de su fuente, manda la fuente.

- **Qué es**: un plataformas infinito en pixel art para Android e iOS que Fangusk le regala a su esposa. Lo que pidió, con
  sus palabras, y las reglas: [docs/diseno.md](docs/diseno.md).
- **Decide Fangusk**: las decisiones están en [docs/adr/](docs/adr/README.md). Las «aceptadas» las dictó él; las
  «propuestas» las tomó un agente y solo él las acepta. No se deduce el producto: si algo no está dicho, se le pregunta.
- **Inspirarse, no copiar**: nada de sprites, tiles, música, sonidos ni nombres de Super Mario World ni de otras sagas
  ([docs/propiedad-intelectual.md](docs/propiedad-intelectual.md)).
- **Antes de diseñar** a Caro, los recolectables o los escenarios, se le piden a Fangusk sus referencias (PRD-11).
- **Nada sale del teléfono**: sin internet, anuncios ni analítica ([ADR 0005](docs/adr/0005-un-regalo-sin-conexion.md)).
- **El backlog** es `../backlog-juego-plataformas`, lo creó la sesión GameMaster y es el tablero compartido. El estado de
  cada tarea se cambia con `python3 scripts/backlog.py estado "En curso" ID…` (y «Hecho» al terminar).
- **Hecho quiere decir** que `bash scripts/verificar.sh` termina en «La puerta pasa» y que se ha probado en el emulador y
  en el simulador ([docs/proceso.md](docs/proceso.md)).
- El código y sus comentarios, en inglés; los documentos y los textos del juego, en español.
