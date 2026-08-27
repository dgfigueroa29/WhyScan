#!/usr/bin/env python3
"""Mide el tamaño del binario de Android y avisa cuando crece.

    python3 tools/binary_size.py --name android-release 'androidApp/build/outputs/apk/release/*.apk'
    python3 tools/binary_size.py --name android-release --baseline tools/binary-size.json <ruta>
    python3 tools/binary_size.py --name android-release --baseline tools/binary-size.json --record <ruta>

## Qué problema resuelve

RNF-06 dice que "el APK no debe crecer por motores que el usuario no usa", y §7.6 del SDD reconoce
que **dentro de Android no se cumple**: el binario enlaza los cuatro motores de la plataforma aunque
se use uno solo. ADR-0009 aplaza Play Feature Delivery con tres razones, y la tercera es la que este
script ataca: *"no hay ninguna medición del APK con la que decidir qué conviene partir"*.

Sin una medición, RNF-06 es un deseo con formato de requisito. No dice cuándo se incumple, así que
se incumple sin que nadie se entere — exactamente lo que le pasaba al objetivo de cobertura antes de
la Ronda 5.

## Qué mide, y qué **no** mide

Un APK es un zip. Se lee su directorio central y se reparte cada entrada en cubos que significan
algo aquí: el código compilado, las librerías nativas **por ABI**, los assets, los recursos. Ese
reparto es lo que convierte un número en una respuesta: el total dice *cuánto*, y los cubos dicen
*de qué* — que es la pregunta de ADR-0009.

Lo que devuelve **no es** el tamaño de descarga de Play, y conviene no confundirlos nunca:

- Play distribuye **APKs partidos** desde el AAB: cada dispositivo se baja una ABI y una densidad,
  no las cuatro y las seis.
- Play re-firma y recomprime, así que ni siquiera el total coincide.

Lo que sí es: **una medida estable con la misma metodología en cada ejecución**. Eso basta para lo
único que se le pide — detectar que un cambio ha engordado el binario y decir por dónde.

## Por qué el umbral es un delta y no un nivel

Aquí se fija una tolerancia de crecimiento por defecto, y en cobertura se rechazó fijar un suelo sin
medir antes. No es incoherencia: **los dos fallan de formas distintas**.

- Un *suelo* de cobertura es un nivel absoluto. Inventarlo antes de medir tiene dos finales y los dos
  son malos: o rompe CI el primer día, o se elige tan bajo que no exige nada.
- Una *tolerancia* de crecimiento es relativa a la línea base, y la línea base se graba de la
  medición real. El primer día el delta es cero por construcción: no puede romper nada. A partir del
  segundo sí pregunta algo, y la pregunta —"¿este PR engorda el binario un 2 %?"— se puede contestar
  sin saber de antemano cuánto pesa la app.

## Sin línea base todavía

Mientras no exista el archivo de línea base, **mide e informa pero no puede fallar**. Es el mismo
modo que `coverage.py` tiene para un módulo sin suelo, y existe por lo mismo: cuando se empieza a
medir algo todavía no hay un número que defender.

Grabar ese número tiene aquí una vuelta de tuerca: **el entorno de desarrollo de este proyecto no
puede construir el APK** —no alcanza `dl.google.com`—, así que la primera medición solo la produce
CI. Por eso el script imprime el JSON listo para pegar y CI lo sube además como artefacto: grabar la
línea base es descargar un archivo y commitearlo, sin compilar nada en local.
"""

from __future__ import annotations

import glob
import json
import os
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Cuánto puede crecer el binario sin que nadie tenga que decir por qué.
#
# Un 2 % sobre una app de este tamaño son decenas de kilobytes: cabe un refactor, no cabe un SDK.
# Es el punto en el que el aviso todavía significa algo y no salta por el ruido de una build a otra.
DEFAULT_TOLERANCE = 2.0

# Cuántas entradas sueltas se listan. Suficiente para reconocer al culpable de un salto, no tantas
# como para que el resumen del run deje de leerse.
BIGGEST_ENTRIES = 10

# Los cubos, en el orden en que se enseñan. El primero que casa gana, así que van de lo más
# específico a lo más general.
#
# Las nativas se separan **por ABI** a propósito: es el único reparto del APK que se puede atribuir a
# un motor concreto —`libzxing*.so` es zxing-cpp y nada más—, y por tanto lo único que hoy contesta
# de verdad la pregunta de RNF-06.
BUCKETS: list[tuple[str, str]] = [
    ("lib/arm64-v8a/", "nativas · arm64-v8a"),
    ("lib/armeabi-v7a/", "nativas · armeabi-v7a"),
    ("lib/x86_64/", "nativas · x86_64"),
    ("lib/x86/", "nativas · x86"),
    ("lib/", "nativas · otras ABI"),
    ("assets/", "assets"),
    ("res/", "recursos"),
    ("META-INF/", "firma y metadatos"),
    ("kotlin/", "metadatos de Kotlin"),
]

