#!/usr/bin/env python3
"""Lee los informes XML de Kover y dice cuánta cobertura hay, por módulo.

    ./gradlew :core:domain:koverXmlReport :core:data:koverXmlReport
    python3 tools/coverage.py core/domain core/data --min 80

## Qué problema resuelve

El SDD §13.1 prometía "≥ 80 % en `:core:domain` y `:core:data`" y **no había nada que lo midiera**.
Un objetivo sin medición no disciplina: no dice cuándo se incumple, así que en la práctica se
incumple sin que nadie se entere. O se instrumenta o se borra la frase; esto es instrumentarla.

## Por qué un script y no `koverVerify`

Kover trae su propia verificación, y para fallar por debajo del umbral habría bastado. Lo que no
trae es **decir cuánto falta y dónde**: su mensaje de error da el porcentaje total y se acaba ahí.
Cuando la cobertura baja, la pregunta útil no es "¿cuánto?" sino "¿qué dejó de estar cubierto?", y
para responderla desde el log de CI hay que ver los paquetes peores. Eso es lo que añade esto.

Sin `--min` no falla nunca: solo informa. Es el modo con el que se midió por primera vez, cuando
todavía no había un número que defender.

El XML de Kover sigue el formato de JaCoCo: contadores `<counter type="LINE" missed="" covered=""/>`
anidados en `<package>` y repetidos a nivel de `<report>` con el total.
"""

from __future__ import annotations

import os
import sys
import xml.etree.ElementTree as ElementTree

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Cuántos paquetes flojos se listan. Suficiente para saber por dónde empezar, no tantos como para
# que el log de CI se vuelva ilegible.
WORST_PACKAGES = 8

# Un paquete con cuatro líneas siempre sale mal o siempre sale bien, y en ninguno de los dos casos
# dice nada. Por debajo de esto no se lista.
MIN_LINES_TO_REPORT = 20


def report_path(module: str) -> str:
    """Donde Kover deja el XML: `<módulo>/build/reports/kover/report.xml`."""
    return os.path.join(REPO, module, "build", "reports", "kover", "report.xml")


def line_counter(element: ElementTree.Element) -> tuple[int, int]:
    """Devuelve `(cubiertas, totales)` del contador de líneas **propio** de este elemento.

    Solo mira los hijos directos: los `<counter>` de un `<package>` ya agregan sus clases, y los del
    `<report>` agregan sus paquetes. Buscar en profundidad contaría lo mismo varias veces.
    """
    for counter in element.findall("counter"):
        if counter.get("type") == "LINE":
            covered = int(counter.get("covered", "0"))
            missed = int(counter.get("missed", "0"))
            return covered, covered + missed
    return 0, 0


def percentage(covered: int, total: int) -> float:
    """Un módulo sin líneas está cubierto al 100 %: no hay nada que dejar sin cubrir."""
    return 100.0 if total == 0 else 100.0 * covered / total


def read_module(module: str) -> tuple[int, int, list[tuple[str, int, int]]]:
    path = report_path(module)
    if not os.path.exists(path):
        print(
            f"no existe {os.path.relpath(path, REPO)}: ¿corrió `:{module.replace('/', ':')}:koverXmlReport`?")
        raise SystemExit(2)

    root = ElementTree.parse(path).getroot()
    covered, total = line_counter(root)

    packages = []
    for package in root.findall("package"):
        package_covered, package_total = line_counter(package)
        if package_total >= MIN_LINES_TO_REPORT:
            packages.append(
                (package.get("name", "?").replace("/", "."), package_covered, package_total))

    return covered, total, packages


def main(argv: list[str]) -> int:
    minimum: float | None = None
    modules = []

    arguments = list(argv)
    while arguments:
        argument = arguments.pop(0)
        if argument == "--min":
            if not arguments:
                print("--min necesita un número")
                return 2
            minimum = float(arguments.pop(0))
        else:
            modules.append(argument)

    if not modules:
        print(__doc__)
        return 2

    worst: list[tuple[str, int, int]] = []
    failures = []

    print("Cobertura de líneas\n")
    for module in modules:
        covered, total, packages = read_module(module)
        share = percentage(covered, total)
        verdict = ""
        if minimum is not None:
            verdict = "  ✓" if share >= minimum else f"  ✗ por debajo de {minimum:g} %"
            if share < minimum:
                failures.append((module, share))
        print(f"  {module:<20} {share:6.1f} %   ({covered}/{total} líneas){verdict}")
        worst.extend(packages)

    worst.sort(key=lambda entry: percentage(entry[1], entry[2]))
    if worst:
        print("\nLos paquetes con menos cobertura, que es por donde se sube:\n")
        for name, covered, total in worst[:WORST_PACKAGES]:
            print(f"  {percentage(covered, total):6.1f} %   {name}  ({covered}/{total})")

    if failures:
        print(
            "\nEl objetivo del SDD §13.1 no se cumple. Subir cobertura escribiendo tests que"
            "\nrecorren líneas sin comprobar nada empeora el proyecto y mejora el número: si el"
            "\numbral ya no describe lo que este código necesita, cámbialo aquí y en el SDD, con"
            "\nel motivo escrito. Lo que no vale es dejar la frase y no mirarla.",
        )
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
