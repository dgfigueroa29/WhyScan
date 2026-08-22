#!/usr/bin/env python3
"""Comprobaciones que no necesitan compilador.

    python3 tools/checks.py

Corre en segundos y sin red, que es justo lo que las hace útiles: el entorno de desarrollo de este
proyecto no alcanza `dl.google.com` ni `api.foojay.io`, así que **aquí no compila nada** y durante
mucho tiempo la única forma de saber si un cambio estaba bien fue subirlo y esperar a CI. Cada vuelta
de esas cuesta entre cinco y quince minutos.

## Por qué está en el repositorio y no en la carpeta temporal de alguien

Esto empezó como un puñado de scripts sueltos, y cada uno se escribió **después** de que CI rechazara
ese caso concreto: primero el orden de imports, luego los recursos sin declarar, luego los números
mágicos. La red crecía a golpes y vivía fuera del control de versiones, así que se perdía entre
sesiones y había que reescribirla. Eso era la deuda D23.

Vivir aquí arregla las dos mitades del problema: cualquiera puede ejecutarlas antes de subir, y
**CI las ejecuta también**, que es lo que impide que se queden obsoletas en silencio. Si una
comprobación se desincroniza de lo que detekt exige de verdad, el propio CI lo dice.

## Qué comprueba, y qué no

Lo que hay aquí se divide en dos grupos:

- **Lo que detekt ya comprueba** —longitud de línea y orden de imports— reproducido para poder verlo
  sin arrancar Gradle. Que estén duplicadas es el punto: aquí sirven de aviso rápido, y detekt sigue
  siendo la autoridad.
- **Lo que no comprueba nadie más**: la paridad de los catálogos de recursos entre inglés y español,
  que cada `Res.string.X` tenga su `import`, que no queden claves huérfanas, y que el `package` de
  cada `.kt` coincida con su ruta en disco. Un catálogo desparejado no rompe la compilación: rompe
  la pantalla de quien tenga el idioma que falta.

Deliberadamente **no** intenta reimplementar detekt entero. Las reglas que dependen del árbol
sintáctico —complejidad, `MagicNumber`, funciones sin usar— necesitan un analizador de verdad, y una
aproximación con expresiones regulares acabaría fallando donde detekt aprueba, que es la peor forma
de tener una comprobación.
"""

from __future__ import annotations

import os
import re
import sys
import xml.etree.ElementTree as ET

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# detekt: `MaxLineLength`. El mismo número que en `detekt.yml`, y si allí cambia hay que cambiarlo
# aquí — es la clase de duplicación que el propio CI destapa la primera vez que divergen.
MAX_LINE = 120

# Directorios que no son código nuestro.
SKIP_DIRS = {".git", "build", ".gradle", ".idea", ".kotlin", "node_modules"}

problems: list[str] = []


def report(path: str, message: str) -> None:
    problems.append(f"{os.path.relpath(path, REPO)}: {message}")


def walk(root: str, suffix: str):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            if name.endswith(suffix):
                yield os.path.join(dirpath, name)


def import_sort_key(path: str) -> tuple[int, str]:
    """El orden que exige ktlint, que no es simplemente alfabético.

    La distribución por defecto pone primero todo lo demás en orden lexicográfico, después
    `java.**`, `javax.**` y `kotlin.**` en ese orden, y al final los imports con alias. Es la razón
    de que `kotlin.time.Instant` vaya *después* de `org.jetbrains...`, que a ojo parece un error.
    """
    if " as " in path:
        return (4, path)
    head = path.split(".")[0]
    return ({"java": 1, "javax": 2, "kotlin": 3}.get(head, 0), path)


def check_kotlin_sources() -> None:
    for f in walk(REPO, ".kt"):
        check_kotlin_file(f)
    for f in walk(REPO, ".kts"):
        check_lines(f)


def check_lines(path: str) -> None:
    for number, line in enumerate(open(path, encoding="utf-8").read().split("\n"), 1):
        if len(line) > MAX_LINE:
            report(path, f"línea {number} de {len(line)} caracteres (máximo {MAX_LINE})")


