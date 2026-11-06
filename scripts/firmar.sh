#!/bin/bash
# El APK de lanzamiento firmado con la clave propia de Fangusk (TEC-14, docs/firma.md): el almacén fuera del repo, en
# ~/CaroFarty-firma/carofarty.p12 (o donde diga CAROFARTY_FIRMA), y su contraseña del llavero del Mac, servicio
# «carofarty-firma». Deja el APK en androidApp/build/outputs/apk/release/ y comprueba con apksigner que va firmado con
# ella. Con --aab hace además el AAB, el paquete que pide Google Play, en androidApp/build/outputs/bundle/release/, y
# comprueba su firma con jarsigner. Nada de esto sale del Mac ni entra en el repo.
#
#   bash scripts/firmar.sh          el APK
#   bash scripts/firmar.sh --aab    el APK y el AAB
set -euo pipefail
cd "$(dirname "$0")/.."

tareas=(:androidApp:assembleRelease)
if [ "${1:-}" = "--aab" ]; then
    tareas+=(:androidApp:bundleRelease)
fi

export CAROFARTY_FIRMA="${CAROFARTY_FIRMA:-$HOME/CaroFarty-firma/carofarty.p12}"
if [ ! -f "$CAROFARTY_FIRMA" ]; then
    echo "No está la clave en $CAROFARTY_FIRMA: cópiala de la copia de seguridad (docs/firma.md)." >&2
    exit 1
fi
if ! CAROFARTY_CLAVE="$(security find-generic-password -s carofarty-firma -a carofarty -w 2>/dev/null)"; then
    echo "No está la contraseña en el llavero (servicio carofarty-firma): guárdala de la copia (docs/firma.md)." >&2
    exit 1
fi
export CAROFARTY_CLAVE
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

./gradlew "${tareas[@]}" --no-configuration-cache -q
unset CAROFARTY_CLAVE

APK=androidApp/build/outputs/apk/release/androidApp-release.apk
APKSIGNER="$(ls -d "$HOME"/Library/Android/sdk/build-tools/*/apksigner | sort -V | tail -1)"
"$APKSIGNER" verify --print-certs "$APK" | grep -E "Signer #1 certificate (DN|SHA-256)"
echo "Firmado con la clave propia: $APK"

if [ "${1:-}" = "--aab" ]; then
    AAB=androidApp/build/outputs/bundle/release/androidApp-release.aab
    # El AAB va firmado como un JAR: jarsigner dice si la firma vale y con qué certificado.
    "$JAVA_HOME/bin/jarsigner" -verify -certs "$AAB" | grep -q "jar verified" ||
        { echo "El AAB no va bien firmado: $AAB" >&2; exit 1; }
    "$JAVA_HOME/bin/keytool" -J-Duser.language=en -printcert -jarfile "$AAB" | grep -E "Owner:|SHA256:" | head -2
    echo "Firmado con la clave propia: $AAB"
fi
