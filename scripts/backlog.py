#!/usr/bin/env python3
"""Cambia el estado o el texto de las tareas del backlog compartido y lo regenera.

El backlog vive fuera del repo, en ../backlog-juego-plataformas (lo creó la sesión GameMaster); la fuente de verdad son
las tablas de sus epicas/*.md y su tools/generar_backlog.py valida y regenera el CSV, el Excel y el resumen.

    python3 scripts/backlog.py estado "En curso" PRD-01 GDD-01
    python3 scripts/backlog.py texto GDD-13 "Nueva descripción con su **Hecho:** …"
    python3 scripts/backlog.py ver GDD-13
"""
import subprocess
import sys
from pathlib import Path

BACKLOG = Path(__file__).resolve().parent.parent.parent / "backlog-juego-plataformas"
ESTADOS = {"Por hacer", "En curso", "Hecho", "Fuera de alcance"}
COLUMNAS = 8  # ID | Tarea | Descripción | Prio | Est. | Hito | Depende de | Estado


def filas():
    """Cada fila de tarea como (fichero, número de línea, celdas)."""
    for fichero in sorted((BACKLOG / "epicas").glob("*.md")):
        for n, linea in enumerate(fichero.read_text(encoding="utf-8").splitlines()):
            if not linea.startswith("| ") or linea.startswith("| ID "):
                continue
            celdas = [c.strip() for c in linea.strip().strip("|").split(" | ")]
            if len(celdas) == COLUMNAS and "-" in celdas[0]:
                yield fichero, n, celdas


def cambiar(ids, columna, valor):
    pendientes = set(ids)
    for fichero, n, celdas in list(filas()):
        if celdas[0] not in pendientes:
            continue
        lineas = fichero.read_text(encoding="utf-8").splitlines()
        celdas[columna] = valor
        lineas[n] = "| " + " | ".join(celdas) + " |"
        fichero.write_text("\n".join(lineas) + "\n", encoding="utf-8")
        pendientes.discard(celdas[0])
        print(f"{celdas[0]}: {celdas[1]} → {valor if columna == 7 else 'texto nuevo'}")
    if pendientes:
        sys.exit(f"No encuentro: {', '.join(sorted(pendientes))}")


def regenerar():
    # Con openpyxl también se regenera backlog.xlsx; sin él, el generador solo avisa y el Excel se queda atrás.
    candidatos = [BACKLOG / ".venv/bin/python", Path("/tmp/backlog-venv/bin/python")]
    interprete = next((str(c) for c in candidatos if c.exists()), sys.executable)
    subprocess.run([interprete, "tools/generar_backlog.py"], cwd=BACKLOG, check=True)


def main(args):
    if len(args) >= 3 and args[0] == "estado":
        if args[1] not in ESTADOS:
            sys.exit(f"Estado no válido: {args[1]} (válidos: {', '.join(sorted(ESTADOS))})")
        cambiar(args[2:], 7, args[1])
    elif len(args) == 3 and args[0] == "texto":
        if "|" in args[2]:
            sys.exit("La descripción no puede llevar «|»: rompería la tabla")
        cambiar([args[1]], 2, args[2])
    elif len(args) >= 2 and args[0] == "ver":
        for _, _, celdas in filas():
            if celdas[0] in args[1:]:
                print(" | ".join(celdas))
        return
    else:
        sys.exit(__doc__)
    regenerar()


if __name__ == "__main__":
    main(sys.argv[1:])