def check_kotlin_file(path: str) -> None:
    text = open(path, encoding="utf-8").read()
    lines = text.split("\n")

    check_lines(path)

    imports = [line[len("import "):].strip() for line in lines if line.startswith("import ")]
    ordered = sorted(imports, key=import_sort_key)
    if imports != ordered:
        for got, want in zip(imports, ordered):
            if got != want:
                report(path, f"orden de imports: se esperaba '{want}' donde está '{got}'")
                break

    # El `package` tiene que coincidir con la ruta bajo `kotlin/`. El compilador no lo exige, pero
    # un paquete que no sigue a su carpeta convierte cualquier búsqueda por ruta en una trampa — y
    # es exactamente lo que deja atrás un renombrado hecho a medias.
    declared = re.search(r"^package\s+([\w.]+)", text, re.M)
    if declared and "/kotlin/" in path:
        on_disk = os.path.dirname(path.split("/kotlin/", 1)[1]).replace("/", ".")
        if declared.group(1) != on_disk:
            report(path, f"package {declared.group(1)} no coincide con la ruta {on_disk}")


def check_xml_is_well_formed() -> None:
    for f in walk(REPO, ".xml"):
        try:
            ET.parse(f)
        except ET.ParseError as error:
            report(f, f"XML mal formado: {error}")


def check_compose_resources() -> int:
    """Paridad entre catálogos, imports por clave y claves huérfanas.

    Los recursos de Compose son **por módulo** y cada clave se importa una a una, así que hay tres
    formas distintas de equivocarse y ninguna rompe la compilación de todas las plataformas:

    - Añadir la cadena en un catálogo y no en el otro. El usuario del idioma que falta ve la clave.
    - Usarla sin importarla. Esto sí lo caza el compilador, pero solo al llegar a ese módulo.
    - Dejar una clave que ya no usa nadie, que es como se acumula texto muerto que alguien traduce.
    """
    catalogs = sorted(
        dirpath
        for dirpath, dirnames, _ in os.walk(REPO)
        if not (set(dirpath.split(os.sep)) & SKIP_DIRS) and dirpath.endswith("composeResources")
    )

    total_keys = 0
    for catalog in catalogs:
        english = os.path.join(catalog, "values", "strings.xml")
        spanish = os.path.join(catalog, "values-es", "strings.xml")
        if not (os.path.exists(english) and os.path.exists(spanish)):
            report(catalog, "falta values/ o values-es/")
            continue

        declared_en = {e.get("name") for e in ET.parse(english).getroot()}
        declared_es = {e.get("name") for e in ET.parse(spanish).getroot()}
        total_keys += len(declared_en)

        for key in sorted(declared_en - declared_es):
            report(spanish, f"falta la clave '{key}', que sí está en inglés")
        for key in sorted(declared_es - declared_en):
            # El catálogo sin calificador es el respaldo de **cualquier** idioma, así que una clave
            # que solo existe en español revienta para todo el mundo que no hable español.
            report(english, f"falta la clave '{key}', que sí está en español")

        module = os.path.dirname(os.path.dirname(catalog))
        used, imported = set(), set()
        for source in walk(module, ".kt"):
            text = open(source, encoding="utf-8").read()
            used |= set(re.findall(r"Res\.string\.(\w+)", text))
            imported |= set(re.findall(r"^import [\w.]+\.resources\.(\w+)$", text, re.M))

        for key in sorted(used - imported):
            report(module, f"se usa Res.string.{key} sin importarla")
        for key in sorted(used - declared_en):
            report(module, f"Res.string.{key} no está declarada en ningún catálogo")
        for key in sorted(declared_en - used):
            report(module, f"clave huérfana '{key}': está declarada y no la usa nadie")

    return total_keys


def main() -> int:
    check_kotlin_sources()
    check_xml_is_well_formed()
    keys = check_compose_resources()

    print(f"{keys} claves de recursos con paridad inglés/español")

    if problems:
        print(f"\n{len(problems)} hallazgos:\n")
        for problem in problems:
            print(f"  - {problem}")
        return 1

    print("sin hallazgos")
    return 0


if __name__ == "__main__":
    sys.exit(main())
