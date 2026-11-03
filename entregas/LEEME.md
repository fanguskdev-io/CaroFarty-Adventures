# CaroFarty para Android

Aquí se deja cada APK de Android para instalar en el móvil. El APK no se versiona: la versión definitiva se genera con
`bash scripts/firmar.sh` ([la firma](../docs/firma.md)) y las betas, con `./gradlew :androidApp:assembleRelease`.

## La versión definitiva, 1.0 (2026-11-03)

`CaroFarty-1.0.apk`, 12 MB, del commit `2a89a4f`, firmada con la clave propia de Fangusk (la huella de su certificado
empieza por `7D:22:0E:DC` y está entera en [la firma](../docs/firma.md)). SHA-256 del fichero:
`076410d870d02ad1cfc553eff7c01f05d2b72833635352298cc4ea32f0a59c13`. Es reproducible: compilada otra vez desde ese commit
con `bash scripts/firmar.sh`, da el mismo fichero, byte a byte.

Es la beta 11 con lo de la beta 12: la pausa que se queda en su sitio al desbloquear el teléfono, una llamada que suena
que pausa la partida (si la música del juego está encendida) y un número de versión que crece con cada una.

**Antes de instalarla en un teléfono que tenga alguna beta, hay que desinstalar la beta**: van firmadas con otra clave y
Android no deja instalar una encima de la otra. Los récords de la beta se pierden al desinstalarla. En un teléfono que no
la haya tenido nunca, como el de Caro, se instala sin más. Las versiones que vengan después, firmadas con la misma clave,
se instalarán encima de esta sin perder nada.

Probada en el emulador, en un teléfono limpio, y, la parte común, en el simulador de iOS.

## La beta del 2026-11-03 (la undécima)

`CaroFarty-beta-2026-11-03-11.apk`, 12 MB, versión de lanzamiento firmada con la clave de depuración de este Mac
(SHA-256 del fichero: `152625e2efd779b90c26ee0671d109203f8032c40a6fc34aae8539aba9cfda81`), del commit `13776ce`.
Sustituye a la décima y trae el pulido, con el arte y los ambientes de GameMaster que Fangusk vio y escuchó («me parece
ok»):

- **Efectos**: polvo al saltar y al aterrizar, una estela al correr muy deprisa, chispas al rescatar, el olor que
  revienta al tocar un huevo o un pescado y plumas al tocar un pájaro; y los «+1» y «-3» que suben sobre Caro, como en
  «Cómo se juega».
- **Ambiente**: hojas que caen en el Bosque y la calima del Desierto; bajo la música, pájaros en el Bosque, viento en la
  Nieve y viento seco en el Desierto (se apagan con la música).
- **Fundidos** cortos entre pantallas.
- **Los sonidos en Android ya no paran el juego**: en un Xiaomi Mi 10 cada salto y cada aterrizaje se comían fotogramas.
- Si un teléfono no llega, quita primero las hojas y luego otros adornos, nunca el juego.

Probada en el emulador y, la parte común, en el simulador de iOS. El ambiente sonoro no se puede oír en el emulador.

## La beta décima

`CaroFarty-beta-2026-11-03-10.apk` (SHA-256 `f70df261be45c8899f4cc162c551e118aa0039ac121ce1b73cf46188f88452de`, commit
`ceea066`): la de los menús para la versión definitiva, como las maquetas de GameMaster
([pantallas](../docs/pantallas.md)):

- **La pausa** con su menú en columna: «Continuar», «Reiniciar», «Ajustes» e «Inicio». Tocar fuera ya no sigue la
  partida; al continuar, una cuenta 3, 2, 1 para tener el dedo listo.
- **Los ajustes**, desde el engranaje del inicio o desde la pausa: la música y los efectos, cada uno por su lado, y la
  vibración, que vibra solo con lo malo (al coger un huevo, un pescado o un pájaro, y al caer). El botón de sonido
  sigue apagándolo todo.
- **Los créditos**: «A FANGUSK.DEV GAME» y las librerías con sus licencias; en «Textos», los textos enteros.
- **El récord nuevo**: sus metros suben en grande y cae confeti.
- **La luz del Bosque**: a ratos, rayos de luz que se filtran entre las copas del fondo; es arte de la beta 11 de
  GameMaster, que ya está dentro.

La undécima la sustituye.

## La beta novena

`CaroFarty-beta-2026-11-03-9.apk` (SHA-256 `0bdb7ba6aed1289b0e0fa72bdbbc89fa4665373ef64a44ba51ae9ec3026f2908`, commit
`41aa4cb`): la de los peligros nuevos que eligió Fangusk ([ADR 0011](../docs/adr/0011-mas-dificultad-tras-la-beta-7.md)),
la tercera carrera desde 450 px/s y el final con gracia. La décima la sustituye.

