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
  `carofarty-firma`, cuenta `carofarty`) y en la nota de la carpeta. Nunca en el repo, en `local.properties` ni en los
  ficheros de Gradle.
- **Sin copia fuera del Mac**: Fangusk decidió el 2026-11-03 que no hace falta («no es necesario»). La clave se queda
  solo en este Mac: el almacén y `LEEME-CLAVE.txt` (el alias, la contraseña, la huella y cómo usarla) en
  `~/CaroFarty-firma`, y la contraseña también en el llavero. **Esa carpeta no se borra**: es la única nota de cómo se
  usa la clave. Si el Mac tiene copia de seguridad (Time Machine), la carpeta va en ella.

**Si se pierden la clave o su contraseña** (el Mac, la carpeta), ninguna versión nueva se podrá instalar encima de la que
tenga Caro: habría que desinstalarla, y se perderían sus récords.

## La versión

Todas las versiones se llaman «1.0». Su número, el que compara Android para instalar una encima de otra, es el de
commits del repo (`git rev-list --count HEAD`), así que cada APK va encima del anterior y el mismo commit da siempre el
mismo número. Sin git, 1.

## Firmar

```bash
bash scripts/firmar.sh
```

El script toma el almacén de `~/CaroFarty-firma/` (o de donde diga `CAROFARTY_FIRMA`) y la contraseña del llavero,
compila el APK de lanzamiento y comprueba con `apksigner` que va firmado con la clave propia: tiene que salir la huella
de arriba. Compila sin la caché de configuración de Gradle, para que la contraseña no se guarde en el disco.

Sin esas dos variables, el APK de lanzamiento sigue firmándose con la clave de depuración, para las pruebas y la puerta.
La app de medida siempre va con la de depuración.

## En otro ordenador

Con la carpeta copiada allí:

```bash
export CAROFARTY_FIRMA=/ruta/a/carofarty.p12
export CAROFARTY_CLAVE='la contraseña de LEEME-CLAVE.txt'
./gradlew :androidApp:assembleRelease --no-configuration-cache
```

Para dejarla otra vez en un Mac, se copia `carofarty.p12` a `~/CaroFarty-firma/` y la contraseña se guarda en el
llavero con `security add-generic-password -s carofarty-firma -a carofarty -w`, que la pide al pulsar Intro.