OTHER_BUCKET = "otros"
DEX_BUCKET = "código (dex)"
ARSC_BUCKET = "tabla de recursos"


def bucket_of(name: str) -> str:
    """A qué cubo va una entrada del zip."""
    # El dex no vive bajo ningún directorio: `classes.dex`, `classes2.dex`… en la raíz.
    if name.startswith("classes") and name.endswith(".dex"):
        return DEX_BUCKET
    if name == "resources.arsc":
        return ARSC_BUCKET
    for prefix, bucket in BUCKETS:
        if name.startswith(prefix):
            return bucket
    return OTHER_BUCKET


def measure(path: str) -> tuple[int, dict[str, int], list[tuple[str, int]]]:
    """Devuelve `(tamaño del archivo, tamaño por cubo, entradas más grandes)`.

    Se mide el tamaño **comprimido** de cada entrada y no el original: lo que ocupa el APK es lo
    comprimido, y un assets de texto que se comprime a la décima parte contaría diez veces de más.
    Las nativas suelen ir sin comprimir —lo pide el cargador desde Android 6— y ahí las dos cifras
    coinciden, que es justo lo que se espera.
    """
    per_bucket: dict[str, int] = {}
    entries: list[tuple[str, int]] = []

    with zipfile.ZipFile(path) as archive:
        for info in archive.infolist():
            if info.is_dir():
                continue
            size = info.compress_size
            per_bucket[bucket_of(info.filename)] = per_bucket.get(bucket_of(info.filename), 0) + size
            entries.append((info.filename, size))

    entries.sort(key=lambda entry: entry[1], reverse=True)
    return os.path.getsize(path), per_bucket, entries


def human(size: int) -> str:
    """Bytes, kibibytes o mebibytes, que es como se habla de un APK."""
    if size >= 1024 * 1024:
        return f"{size / (1024 * 1024):.2f} MiB"
    # Por debajo del kibibyte se dan bytes: un cubo de veintiséis bytes escrito como "0.0 KiB"
    # parece vacío y no lo está, y eso es justo lo que no debe hacer una herramienta de medida.
    if size >= 1024:
        return f"{size / 1024:.1f} KiB"
    return f"{size} B"


def delta(current: int, previous: int) -> str:
    difference = current - previous
    if difference == 0:
        return "sin cambio"
    share = (difference / previous * 100) if previous else 0.0
    # El signo se pone aquí y `human` recibe el valor absoluto: pasarle un negativo lo mandaba a la
    # rama de los bytes —ningún negativo llega a un kibibyte— y un adelgazamiento de 900 KiB se
    # imprimía como "-921600 B". Una herramienta de medida que solo sabe leer los empeoramientos
    # sirve para la mitad de las veces que hace falta.
    return f"{'+' if difference > 0 else '-'}{human(abs(difference))} ({share:+.2f} %)"


def shown(path: str) -> str:
    """La ruta relativa al repositorio, salvo que quede fuera — ahí una ristra de `..` no ayuda."""
    relative = os.path.relpath(path, REPO)
    return path if relative.startswith("..") else relative


def resolve(patterns: list[str]) -> list[str]:
    """Expande los comodines aquí y no en el shell: así funciona igual desde CI y desde una consola."""
    found: list[str] = []
    for pattern in patterns:
        matches = sorted(glob.glob(pattern))
        if matches:
            found.extend(matches)
        elif os.path.exists(pattern):
            found.append(pattern)
    return found


