# La beta de CaroFarty

Aquí se deja cada APK de Android para instalar en el móvil. El APK no se versiona: se genera con
`./gradlew :androidApp:assembleRelease` y se copia aquí con la fecha en el nombre.

## La beta del 2026-11-02

`CaroFarty-beta-2026-11-02.apk`, 8,7 MB, versión de lanzamiento firmada con la clave de depuración de este Mac
(SHA-256 del fichero: `055d7a8de2374da69ed690dca7dfe43eff870a7cddb704330f586aedeadb30be`). Probada en el emulador de
Android 16; en el simulador de iOS va a 60 FPS. Lo único que falta es probarla en un teléfono de verdad.

## Qué lleva

- Caro con su arte, sus poses (espera, carrera, carrera rápida, salto, ápice, caída, aterrizaje, caída al barranco, sin
  puntos y récord) y su cara de contenta al rescatar un animal o de asco con un huevo o un pescado.
- Gatitos de ocho pelajes, perritos de nueve razas que pasean, y huevos y pescados con su olor.
- Los tres escenarios, Bosque, Nieve y Desierto, con su suelo, sus bloques, su cielo, dos fondos que se mueven a distinta
  velocidad y su decoración. Se eligen en el panel de inicio y en el de fin, y el juego recuerda el último.
- Música en bucle para cada escenario, jingles de inicio, fin y récord, y efectos al saltar, aterrizar, recoger, caer y
  pulsar botones. Son provisionales, de GameMaster, y nadie los había oído aún: cuándo suena cada cosa, en
  [docs/sonido.md](../docs/sonido.md). La música con capas que nunca aburre es de M4.
- Si bate un récord, Caro lo celebra a los dos lados del panel de fin.

## Instalarla en un Android

**Con el cable** (y las opciones de desarrollador con la depuración USB activada):

```bash
~/Library/Android/sdk/platform-tools/adb install -r entregas/CaroFarty-beta-2026-11-02.apk
```

La primera vez, el móvil pregunta si permite la depuración USB desde este Mac: hay que aceptar en su pantalla (ahora mismo
hay un Samsung conectado que sale como «unauthorized» porque no se ha aceptado).

**Sin cable**:

1. Pasa el APK al móvil: por Google Drive, por correo, con Quick Share o copiándolo a la carpeta Descargas.
2. Ábrelo desde la app Archivos (o desde donde lo hayas descargado).
3. Android pedirá permiso para instalar apps de esa app: «Ajustes» → activa «Permitir de esta fuente» → vuelve atrás.
4. Pulsa «Instalar». Si avisa de que la app es de un desarrollador desconocido, es normal: está firmada con la clave de
   depuración de este Mac y no viene de Google Play.

Si ya había una versión anterior de CaroFarty firmada con otra clave, Android dirá que no se puede actualizar: desinstala
la anterior y vuelve a instalar. Los récords se pierden al desinstalar.

## Si algo falla

- El juego no necesita internet ni pide permisos: nada sale del teléfono.
- Si va a tirones, cuéntamelo con el modelo del móvil; la versión de lanzamiento (este APK) va más fluida que la de
  depuración.