## La beta octava

`CaroFarty-beta-2026-11-03-8.apk` (SHA-256 `44688fc8abe438adc00fb64e1d260ecb47b0660fc457d901d6485f19927f768d`, commit
`0498095`): la que endureció las reglas y el equilibrio. La novena la sustituye.

## La app de medida

`CaroFarty-medida-2026-11-03.apk`, 12 MB (SHA-256 `8562b0c1864795226c334dfb2ab0f02c94530c07047361d6dad089155f4e630a`),
del commit `1e0b4e9`: la de la beta 11, para el Xiaomi Mi 10 del amigo de Fangusk, que perdía fotogramas al saltar y
al aterrizar por los sonidos ([rendimiento](../docs/rendimiento.md)). Desde esta, los sonidos empiezan en su propio
hilo, y la quinta línea rosa dice `SONIDO … MS · EN SU HILO … MS · GC … PARADAS … MS`: lo que tardan los sonidos en el
hilo del juego (tiene que quedarse casi en 0) y en el suyo, y las paradas del recolector. Se instala encima de la
anterior. La medida que vale para comparar es **con el sonido puesto**, como la primera (`LENTOS 8,3 %`): cerrar la app
del todo, abrirla, dejarla unos 3 minutos y hacer una captura de las cinco líneas.

La anterior, `CaroFarty-medida-2026-11-02.apk` (SHA-256 `77281408f6b548b3fe8f61f3a5314c9407840ea174172558b9d9cdac5ed2b9e6`),
de la beta 7, ya cumplió su papel: Fangusk midió 30 minutos en su Nothing (60 FPS, sin fotogramas lentos). Las dos son
otra app, «CaroFarty medida», que se instala aparte del juego y no toca sus récords. Juega sola con el piloto
automático, optimizada como la beta, para ver si el sonido y los fotogramas aguantan en un teléfono:

1. Instálala como la beta (abajo) y ábrela: empieza a jugar sola, con música, y la pantalla no se apaga.
2. Deja el móvil enchufado 30 minutos, con el volumen bajo pero sin silenciar, y escucha de vez en cuando por si hay
   cortes o chasquidos.
3. Al acabar, mira abajo a la izquierda la primera de las líneas rosas: `FPS … · PEOR … MS · LENTOS … %`. El
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
  delante, y desde 450 px/s con **su carrera de sprint**, más inclinada y con la melena al viento.
- **Los peligros de la beta 9**: desde la mitad de la dificultad, **pájaros** que vienen de frente a tres alturas o van y
  vienen sobre los barrancos, y restan 8; **huevos y pescados que flotan** arriba y abajo; en el Bosque y la Nieve,
  **peces que saltan del agua** de los barrancos; **plataformas que tiemblan y caen** al pisarlas; y antes de algunos
  barrancos, **hielo** en la Nieve, que la hace ir más deprisa y saltar más lejos, y **arenas movedizas** en el Desierto,
  donde se hunde y salta más bajo.
- **El final con gracia**: «¡UPS! ¡AL AGUA!» (o «¡AL CAÑÓN!» en el Desierto), «¡PUAJ! DEMASIADO MAL OLOR» sin puntos y «¡AY!
  ¡QUÉ PICOTAZO!» si fue un pájaro, y debajo los animalitos salvados en la partida.
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
~/Library/Android/sdk/platform-tools/adb install -r entregas/CaroFarty-1.0.apk
```

La primera vez, el móvil pregunta si permite la depuración USB desde este Mac: hay que aceptar en su pantalla.

**Sin cable**:

1. Pasa el APK al móvil: por Google Drive, por correo, con Quick Share o copiándolo a la carpeta Descargas.
2. Ábrelo desde la app Archivos (o desde donde lo hayas descargado).
3. Android pedirá permiso para instalar apps de esa app: «Ajustes» → activa «Permitir de esta fuente» → vuelve atrás.
4. Pulsa «Instalar». Si avisa de que la app es de un desarrollador desconocido, es normal: no viene de Google Play.

Si ya había una versión anterior de CaroFarty firmada con otra clave, Android dirá que no se puede actualizar: desinstala
la anterior y vuelve a instalar. Los récords se pierden al desinstalar.

## Si algo falla

- El juego no necesita internet ni pide permisos: nada sale del teléfono.
- Si va a tirones, cuéntamelo con el modelo del móvil; la versión de lanzamiento (este APK) va más fluida que la de
  depuración.
