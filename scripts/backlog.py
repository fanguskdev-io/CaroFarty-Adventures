#!/usr/bin/env python3
"""Cambia las tareas del backlog compartido y lo regenera.

El backlog vive fuera del repo, en ../backlog-juego-plataformas (lo creó la sesión GameMaster); la fuente de verdad son
las tablas de sus epicas/*.md y su tools/generar_backlog.py valida y regenera el CSV, el Excel y el resumen.

    python3 scripts/backlog.py estado "En curso" PRD-01 GDD-01
    python3 scripts/backlog.py texto GDD-13 "Nueva descripción con su **Hecho:** …"
    python3 scripts/backlog.py titulo GEN-08 "Nuevo título"
    python3 scripts/backlog.py deps ART-18 "ART-09, ART-21"
    python3 scripts/backlog.py hito M6 GEN-06 TEC-12
    python3 scripts/backlog.py nueva UI-19 "| UI-20 | Título | Descripción | P1 | S | M3 | GEN-08 | Por hacer |"
    python3 scripts/backlog.py fuera ART-15 "Motivo, que encabeza la descripción."
    python3 scripts/backlog.py ver GDD-13

`nueva` inserta la fila detrás de la tarea indicada; `fuera` la mueve a la tabla «Fuera de alcance» de su épica, con el
hito en «—», como las demás de esa tabla; `hito` cambia el hito de las tareas, por ejemplo a M6 lo que se deja para
después de la versión definitiva.
"""
import subprocess
import sys
from pathlib import Path

BACKLOG = Path(__file__).resolve().parent.parent.parent / "backlog-juego-plataformas"
ESTADOS = {"Por hacer", "En curso", "Hecho", "Fuera de alcance"}
COLUMNAS = 8  # ID | Tarea | Descripción | Prio | Est. | Hito | Depende de | Estado
TITULO, DESCRIPCION, HITO, DEPENDE, ESTADO = 1, 2, 5, 6, 7


def celdas_de(linea):
    if not linea.startswith("| ") or linea.startswith("| ID "):
        return None
    celdas = [c.strip() for c in linea.strip().strip("|").split(" | ")]
    return celdas if len(celdas) == COLUMNAS and "-" in celdas[0] else None


def fila(celdas):
    return "| " + " | ".join(celdas) + " |"


def buscar(id_tarea):
    """El fichero, sus líneas y el número de línea de la tarea."""
    for fichero in sorted((BACKLOG / "epicas").glob("*.md")):
        lineas = fichero.read_text(encoding="utf-8").splitlines()
        for n, linea in enumerate(lineas):
            celdas = celdas_de(linea)
            if celdas and celdas[0] == id_tarea:
                return fichero, lineas, n
    sys.exit(f"No encuentro: {id_tarea}")


def guardar(fichero, lineas):
    fichero.write_text("\n".join(lineas) + "\n", encoding="utf-8")


def cambiar(ids, columna, valor):
    for id_tarea in ids:
        fichero, lineas, n = buscar(id_tarea)
        celdas = celdas_de(lineas[n])
        celdas[columna] = valor
        lineas[n] = fila(celdas)
        guardar(fichero, lineas)
        print(f"{id_tarea}: {celdas[TITULO]} → {valor if columna in (ESTADO, DEPENDE, HITO) else 'cambiado'}")


def nueva(detras_de, linea):
    if celdas_de(linea) is None:
        sys.exit(f"La fila no tiene las {COLUMNAS} columnas: {linea}")
    fichero, lineas, n = buscar(detras_de)
    lineas.insert(n + 1, linea)
    guardar(fichero, lineas)
    print(f"{celdas_de(linea)[0]}: añadida detrás de {detras_de}")


def fuera(id_tarea, motivo):
    fichero, lineas, n = buscar(id_tarea)
    celdas = celdas_de(lineas.pop(n))
    # La tabla «Fuera de alcance» de la misma épica: la primera que viene detrás, antes de la épica siguiente.
    inicio = next((i for i in range(n, len(lineas)) if lineas[i].startswith("### Fuera de alcance")), None)
    siguiente = next((i for i in range(n, len(lineas)) if lineas[i].startswith("## EP")), len(lineas))
    if inicio is None or inicio > siguiente:
        sys.exit(f"{id_tarea}: su épica no tiene tabla «Fuera de alcance»")
    ultima = next(i for i in range(inicio, len(lineas)) if lineas[i].startswith("| ID "))
    while ultima + 1 < len(lineas) and lineas[ultima + 1].startswith("|"):
        ultima += 1
    celdas[DESCRIPCION] = f"{motivo} {celdas[DESCRIPCION]}"
    celdas[HITO] = "—"
    celdas[ESTADO] = "Fuera de alcance"
    lineas.insert(ultima + 1, fila(celdas))
    guardar(fichero, lineas)
    print(f"{id_tarea}: {celdas[TITULO]} → fuera de alcance")


def regenerar():
    # Con openpyxl también se regenera backlog.xlsx; sin él, el generador solo avisa y el Excel se queda atrás.
    candidatos = [BACKLOG / ".venv/bin/python", Path("/tmp/backlog-venv/bin/python")]
    interprete = next((str(c) for c in candidatos if c.exists()), sys.executable)
    subprocess.run([interprete, "tools/generar_backlog.py"], cwd=BACKLOG, check=True)


def main(args):
    orden = args[0] if args else ""
    if orden == "estado" and len(args) >= 3:
        if args[1] not in ESTADOS:
            sys.exit(f"Estado no válido: {args[1]} (válidos: {', '.join(sorted(ESTADOS))})")
        cambiar(args[2:], ESTADO, args[1])
    elif orden in ("texto", "titulo", "deps") and len(args) == 3:
        if "|" in args[2]:
            sys.exit("El texto no puede llevar «|»: rompería la tabla")
        cambiar([args[1]], {"texto": DESCRIPCION, "titulo": TITULO, "deps": DEPENDE}[orden], args[2])
    elif orden == "hito" and len(args) >= 3:
        cambiar(args[2:], HITO, args[1])
    elif orden == "nueva" and len(args) == 3:
        nueva(args[1], args[2])
    elif orden == "fuera" and len(args) == 3:
        fuera(args[1], args[2])
    elif orden == "ver" and len(args) >= 2:
        for id_tarea in args[1:]:
            _, lineas, n = buscar(id_tarea)
            print(" | ".join(celdas_de(lineas[n])))
        return
    else:
        sys.exit(__doc__)
    regenerar()


if __name__ == "__main__":
    main(sys.argv[1:])
