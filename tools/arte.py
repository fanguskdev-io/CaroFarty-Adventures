#!/usr/bin/env python3
"""Convierte el arte de arte/ en las hojas PNG y el índice que carga el juego (TEC-11, docs/arte.md).

    python3 tools/arte.py                        lee arte/ y escribe en shared/src/commonMain/composeResources/files/arte/
    python3 tools/arte.py --desde OTRA/arte --hacia /tmp/arte   para probar una entrega antes de instalarla
    python3 tools/arte.py --comprobar            no escribe nada: falla si lo generado no está al día con arte/

Comprueba lo que promete el formato: cada fila mide el ancho del fotograma, cada carácter es un índice de su paleta, las
variantes tienen tantos colores como su base y los PNG no usan ningún color fuera de su paleta.

Lo que genera, por cada JSON de sprites (caro, recolectables, efectos y los tiles de cada escenario):
- una hoja PNG con la paleta base y una más por variante, con los fotogramas en el mismo sitio en todas;
- su entrada en indice.json: la hoja, tamaño, ancla, columnas, animaciones (fotogramas, ms y las filas vacías bajo el
  dibujo, para posar a los animales sobre el suelo), variantes (familia, nombre y hoja) y razas.
Y por cada escenario, su cielo y sus fondos en PNG con transparencia, y las filas que quedan bajo el suelo.
"""
import argparse
import hashlib
import io
import json
import sys
from pathlib import Path

try:
    from PIL import Image
except ModuleNotFoundError:  # pragma: no cover - la puerta lo dice y sigue con lo ya generado
    Image = None

RAIZ = Path(__file__).resolve().parent.parent
ORIGEN = RAIZ / "arte"
DESTINO = RAIZ / "shared" / "src" / "commonMain" / "composeResources" / "files" / "arte"
INDICES = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
TRANSPARENTE = "."
COLUMNAS = 16
ESCENARIOS = ("bosque", "nieve", "desierto")
FONDOS = (("cielo", "cielo"), ("fondo-lejano", "lejano"), ("fondo-medio", "medio"))


class ArteMal(Exception):
    """Algo de la entrega no cumple el formato: el mensaje dice qué y dónde."""


def leer_paleta(ruta):
    """Los colores de un .gpl de GIMP, en orden: el primero es el índice 0."""
    colores = []
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        partes = linea.split()
        if len(partes) >= 3 and all(p.isdigit() for p in partes[:3]):
            colores.append(tuple(int(p) for p in partes[:3]))
    if not colores:
        raise ArteMal(f"{ruta.name}: la paleta no tiene colores")
    return colores


def buscar_paleta(origen, nombre):
    encontradas = sorted(origen.rglob(f"{nombre}.gpl"))
    if not encontradas:
        raise ArteMal(f"falta la paleta «{nombre}.gpl»")
    return leer_paleta(encontradas[0])


def duraciones(animacion, fotogramas, donde):
    ms = animacion.get("ms", 0)
    if isinstance(ms, list):
        if len(ms) != fotogramas:
            raise ArteMal(f"{donde}: {len(ms)} duraciones para {fotogramas} fotogramas")
        return ms
    return [ms] * fotogramas


def variantes_de(datos, origen, base):
    """
    Las variantes de un JSON como (familia, nombre, colores), en su orden. Admite un mapa plano, sin familia (la ropa de
    Caro por escenario), o uno por familia (los pelajes del gatito, las razas del perrito).
    """
    salida = []
    for clave, valor in datos.get("variantes", {}).items():
        pares = [(clave, n, p) for n, p in valor.items()] if isinstance(valor, dict) else [("", clave, valor)]
        for familia, nombre, paleta in pares:
            colores = buscar_paleta(origen, paleta)
            if len(colores) != len(base):
                raise ArteMal(f"la variante «{paleta}» tiene {len(colores)} colores y su base {len(base)}")
            salida.append((familia, nombre, colores))
    return salida


