#!/usr/bin/env python3
"""La garantía de privacidad sobre el manifiesto **fusionado**, no sobre el que escribimos.

    python3 tools/merged_manifest.py            # busca por los patrones conocidos de AGP
    python3 tools/merged_manifest.py 'ruta/**/AndroidManifest.xml'   # o con los que se le pasen

## Por qué hace falta esto además de `checks.py`

`check_privacy_guarantee()` mira `androidApp/src/main/AndroidManifest.xml`, que es lo único que
existe antes de Gradle. Eso deja fuera el caso más peligroso y el que nadie ve venir: **una
dependencia que declare `INTERNET` en su propio manifiesto**. El fusionador de AGP lo mete en el APK
sin preguntar, el manifiesto fuente sigue estando limpio, y la promesa que la app le hace al usuario
en Ajustes —"no pide permiso de internet, así que lo que escaneás no puede salir del dispositivo"—
pasa a ser falsa sin que cambie ni una línea de este repositorio.

Es exactamente el error de forma que ya costó caro dos veces aquí: **auditar lo que hace el código
propio y no lo que el sistema hace con él**. Con `allowBackup` fue el proceso de copia de seguridad;
con D18, un tipo mal registrado que el CI no podía ver. Aquí es el fusionador de manifiestos.

Lo destapó la auditoría del 30-08-2026, que encontró que el escenario "una dependencia introduce el
permiso al fusionar" estaba **escrito como cubierto en la especificación** y no lo comprobaba nadie.

## Qué falla y qué solo se informa

Falla —y tumba el PR— por lo que rompe la garantía: `INTERNET`, `allowBackup` distinto de `false` y
`dataExtractionRules` ausente.

**El resto de permisos se imprimen y no fallan.** La app necesita `CAMERA`, y las dependencias de
Google traen los suyos; convertir cualquier permiso nuevo en un error rompería el build por motivos
legítimos y acabaría con alguien desactivando la comprobación. Lo que sí hace es **ponerlos todos en
el resumen del run**, que es donde una revisión los ve sin excavar en dos mil líneas de log.

## Por qué se niega a pasar si no encuentra el archivo

Una comprobación que no encuentra su objetivo y sale con cero es peor que no tenerla: da por
revisado lo que nadie revisó, y en este proyecto ya pasó con detekt, que analizaba **cero archivos**
y salía en verde. La ruta del manifiesto fusionado la elige AGP y cambia entre versiones, así que
aquí se busca por patrón y **no encontrar nada es un fallo**.

Eso pasó en la primera ejecución real: el patrón único apuntaba a `merged_manifests/` y AGP dejaba
el archivo en otro sitio, así que el paso murió en un segundo. Se corrigió de dos formas a la vez
—varios patrones, y **listar lo que sí hay** cuando ninguno acierta—, porque adivinar la ruta otra
vez habría costado otra ronda de CI para volver a no saber nada.
"""

from __future__ import annotations

import glob
import os
import sys
import xml.etree.ElementTree as ET

# Antes del import y no en `__main__`: `checks` vive en esta misma carpeta, y quien lance el script
# desde la raíz del repositorio no la tiene en el path.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import checks  # noqa: E402  — necesita el `sys.path` de arriba

ANDROID = "{http://schemas.android.com/apk/res/android}"

INTERMEDIATES = "androidApp/build/intermediates"

# AGP elige dónde deja el manifiesto de un módulo de aplicación y lo ha movido entre versiones:
# `merged_manifests/` y `packaged_manifests/`, con o sin el nombre de la tarea como carpeta
# intermedia. Se prueban por orden y basta con que uno acierte. El tercero es el comodín que
# sobrevive al próximo cambio de nombre sin dejar de mirar **solo** al módulo de aplicación: los
# manifiestos de las librerías no llevan `allowBackup` ni `dataExtractionRules`, y darían un fallo
# falso en cuanto entraran en la lista.
DEFAULT_PATTERNS = (
    f"{INTERMEDIATES}/merged_manifests/**/AndroidManifest.xml",
    f"{INTERMEDIATES}/packaged_manifests/**/AndroidManifest.xml",
    f"{INTERMEDIATES}/*manifest*/**/AndroidManifest.xml",
)


def permissions(path: str) -> list[str]:
    root = ET.parse(path).getroot()
    return sorted(
        name for name in
        (element.get(f"{ANDROID}name") for element in root.iter("uses-permission"))
        if name
    )


def not_found(patterns: list[str]) -> int:
    """El fallo por no encontrar nada, con lo que hace falta para arreglarlo en un solo intento."""
    print("no se encontró ningún manifiesto fusionado. Patrones probados:")
    for pattern in patterns:
        print(f"  - {pattern}")
    print()
    print("Esto es un fallo y no un aviso: si AGP cambió la ruta, esta comprobación dejaría de")
    print("mirar nada y saldría en verde, que es la peor forma de tener un control de calidad.")
    print("Hay que corregir el patrón, no quitar el paso.")
    print()

    # Lo que sí hay, para que el arreglo salga de un dato y no de otra suposición. Se enseña y
    # **no** se usa: un manifiesto encontrado a ciegas puede ser una fase intermedia sin
    # `allowBackup`, y fallar diciendo que la garantía de privacidad se rompió cuando lo que pasa
    # es que se miró el archivo equivocado sería peor que este fallo. Aquí se informa; el patrón lo
    # arregla una persona.
    candidates = sorted(glob.glob("androidApp/build/**/AndroidManifest.xml", recursive=True))
    if candidates:
        print("manifiestos que sí existen bajo androidApp/build (candidatos para el patrón):")
        for path in candidates:
            print(f"  - {path}")
        return 1

    existing = sorted(glob.glob(f"{INTERMEDIATES}/*"))
    if existing:
        print(f"carpetas que sí existen en {INTERMEDIATES}:")
        for path in existing:
            print(f"  - {os.path.basename(path)}")
    else:
        print(f"{INTERMEDIATES} no existe: ¿se ejecutó `assembleDebug` antes de este paso?")
    return 1


def main(argv: list[str]) -> int:
    patterns = argv[1:] or list(DEFAULT_PATTERNS)

    found: list[str] = []
    for pattern in patterns:
        found = sorted(glob.glob(pattern, recursive=True))
        if found:
            break

    if not found:
        return not_found(patterns)

    for manifest in found:
        print(f"manifiesto fusionado: {os.path.relpath(manifest)}")
        declared = permissions(manifest)
        if declared:
            print("  permisos que llegan al APK:")
            for name in declared:
                print(f"    - {name}")
        else:
            print("  sin permisos declarados")

        checks.check_privacy_guarantee(manifest)

    print()
    if checks.problems:
        print(f"{len(checks.problems)} hallazgos sobre el manifiesto fusionado:\n")
        for problem in checks.problems:
            print(f"  - {problem}")
        print()
        print("La garantía de privacidad está escrita en el README, en Ajustes y en docs/legal/.")
        print("Si esto falla, o se quita la dependencia que lo introduce o deja de ser cierta.")
        return 1

    print("la garantía se mantiene en el manifiesto fusionado")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
