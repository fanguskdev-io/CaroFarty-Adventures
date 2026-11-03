# La beta de CaroFarty

Aquí se deja cada APK de Android para instalar en el móvil. El APK no se versiona: se genera con
`./gradlew :androidApp:assembleRelease` y se copia aquí con la fecha en el nombre.

## La beta del 2026-11-03 (la octava)

`CaroFarty-beta-2026-11-03-8.apk`, 9,8 MB, versión de lanzamiento firmada con la clave de depuración de este Mac
(SHA-256 del fichero: `44688fc8abe438adc00fb64e1d260ecb47b0660fc457d901d6485f19927f768d`), del commit `0498095`.
Sustituye a la séptima y es **mucho más difícil**, como pidió Fangusk tras jugarla
([ADR 0011](../docs/adr/0011-mas-dificultad-tras-la-beta-7.md)): se empieza con 5 puntos y el huevo resta 3 y el pescado
5; hay muchos menos animales y más huevos y pescados, más difíciles de esquivar; plataformas de 1 y 2 tiles; y la
velocidad y la dificultad siguen subiendo hasta el minuto 30. Quedarse sin puntos ya es un riesgo de verdad. Probada en
el emulador y, la parte común, en el simulador de iOS; falta probarla en su Nothing Phone (3).

## La app de medida (AUD-29)

`CaroFarty-medida-2026-11-02.apk`, 9,8 MB (SHA-256 `77281408f6b548b3fe8f61f3a5314c9407840ea174172558b9d9cdac5ed2b9e6`),
de la beta 7. Ya cumplió su papel: Fangusk midió 30 minutos en su Nothing (60 FPS, sin fotogramas lentos) y se puede
desinstalar. Es otra app, «CaroFarty medida», que se instala aparte del juego y no toca sus récords. Juega sola con el
piloto automático, optimizada como la beta, para ver si el sonido y los fotogramas aguantan 30 minutos en su teléfono:

1. Instálala como la beta (abajo) y ábrela: empieza a jugar sola, con música, y la pantalla no se apaga.
2. Deja el móvil enchufado 30 minutos, con el volumen bajo pero sin silenciar, y escucha de vez en cuando por si hay
   cortes o chasquidos.
3. Al acabar, mira abajo a la izquierda la primera de las cuatro líneas rosas: `FPS … · PEOR … MS · LENTOS … %`. El
   dato es **LENTOS**, el porcentaje de fotogramas lentos desde que se abrió; apúntalo junto con **PEOR**.
4. Luego puedes desinstalarla: es solo para medir.

## Qué lleva

- **El arte de la interfaz de GameMaster** en todas las pantallas, como las maquetas que eligió Fangusk: el cielo de día
  con nubes, el título con el gatito y el perrito asomados, botones con iconos que se ven pulsados al tocarlos
  ([docs/pantallas.md](../docs/pantallas.md)).
- **La presentación** al abrir el juego: el título con el subtítulo «SALVANDO EL MUNDO, PATITA A PATITA», «TOCA PARA
  EMPEZAR» en frambuesa parpadeando y, abajo, el último escenario jugado pasando bajo un banco de nubes, con Caro
  corriendo y rescatando un gatito o un perrito cada 5 s. Un toque lleva a «Cómo se juega» la primera vez y al inicio
  después.
- **Pantalla inicial** con una tarjeta por fase (Bosque, Nieve y Desierto) con su escenario en marcha y Caro corriendo;
  la última que jugó, en un **marco dorado**. Al tocar una fase empieza la partida; al acabar, «Otra vez» o «Inicio».
  Arriba a la izquierda, **el botón de salir**: pregunta «¿SALIR DEL JUEGO?» y con «SÍ» cierra la app.
- **Más plataformeo** (ADR 0009): suelos más cortos, más barrancos, **plataformas** que se atraviesan saltando desde
  abajo y sobre las que se aterriza, barrancos que solo se cruzan saltando a ellas y **plataformas que se mueven** y la
  llevan: la hoja dorada, el trineo y la alfombra voladora. Las **plataformas con premio**, con un perrito o un gatito
  encima, también son semisólidas: se suben desde abajo. Desde esta beta, **algo más**: plataformas en uno de cada 2
  retos, más cadenas de 2 y 3 plataformas, a menudo **como escaleras** que suben o bajan, las móviles antes (desde
  225 px/s) y más a menudo, y las calmas más cortas.
- **Animales vigilados** (ADR 0010): a veces un huevo frito o un pescado junto a un animal, donde aterriza el salto. Unos
  se rescatan limpios con un salto bien medido; otros son un dilema: o el animal o no restar. Siempre hay un camino que
  no toca nada que reste. Pocos al principio y más con la distancia: desde esta beta, a toda dificultad, uno de cada 2
  animales, y casi siempre un dilema que cuesta 2 o 4 puntos.
- **Más difícil** (ADR 0011), desde esta beta: 5 puntos al empezar, el huevo a −3 y el pescado a −5; muchos menos
  animales (unos 4 cada 100 m al empezar y 1 a toda dificultad, por 11 y 9 en la séptima) y más huevos y pescados: en
  el camino del salto, en parejas, uno arriba y otro abajo, que obligan a medir la altura, donde se aterriza y entre
  plataformas. Quien va a por todos los animales se puede quedar sin puntos; quien los elige, no.
- **Plataformas pequeñas**: desde la mitad de la dificultad, las de los barrancos miden 1 o 2 tiles, quietas o móviles,
  para aterrizar y volver a saltar enseguida.
- **Hasta el minuto 30**: la velocidad sigue subiendo despacio tras los primeros minutos, hasta 460 px/s a los 10 y unos
  550 a los 30, y con ella la dificultad; a esas velocidades Caro corre más cerca del borde izquierdo para ver más por
  delante.
- **Récords por fase**: las 5 mejores partidas con medallas, sus metros, sus puntos máximos y la fecha (2 NOV 2026).
- **Cómo se juega**: dos páginas con dos viñetas que se juegan solas; sale la primera vez que se abre. Ya no se mezclan
  las dos viñetas de una página en el móvil y el salto no se sale del marco.
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
~/Library/Android/sdk/platform-tools/adb install -r entregas/CaroFarty-beta-2026-11-03-8.apk
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
