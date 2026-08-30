#!/usr/bin/env python3
"""La garantía de privacidad sobre el manifiesto **fusionado**, no sobre el que escribimos.

    python3 tools/merged_manifest.py 'androidApp/build/intermediates/merged_manifests/**/AndroidManifest.xml'

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

DEFAULT_PATTERN = "androidApp/build/intermediates/merged_manifests/**/AndroidManifest.xml"


def permissions(path: str) -> list[str]:
    root = ET.parse(path).getroot()
    return sorted(
        name for name in
        (element.get(f"{ANDROID}name") for element in root.iter("uses-permission"))
        if name
    )


def main(argv: list[str]) -> int:
    pattern = argv[1] if len(argv) > 1 else DEFAULT_PATTERN
    found = sorted(glob.glob(pattern, recursive=True))

    if not found:
        print(f"no se encontró ningún manifiesto fusionado con el patrón: {pattern}")
        print()
        print("Esto es un fallo y no un aviso: si AGP cambió la ruta, esta comprobación dejaría de")
        print("mirar nada y saldría en verde, que es la peor forma de tener un control de calidad.")
        print("Hay que corregir el patrón, no quitar el paso.")
        return 1

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
