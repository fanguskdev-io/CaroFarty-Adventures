# La beta de CaroFarty

Aquí se deja cada APK de Android para instalar en el móvil. El APK no se versiona: se genera con
`./gradlew :androidApp:assembleRelease` y se copia aquí con la fecha en el nombre.

## Qué lleva la beta del 2026-11-02

- Caro con su arte, sus poses (espera, carrera, carrera rápida, salto, ápice, caída, aterrizaje, caída al barranco, sin
  puntos y récord) y su cara de contenta al rescatar un animal o de asco con un huevo o un pescado.
- Gatitos de ocho pelajes, perritos de nueve razas que pasean, y huevos y pescados con su olor.
- Los tres escenarios, Bosque, Nieve y Desierto, con su suelo, sus bloques, su cielo, dos fondos que se mueven a distinta
  velocidad y su decoración. Se eligen en el panel de inicio y en el de fin, y el juego recuerda el último.
- La música y los sonidos que haya preparado GameMaster para la beta (provisionales: la música con capas es de M4).

## Instalarla en un Android

**Con el cable** (y las opciones de desarrollador con la depuración USB activada):

```bash
adb install -r entregas/CaroFarty-beta-2026-11-02.apk
```

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
