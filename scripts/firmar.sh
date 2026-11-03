#!/bin/bash
# El APK de lanzamiento firmado con la clave propia de Fangusk (TEC-14, docs/firma.md): el almacén fuera del repo, en
# ~/CaroFarty-firma/carofarty.p12 (o donde diga CAROFARTY_FIRMA), y su contraseña del llavero del Mac, servicio
# «carofarty-firma». Deja el APK en androidApp/build/outputs/apk/release/ y comprueba con apksigner que va firmado con
# ella. Nada de esto sale del Mac ni entra en el repo.
set -euo pipefail
cd "$(dirname "$0")/.."

export CAROFARTY_FIRMA="${CAROFARTY_FIRMA:-$HOME/CaroFarty-firma/carofarty.p12}"
if [ ! -f "$CAROFARTY_FIRMA" ]; then
    echo "No está la clave en $CAROFARTY_FIRMA: cópiala del pendrive (docs/firma.md)." >&2
    exit 1
fi
if ! CAROFARTY_CLAVE="$(security find-generic-password -s carofarty-firma -a carofarty -w 2>/dev/null)"; then
    echo "No está la contraseña en el llavero (servicio carofarty-firma): guárdala del pendrive (docs/firma.md)." >&2
    exit 1
fi
export CAROFARTY_CLAVE
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

./gradlew :androidApp:assembleRelease --no-configuration-cache -q
unset CAROFARTY_CLAVE

APK=androidApp/build/outputs/apk/release/androidApp-release.apk
APKSIGNER="$(ls -d "$HOME"/Library/Android/sdk/build-tools/*/apksigner | sort -V | tail -1)"
"$APKSIGNER" verify --print-certs "$APK" | grep -E "Signer #1 certificate (DN|SHA-256)"
echo "Firmado con la clave propia: $APK"
