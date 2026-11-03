# La firma de Android

Con qué clave se firma la app de Android y dónde está (TEC-14). Fangusk eligió una clave propia para la versión
definitiva, porque será la primera que instale Caro: «Caro no tiene la app instalada, la versión definitiva será la
primera que instale». Las betas iban firmadas con la clave de depuración de este Mac. Android no deja instalar encima
una app firmada con otra clave, así que en su teléfono Fangusk tiene que desinstalar la beta una vez, perdiendo sus
récords de prueba. Ya lo sabe.

## La clave

- **El almacén**: `~/CaroFarty-firma/carofarty.p12`, fuera del repo. Es un PKCS12 con una sola clave, de alias
  `carofarty`: RSA de 4096 bits, hecha el 2026-11-03 y válida hasta 2056.
- **La huella SHA-256 del certificado**:
  `7D:22:0E:DC:C1:5D:8F:C0:D9:A5:96:41:D7:47:5E:90:6A:8D:8F:7E:F4:86:4B:8A:67:99:E6:08:21:1F:B3:5D`.
- **La contraseña** sirve para el almacén y para la clave. Solo está en el llavero de este Mac (servicio
  `carofarty-firma`, cuenta `carofarty`) y en la copia de seguridad. Nunca en el repo, en `local.properties` ni en los
  ficheros de Gradle.
- **La copia de seguridad**, en un pendrive o disco externo, como eligió Fangusk: la carpeta `~/CaroFarty-firma` entera,
  con el almacén y `LEEME-CLAVE.txt` (el alias, la contraseña, la huella y cómo usarla). Cuando la copia esté en el
  pendrive y se abra, `LEEME-CLAVE.txt` se borra del Mac.

**Si se pierden la clave o su contraseña**, ninguna versión nueva se podrá instalar encima de la que tenga Caro: habría
que desinstalarla, y se perderían sus récords. Por eso la copia.

## Firmar

```bash
bash scripts/firmar.sh
```

El script toma el almacén de `~/CaroFarty-firma/` (o de donde diga `CAROFARTY_FIRMA`) y la contraseña del llavero,
compila el APK de lanzamiento y comprueba con `apksigner` que va firmado con la clave propia: tiene que salir la huella
de arriba. Compila sin la caché de configuración de Gradle, para que la contraseña no se guarde en el disco.

Sin esas dos variables, el APK de lanzamiento sigue firmándose con la clave de depuración, para las pruebas y la puerta.
La app de medida siempre va con la de depuración.

## En otro ordenador, o si este se pierde

Desde el pendrive:

```bash
export CAROFARTY_FIRMA=/ruta/a/carofarty.p12
export CAROFARTY_CLAVE='la contraseña de LEEME-CLAVE.txt'
./gradlew :androidApp:assembleRelease --no-configuration-cache
```

Para dejarla otra vez en un Mac, se copia `carofarty.p12` a `~/CaroFarty-firma/` y la contraseña se guarda en el
llavero con `security add-generic-password -s carofarty-firma -a carofarty -w`, que la pide al pulsar Intro.