def leer_sprites(ruta, origen):
    """Un JSON de sprites, comprobado: su paleta, sus animaciones como listas de rejillas y sus variantes."""
    datos = json.loads(ruta.read_text(encoding="utf-8"))
    base = buscar_paleta(origen, datos["paleta"])
    ancho, alto = datos["tamaño"]
    animaciones = {}
    for nombre, animacion in datos["animaciones"].items():
        fotogramas = animacion["fotogramas"]
        for f, rejilla in enumerate(fotogramas):
            donde = f"{ruta.parent.name}/{ruta.name}, «{nombre}», fotograma {f}"
            if len(rejilla) != alto:
                raise ArteMal(f"{donde}: {len(rejilla)} filas y el fotograma mide {alto}")
            for y, fila in enumerate(rejilla):
                if len(fila) != ancho:
                    raise ArteMal(f"{donde}, fila {y}: {len(fila)} píxeles y el fotograma mide {ancho}")
                for x, c in enumerate(fila):
                    if c != TRANSPARENTE and (c not in INDICES or INDICES.index(c) >= len(base)):
                        raise ArteMal(f"{donde}, ({x}, {y}): «{c}» no es un índice de la paleta «{datos['paleta']}»")
        animaciones[nombre] = (fotogramas, duraciones(animacion, len(fotogramas), f"«{nombre}»"))
    return datos, base, animaciones, variantes_de(datos, origen, base)


def rejilla_png(rejilla, ancho, alto, paleta, donde):
    """Una rejilla comprobada como imagen RGBA: las piezas de la interfaz, que no son animaciones."""
    if len(rejilla) != alto:
        raise ArteMal(f"{donde}: {len(rejilla)} filas y la pieza mide {alto}")
    imagen = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pixeles = imagen.load()
    for y, fila in enumerate(rejilla):
        if len(fila) != ancho:
            raise ArteMal(f"{donde}, fila {y}: {len(fila)} píxeles y la pieza mide {ancho}")
        for x, c in enumerate(fila):
            if c == TRANSPARENTE:
                continue
            if c not in INDICES or INDICES.index(c) >= len(paleta):
                raise ArteMal(f"{donde}, ({x}, {y}): «{c}» no es un índice de su paleta")
            r, g, b = paleta[INDICES.index(c)]
            pixeles[x, y] = (r, g, b, 255)
    return imagen


def convertir_ui(origen, salida, indice):
    """
    La interfaz (UI-02): las piezas de ui.json (paneles, botones, pestañas, marcos, tarjetas y cinta en 9-slice, y las
    fijas), los colores del texto, las hojas de iconos, cifras, medallas, puntos y medidor, y el título.
    """
    carpeta = origen / "ui"
    datos = json.loads((carpeta / "ui.json").read_text(encoding="utf-8"))
    paleta = buscar_paleta(origen, datos["paleta"])
    piezas = {}
    for nombre, pieza in datos.get("piezas", {}).items():
        ancho, alto = pieza["tamaño"]
        imagen = rejilla_png(pieza["rejilla"], ancho, alto, paleta, f"ui/ui.json, «{nombre}»")
        hoja_pieza = f"ui-{nombre}.png"
        salida[hoja_pieza] = png(imagen)
        entrada = {"hoja": hoja_pieza, "tamaño": [ancho, alto]}
        for clave in ("9-slice", "contenido", "alto-fijo", "icono"):
            if clave in pieza:
                entrada[clave] = pieza[clave]
        piezas[nombre] = entrada
    entrada_ui = {"piezas": piezas, "texto": datos.get("texto", {})}
    for grupo in ("iconos", "cifras", "medallas", "puntos", "medidor"):
        ruta = carpeta / f"{grupo}.json"
        if ruta.exists():
            convertir_sprites(f"ui-{grupo}", ruta, origen, salida, indice)
    if (carpeta / "titulo.png").exists():
        titulo = convertir_fondo(carpeta / "titulo.png", buscar_paleta(origen, "titulo"), "ui/titulo.png")
        salida["ui-titulo.png"] = png(titulo)
        entrada_ui["titulo"] = "ui-titulo.png"
    indice["ui"] = entrada_ui


