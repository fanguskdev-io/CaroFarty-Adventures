# La beta de CaroFarty

Aquí se deja cada APK de Android para instalar en el móvil. El APK no se versiona: se genera con
`./gradlew :androidApp:assembleRelease` y se copia aquí con la fecha en el nombre.

## La beta del 2026-11-02 (la cuarta)

`CaroFarty-beta-2026-11-02-4.apk`, 9,7 MB, versión de lanzamiento firmada con la clave de depuración de este Mac
(SHA-256 del fichero: `0ec7715b063202f4a97f52d9928ace9b87bdcbd6767902a8efcde555ae9cb871`), del commit `558c28a`. Es la
cuarta de ese día: sustituye a la tercera y añade la música de cada partida hecha mientras se juega, Caro con la cabeza
girada hacia donde va y el arreglo de la pausa (en Android, la música gastaba casi un núcleo mientras estaba en pausa).
Probada en el emulador y, la parte común, en el simulador de iOS; falta probarla en su Nothing Phone (3).

## Qué lleva

- **El arte de la interfaz de GameMaster** en todas las pantallas, como las maquetas que eligió Fangusk: el cielo de día
  con nubes, el título con el gatito y el perrito asomados, botones con iconos que se ven pulsados al tocarlos
  ([docs/pantallas.md](../docs/pantallas.md)).
- **Pantalla inicial** con una tarjeta por fase (Bosque, Nieve y Desierto) con su escenario en marcha y Caro corriendo;
  la última que jugó, en un **marco dorado**. Al tocar una fase empieza la partida; al acabar, «Otra vez» o «Inicio».
- **Récords por fase**: las 5 mejores partidas con medallas, sus metros, sus puntos máximos y la fecha (2 NOV 2026).
- **Cómo se juega**: dos páginas con dos viñetas que se juegan solas; sale la primera vez que se abre.
- **La pista «MANTÉN»** sobre Caro en los primeros barrancos de sus 3 primeras partidas, con un medidor que se llena
  mientras mantiene el salto.
- **Botón de silencio** junto al de pausa y en el inicio, que el juego recuerda.
- **Caro nueva**: carreras de 8 fotogramas, brazos del salto, cuello, **la cabeza girada hacia donde va** al correr y
  saltar, y la pose de retroceso; al coger un huevo o un pescado, **un retroceso y 1 s parpadeando** en el que no
  pierde más puntos.
- Gatitos de ocho pelajes, **perritos de nueve razas** que pasean, y huevos y pescados con su olor.
- Los tres escenarios con su suelo, sus bloques, su cielo, dos fondos que se mueven a distinta velocidad, su decoración y
  el fondo de los barrancos; en la Nieve, **la cabaña que sale a veces cerca y a veces lejos**.
- **La música que aprobó Fangusk**, distinta en cada escenario, con sus jingles y los efectos
  ([docs/sonido.md](../docs/sonido.md)). En cada partida, **una canción nueva hecha mientras se juega**, con capas que
  entran según corre más deprisa y su versión tensa con 3 puntos o menos; en los menús, los bucles.
- **La racha de rescates**: cada perrito o gatito seguido suena un grado más alto, hasta una octava.
- **El icono** «gorrito»: Caro con el gatito en la cabeza y el golden en brazos.

## Instalarla en un Android

**Con el cable** (y las opciones de desarrollador con la depuración USB activada):

```bash
~/Library/Android/sdk/platform-tools/adb install -r entregas/CaroFarty-beta-2026-11-02-4.apk
```

La primera vez, el móvil pregunta si permite la depuración USB desde este Mac: hay que aceptar en su pantalla.

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
