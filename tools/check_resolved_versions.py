#!/usr/bin/env python3
"""Comprueba que la versión declarada de una dependencia sea la que de verdad se resuelve.

    ./gradlew :composeApp:dependencies > /tmp/deps.txt
    python3 tools/check_resolved_versions.py /tmp/deps.txt

## Qué problema resuelve

**Declarar una versión no la impone.** Si otro punto del grafo pide una superior, Gradle resuelve la
mayor para todo el classpath y lo escrito en `libs.versions.toml` pasa a ser una sugerencia. Eso no
es un defecto de Gradle —es su modelo de conflictos— pero sí es información que en este proyecto no
tenía quien la mirara.

Costó una tanda entera de CI: se fijó `kotlinx-datetime` en 0.6.2, donde `kotlinx.datetime.Instant`
es una clase de verdad, y se resolvió una 0.7+ donde ese nombre sobrevive **solo como typealias**. El
compilador lo aceptó —para él un alias es válido— y la JVM no lo encontró, porque un typealias no
existe en el bytecode. Lo peor de los dos mundos: compilaba y reventaba al ejecutar.

## Qué falla y qué solo se informa

- **Falla** cuando lo sustituido era **nuestra propia declaración**: alguien escribió una versión en
  el catálogo y el grafo la ignoró. Ese es exactamente el caso de arriba, y es el que hay que ver.
- **Solo informa** de los ascensos entre dependencias de terceros. Que dos librerías ajenas pidan
  versiones distintas de una tercera y Gradle escoja es funcionamiento normal, y convertirlo en
  error dejaría un build que falla por decisiones que no son nuestras.

Se apoya en el marcador `->` del informe de dependencias de Gradle, que es su forma de decir "pedías
esto y te doy esto otro".
"""

from __future__ import annotations

import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(REPO, "gradle", "libs.versions.toml")

# `org.grupo:artefacto:pedida -> resuelta`, con el ruido del árbol delante y a veces `(*)` detrás.
UPGRADE = re.compile(r"([\w.\-]+):([\w.\-]+):([\w.\-+]+)\s+->\s+([\w.\-+]+)")


def declared_versions() -> dict[str, str]:
    """Los módulos del catálogo con la versión que se les escribió, resolviendo `version.ref`.

    Las entradas sin versión —las que dependen de un BOM, como `koin-core`— se quedan fuera a
    propósito: ahí **no hay declaración que contradecir**, que es justo el sentido de usar un BOM.
    """
    text = open(CATALOG, encoding="utf-8").read()

    versions: dict[str, str] = {}
    versions_block = re.search(r"\[versions\](.*?)(?=^\[)", text, re.S | re.M)
    if versions_block:
        for name, value in re.findall(r'^\s*([\w\-]+)\s*=\s*"([^"]+)"', versions_block.group(1), re.M):
            versions[name] = value

    libraries_block = re.search(r"\[libraries\](.*?)(?=^\[|\Z)", text, re.S | re.M)
    if not libraries_block:
        return {}

    declared: dict[str, str] = {}
    for line in libraries_block.group(1).split("\n"):
        module = re.search(r'module\s*=\s*"([^"]+)"', line)
        if not module:
            continue
        literal = re.search(r'version\s*=\s*"([^"]+)"', line)
        reference = re.search(r'version\.ref\s*=\s*"([^"]+)"', line)
        if literal:
            declared[module.group(1)] = literal.group(1)
        elif reference and reference.group(1) in versions:
            declared[module.group(1)] = versions[reference.group(1)]

    return declared


def upgrades(report: str) -> set[tuple[str, str, str]]:
    """`(módulo, pedida, resuelta)` de cada ascenso del informe, sin repetir.

    El mismo ascenso aparece una vez por cada rama del árbol que llega a él, y en un proyecto
    multiplataforma eso son decenas de líneas idénticas.
    """
    found = set()
    for group, name, requested, selected in UPGRADE.findall(report):
        if requested != selected:
            found.add((f"{group}:{name}", requested, selected))
    return found


def main(paths: list[str]) -> int:
    if not paths:
        print("uso: check_resolved_versions.py <informe-de-dependencias>...", file=sys.stderr)
        return 2

    report = "\n".join(open(path, encoding="utf-8").read() for path in paths)

    # Fallo cerrado: si el informe no tiene ni una línea de dependencia, es que el comando de Gradle
    # no dio lo que se esperaba, y un chequeo que aprueba porque no leyó nada es peor que ninguno.
    if not re.search(r"[\w.\-]+:[\w.\-]+:[\w.\-+]+", report):
        print("el informe no contiene ninguna dependencia: ¿falló el comando de Gradle?", file=sys.stderr)
        return 2

    declared = declared_versions()
    ours, theirs = [], []

    for module, requested, selected in sorted(upgrades(report)):
        line = f"{module}: se pidió {requested} y se resolvió {selected}"
        if declared.get(module) == requested:
            ours.append(line)
        else:
            theirs.append(line)

    print(f"{len(declared)} módulos con versión declarada en el catálogo")

    if theirs:
        print(f"\n{len(theirs)} ascensos entre dependencias de terceros (informativo):\n")
        for line in theirs:
            print(f"  · {line}")

    if ours:
        print(f"\n{len(ours)} versiones del catálogo que el grafo NO respeta:\n")
        for line in ours:
            print(f"  - {line}")
        print(
            "\nDeclarar una versión no la impone. Escribí en el catálogo la que de verdad se"
            "\nresuelve, o usá `strictly` si esa versión importa y quieres que el build falle"
            "\nen vez de ascender en silencio.",
        )
        return 1

    print("\nninguna versión declarada queda sustituida")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
