#!/usr/bin/env bash
# scripts/verificar.sh · la puerta de calidad de CaroFarty (docs/proceso.md). La misma orden para todo el proyecto.
#
#   bash scripts/verificar.sh                 todo: estilo, pruebas, lint, APK y, en macOS, iOS
#   CAROFARTY_SKIP_IOS=1 bash scripts/...    sin la parte de iOS (una máquina Linux no puede compilarla)
#
# Corre cada paso aunque falle el anterior y falla al final si alguno falló. Lo que comprueba, en orden:
#   1. que ningún fichero de Kotlin use «!!» (null-safety sin atajos);
#   2. ktlint, las pruebas comunes en la JVM y en el host de Android, el lint de Android y los APK de depuración y de
#      lanzamiento (este, con R8); cualquier aviso del compilador rompe la construcción (build.gradle.kts raíz);
#   3. en macOS: las pruebas comunes en el simulador de iOS y la app de iOS compilada con xcodebuild.
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

if [[ -z "${JAVA_HOME:-}" && -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

failed=()

step() {
    local name=$1
    shift
    printf '\n▶ %s\n' "$name"
    if "$@"; then
        printf '✓ %s\n' "$name"
    else
        printf '✗ %s\n' "$name"
        failed+=("$name")
    fi
}

no_double_bang() {
    local found
    found=$(grep -rn '!!' shared androidApp desktopApp --include='*.kt' | grep -v '/build/' || true)
    if [[ -n "$found" ]]; then
        printf '%s\n' "$found"
        return 1
    fi
}

step "Sin «!!» en el código" no_double_bang

step "Estilo, pruebas, lint y APK" ./gradlew --continue \
    ktlintCheck \
    :shared:jvmTest :shared:testAndroidHostTest \
    :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease

if [[ "$(uname)" == "Darwin" && "${CAROFARTY_SKIP_IOS:-0}" != "1" ]]; then
    step "Pruebas en el simulador de iOS" ./gradlew --continue :shared:iosSimulatorArm64Test

    simulator=$(xcrun simctl list devices available | grep -E '^\s+iPhone' | head -1 | grep -oE '[0-9A-F-]{36}')
    step "App de iOS para el simulador (xcodebuild)" xcodebuild -quiet \
        -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
        -destination "platform=iOS Simulator,id=${simulator}" \
        -derivedDataPath build/xcode build
fi

printf '\n'
if (( ${#failed[@]} > 0 )); then
    printf 'La puerta NO pasa. Han fallado:\n'
    printf '  - %s\n' "${failed[@]}"
    exit 1
fi
printf 'La puerta pasa.\n'
