# La beta de CaroFarty

Aquí se deja cada APK de Android para instalar en el móvil. El APK no se versiona: se genera con
`./gradlew :androidApp:assembleRelease` y se copia aquí con la fecha en el nombre.

## La beta del 2026-11-02

`CaroFarty-beta-2026-11-02.apk`, 8,8 MB, versión de lanzamiento firmada con la clave de depuración de este Mac
(SHA-256 del fichero: `884dd5dfa2c19505c8cf67e7fecbd60f3ce573d737be7b104c796b76b579d01b`). Es la segunda de ese día:
lleva lo que pidió Fangusk al jugar la primera. Probada en el emulador y, la parte común, en el simulador de iOS; falta
probarla en su Nothing Phone (3).

## Qué lleva

- **Pantalla inicial** con el título, una tarjeta por fase (Bosque, Nieve y Desierto) con la vista de su escenario, los
  botones «Récords» y «Cómo se juega» y el de sonido. Al tocar una fase empieza la partida. Al acabar, «Otra vez» o
  «Inicio».
- **Récords por fase**: las 5 mejores partidas con sus metros, sus puntos máximos y la fecha.
- **Cómo se juega**: cuatro páginas con una partida en miniatura que se juega sola; sale la primera vez que se abre.
- **Botón de silencio** junto al de pausa y en el inicio, que el juego recuerda.
- Caro con su arte, sus poses y sus caras; al coger un huevo o un pescado, **un retroceso y 1 s parpadeando** en el que no
  pierde más puntos.
- Gatitos de ocho pelajes, **perritos de nueve razas** (el yorkshire con su lazo, cada raza de su tamaño) que pasean, y
  huevos y pescados con su olor.
- Los tres escenarios con su suelo, sus bloques, su cielo, dos fondos que se mueven a distinta velocidad, su decoración y
  el fondo de los barrancos nuevo; en la Nieve, **la cabaña que sale a veces cerca y a veces lejos**.
- Música en bucle por escenario, jingles y efectos, provisionales ([docs/sonido.md](../docs/sonido.md)). La música nueva,
  más distinta entre escenarios, aún no está.

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