def hoja(animaciones, ancho, alto, paleta):
    """Todos los fotogramas en una hoja de COLUMNAS de ancho, en el orden de las animaciones."""
    total = sum(len(f) for f, _ in animaciones.values())
    filas = (total + COLUMNAS - 1) // COLUMNAS
    imagen = Image.new("RGBA", (COLUMNAS * ancho, max(1, filas) * alto), (0, 0, 0, 0))
    pixeles = imagen.load()
    n = 0
    for fotogramas, _ in animaciones.values():
        for rejilla in fotogramas:
            x0, y0 = (n % COLUMNAS) * ancho, (n // COLUMNAS) * alto
            for y, fila in enumerate(rejilla):
                for x, c in enumerate(fila):
                    if c != TRANSPARENTE:
                        r, g, b = paleta[INDICES.index(c)]
                        pixeles[x0 + x, y0 + y] = (r, g, b, 255)
            n += 1
    return imagen


def png(imagen):
    buffer = io.BytesIO()
    imagen.save(buffer, format="PNG", optimize=True)
    return buffer.getvalue()


def vacias_abajo(fotogramas):
    """Las filas transparentes bajo el dibujo en todos los fotogramas: lo que hay que bajarlo para que pise el suelo."""
    def vacias(rejilla):
        n = 0
        while n < len(rejilla) and rejilla[-1 - n].strip(TRANSPARENTE) == "":
            n += 1
        return n

    return min(vacias(f) for f in fotogramas)


def convertir_sprites(nombre, ruta, origen, salida, indice):
    datos, base, animaciones, variantes = leer_sprites(ruta, origen)
    ancho, alto = datos["tamaño"]
    entrada = {
        "hoja": f"{nombre}.png",
        "tamaño": [ancho, alto],
        "ancla": datos.get("ancla", [0, 0]),
        "columnas": COLUMNAS,
        "animaciones": {},
        "variantes": [],
        "razas": datos.get("razas", []),
    }
    # Lo que algunas hojas de la interfaz añaden: los caracteres de las cifras y su avance, la yema del dedo.
    for clave in ("caracteres", "avance", "yema"):
        if clave in datos:
            entrada[clave] = datos[clave]
    n = 0
    for anim, (fotogramas, ms) in animaciones.items():
        entrada["animaciones"][anim] = {
            "fotogramas": list(range(n, n + len(fotogramas))),
            "ms": ms,
            "vacias-abajo": vacias_abajo(fotogramas),
        }
        # Los tiles que avanza un ciclo entero de una carrera, si la animación lo dice (corre, corre-rapido).
        ciclo = datos["animaciones"][anim].get("ciclo-tiles")
        if ciclo is not None:
            entrada["animaciones"][anim]["ciclo-tiles"] = ciclo
        n += len(fotogramas)
    salida[entrada["hoja"]] = png(hoja(animaciones, ancho, alto, base))
    for familia, variante, paleta in variantes:
        fichero = f"{nombre}@{familia + '-' if familia else ''}{variante}.png"
        salida[fichero] = png(hoja(animaciones, ancho, alto, paleta))
        entrada["variantes"].append({"familia": familia, "nombre": variante, "hoja": fichero})
    indice["conjuntos"][nombre] = entrada


def convertir_fondo(ruta, paleta, donde):
    """Un PNG indexado (o en color) con los colores de su paleta, como RGBA con su transparencia."""
    imagen = Image.open(ruta)
    rgba = imagen.convert("RGBA")
    permitidos = set(paleta)
    for (r, g, b, a) in set(rgba.getdata()):
        if a and (r, g, b) not in permitidos:
            raise ArteMal(f"{donde}: el color {r},{g},{b} no está en su paleta")
    return rgba


def generar(origen):
    """Todo lo que el juego carga, como nombre de fichero → bytes."""
    if not origen.is_dir():
        raise ArteMal(f"no existe {origen}")
    salida = {}
    indice = {"version": 1, "conjuntos": {}, "escenarios": {}}
    for grupo, fichero in (("caro", "caro"), ("recolectables", "recolectables"), ("efectos", "efectos")):
        ruta = origen / grupo / f"{fichero}.json"
        if ruta.exists():
            convertir_sprites(grupo, ruta, origen, salida, indice)
    for escenario in ESCENARIOS:
        carpeta = origen / escenario
        if not (carpeta / "tiles.json").exists():
            continue
        convertir_sprites(f"{escenario}-tiles", carpeta / "tiles.json", origen, salida, indice)
        datos_fondos = json.loads((carpeta / "fondos.json").read_text(encoding="utf-8")) if (carpeta / "fondos.json").exists() else {}
        paleta = buscar_paleta(origen, datos_fondos.get("paleta", escenario))
        bajo = datos_fondos.get("filas-bajo-el-suelo", {})
        variantes = datos_fondos.get("variantes", {})
        entrada = {"tiles": f"{escenario}-tiles"}
        for fondo, clave in FONDOS:
            ruta = carpeta / f"{fondo}.png"
            if ruta.exists():
                imagen = convertir_fondo(ruta, paleta, f"{escenario}/{fondo}.png")
                salida[f"{escenario}-{fondo}.png"] = png(imagen)
                entrada[clave] = {"hoja": f"{escenario}-{fondo}.png", "bajo-el-suelo": bajo.get(clave, 0)}
                # Las variantes de una capa (la cabaña de la nieve): la misma tira con algo más, que el juego intercala.
                for nombre, fichero in variantes.get(clave, {}).items():
                    donde = f"{escenario}/{fichero}"
                    if not (carpeta / fichero).exists():
                        raise ArteMal(f"{donde}: no existe")
                    variante = convertir_fondo(carpeta / fichero, paleta, donde)
                    if variante.size != imagen.size:
                        raise ArteMal(f"{donde}: mide {variante.size} y su capa {imagen.size}")
                    hoja_variante = f"{escenario}-{fondo}-{nombre}.png"
                    salida[hoja_variante] = png(variante)
                    entrada[clave].setdefault("variantes", {})[nombre] = hoja_variante
        indice["escenarios"][escenario] = entrada
    if (origen / "ui" / "ui.json").exists():
        convertir_ui(origen, salida, indice)
    salida["indice.json"] = (json.dumps(indice, ensure_ascii=False, indent=1) + "\n").encode("utf-8")
    return salida


def huella(ficheros):
    return {nombre: hashlib.sha256(datos).hexdigest() for nombre, datos in ficheros.items()}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--desde", type=Path, default=ORIGEN)
    parser.add_argument("--hacia", type=Path, default=DESTINO)
    parser.add_argument("--comprobar", action="store_true")
    args = parser.parse_args()
    if Image is None:
        print("arte: falta Pillow (pip install pillow); se usa lo ya generado")
        return 0
    if not args.desde.exists():
        print(f"arte: no hay {args.desde}; el juego usa el prototipo en gris")
        return 0
    try:
        ficheros = generar(args.desde)
    except ArteMal as e:
        print(f"arte: {e}")
        return 1
    destino = args.hacia
    actuales = {p.name: p.read_bytes() for p in destino.glob("*")} if destino.exists() else {}
    if args.comprobar:
        if huella(ficheros) != huella(actuales):
            print("arte: lo generado no está al día con arte/; ejecuta python3 tools/arte.py")
            return 1
        print(f"arte: al día ({len(ficheros)} ficheros)")
        return 0
    destino.mkdir(parents=True, exist_ok=True)
    for viejo in set(actuales) - set(ficheros):
        (destino / viejo).unlink()
    for nombre, datos in ficheros.items():
        if actuales.get(nombre) != datos:
            (destino / nombre).write_bytes(datos)
    print(f"arte: {len(ficheros)} ficheros en {destino}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