def report(name: str, path: str, baseline: dict | None) -> tuple[dict, list[str]]:
    """Imprime la medición y devuelve `(lo medido, los motivos de fallo)`."""
    total, per_bucket, entries = measure(path)
    previous = (baseline or {}).get(name)
    problems: list[str] = []

    print(f"{name}  ·  {shown(path)}\n")
    if previous:
        print(f"  total   {human(total):>12}   {delta(total, previous['total'])}")
    else:
        print(f"  total   {human(total):>12}")

    print()
    for bucket, size in sorted(per_bucket.items(), key=lambda item: item[1], reverse=True):
        line = f"  {bucket:<24} {human(size):>12}"
        if previous:
            before = previous.get("buckets", {}).get(bucket)
            # Un cubo que antes no existía es una novedad, no un crecimiento del 0 %: decirlo así
            # evita que aparezca como "sin cambio" el día que alguien añade una ABI.
            line += f"   {delta(size, before) if before is not None else 'nuevo'}"
        print(line)

    # Un cubo que estaba y ya no está no aparecería en el bucle de arriba, y su desaparición se
    # perdería dentro del total. En un APK eso importa: que deje de empaquetarse una ABI no es
    # "pesa menos", es que la app dejó de instalarse en esos dispositivos.
    for bucket in sorted((previous or {}).get("buckets", {}).keys() - per_bucket.keys()):
        gone = previous["buckets"][bucket]
        print(f"  {bucket:<24} {'—':>12}   desapareció ({human(gone)})")
        problems.append(f"el cubo '{bucket}' desapareció del binario")

    print(f"\n  Las {BIGGEST_ENTRIES} entradas más grandes, que es por donde se adelgaza:\n")
    for entry, size in entries[:BIGGEST_ENTRIES]:
        print(f"    {human(size):>12}   {entry}")

    return {"total": total, "buckets": per_bucket}, problems


def main(argv: list[str]) -> int:
    name = None
    baseline_path = None
    record = False
    tolerance = DEFAULT_TOLERANCE
    patterns: list[str] = []

    arguments = list(argv)
    while arguments:
        argument = arguments.pop(0)
        if argument == "--name" and arguments:
            name = arguments.pop(0)
        elif argument == "--baseline" and arguments:
            baseline_path = arguments.pop(0)
        elif argument == "--tolerance" and arguments:
            tolerance = float(arguments.pop(0))
        elif argument == "--record":
            record = True
        elif argument.startswith("--"):
            print(f"opción desconocida: {argument}")
            return 2
        else:
            patterns.append(argument)

    if not name or not patterns:
        print(__doc__)
        return 2

    paths = resolve(patterns)
    if not paths:
        print(f"no hay ningún binario en {patterns}. ¿Se ensambló antes de medir?")
        return 2
    if len(paths) > 1:
        # Medir dos binarios bajo el mismo nombre daría una línea base que depende de cuál se
        # eligiera, así que es mejor parar y que alguien afine el patrón.
        print("el patrón casa con más de un binario y no se puede saber cuál es el bueno:")
        for path in paths:
            print(f"  {shown(path)}")
        return 2

    baseline: dict | None = None
    if baseline_path and os.path.exists(baseline_path):
        baseline = json.load(open(baseline_path, encoding="utf-8"))

    print("Tamaño del binario\n")
    measured, problems = report(name, paths[0], baseline)

    if record:
        updated = dict(baseline or {})
        updated[name] = measured
        with open(baseline_path or os.path.join(REPO, "tools", "binary-size.json"), "w", encoding="utf-8") as file:
            json.dump(updated, file, indent=2, ensure_ascii=False, sort_keys=True)
            file.write("\n")
        print(f"\nLínea base grabada en {baseline_path}")
        return 0

    if baseline is None or name not in baseline:
        print(
            "\nTodavía no hay línea base: esto **se mide y se informa, pero no puede fallar**."
            "\nPara grabarla, pega esto en el archivo de línea base y commitéalo — no hace falta"
            "\ncompilar nada en local, que aquí no se puede:\n",
        )
        print(json.dumps({name: measured}, indent=2, ensure_ascii=False, sort_keys=True))
        return 0

    if problems:
        print("\nAlgo desapareció del binario, y eso no se compensa con que pese menos:\n")
        for problem in problems:
            print(f"  · {problem}")
        print(
            "\nSi es deliberado —un split de ABI, un motor que se retira— vuelve a grabar la línea"
            "\nbase en el mismo PR. Si no lo es, la app acaba de dejar de funcionar en algún sitio.",
        )
        return 1

    growth = (measured["total"] - baseline[name]["total"]) / baseline[name]["total"] * 100
    if growth > tolerance:
        print(
            f"\nEl binario crece un {growth:.2f} %, por encima del {tolerance:g} % que se acepta sin"
            "\nexplicación. Eso no es necesariamente un error: puede que el PR traiga algo que pesa y"
            "\nvalga la pena. Lo que no vale es que engorde sin que nadie se entere — mira los cubos"
            "\nde arriba para saber por dónde, y si el crecimiento es deliberado, vuelve a grabar la"
            "\nlínea base en el mismo PR con el motivo en el mensaje del commit.",
        )
        return 1

    print(f"\nCrecimiento del {growth:+.2f} %, dentro del {tolerance:g} % aceptado.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
