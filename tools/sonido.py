#!/usr/bin/env python3
"""Instala la música y los sonidos de una entrega en los recursos del juego (AUD, beta).

    python3 tools/sonido.py --desde ../backlog-juego-plataformas/diseno/musica/beta

Copia <escenario>-bucle.wav a files/musica/<escenario>.wav y cada sonido o jingle a files/sonidos/<nombre>.wav, con los
nombres que busca el juego (audio/GameAudio.kt). Comprueba que cada uno es un WAV de PCM de 16 bits, mono o estéreo, que
es lo que leen las tres plataformas, y avisa de los que falten. No convierte nada: la entrega llega ya en mono a 22 050 Hz.
"""
import argparse
import shutil
import sys
import wave
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
RECURSOS = RAIZ / "shared" / "src" / "commonMain" / "composeResources" / "files"
ESCENARIOS = ("bosque", "nieve", "desierto")
SONIDOS = (
    "salto-corto", "salto-largo", "aterrizaje", "rescate-perrito", "rescate-gatito", "huevo", "pescado",
    "caida-barranco", "fin-partida", "record", "ui-pulsar", "ui-volver", "jingle-inicio", "jingle-record", "jingle-fin",
)


def comprobar(ruta):
    """Lo que dura el WAV en segundos, o un error si no es PCM de 16 bits."""
    try:
        with wave.open(str(ruta)) as w:
            if w.getsampwidth() != 2 or w.getnchannels() not in (1, 2) or w.getcomptype() != "NONE":
                return None, f"{ruta.name}: no es PCM de 16 bits, mono o estéreo"
            return w.getnframes() / w.getframerate(), None
    except (wave.Error, EOFError) as e:
        return None, f"{ruta.name}: {e}"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--desde", type=Path, required=True)
    args = parser.parse_args()
    pares = [(args.desde / f"{e}-bucle.wav", RECURSOS / "musica" / f"{e}.wav") for e in ESCENARIOS]
    pares += [(args.desde / f"{s}.wav", RECURSOS / "sonidos" / f"{s}.wav") for s in SONIDOS]
    # Los jingles de cada escenario, con sus instrumentos, si la entrega los trae; si no, suena el genérico.
    jingles = [s for s in SONIDOS if s.startswith("jingle-")]
    pares += [
        (args.desde / f"{j}-{e}.wav", RECURSOS / "sonidos" / f"{j}-{e}.wav")
        for j in jingles
        for e in ESCENARIOS
        if (args.desde / f"{j}-{e}.wav").exists()
    ]
    errores = 0
    for origen, destino in pares:
        if not origen.exists():
            # Algunas entregas dejan los efectos en una subcarpeta.
            otro = args.desde / "efectos" / origen.name
            if not otro.exists():
                print(f"sonido: falta {origen.name}")
                continue
            origen = otro
        segundos, error = comprobar(origen)
        if error:
            print(f"sonido: {error}")
            errores += 1
            continue
        destino.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(origen, destino)
        print(f"sonido: {destino.relative_to(RECURSOS)} ({segundos:.1f} s)")
    return 1 if errores else 0


if __name__ == "__main__":
    sys.exit(main())
