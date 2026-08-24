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

# Operadores y convenciones que Kotlin resuelve por sintaxis, sin nombrarlos: `by` llama a
# `getValue`/`setValue`, `a[b] = c` llama a `set`, `a(b)` a `invoke`. Buscar su nombre en el cuerpo
# no encuentra nada y el import es imprescindible.
OPERATORS = {
    "getValue", "setValue", "provideDelegate",
    "get", "set", "invoke", "contains", "iterator", "compareTo", "equals",
    "plus", "minus", "times", "div", "rem", "rangeTo", "rangeUntil",
    "plusAssign", "minusAssign", "timesAssign", "divAssign", "remAssign",
    "inc", "dec", "unaryPlus", "unaryMinus", "not",
    "component1", "component2", "component3", "component4", "component5",
}

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

    check_unused_imports(path, text, imports)
    check_labels_resolve(path, text)

    # El `package` tiene que coincidir con la ruta bajo `kotlin/`. El compilador no lo exige, pero
    # un paquete que no sigue a su carpeta convierte cualquier búsqueda por ruta en una trampa — y
    # es exactamente lo que deja atrás un renombrado hecho a medias.
    declared = re.search(r"^package\s+([\w.]+)", text, re.M)
    if declared and "/kotlin/" in path:
        on_disk = os.path.dirname(path.split("/kotlin/", 1)[1]).replace("/", ".")
        if declared.group(1) != on_disk:
            report(path, f"package {declared.group(1)} no coincide con la ruta {on_disk}")


def strip_string_literals(text: str) -> str:
    """Vacía las cadenas conservando lo interpolado, que es código de verdad."""

    def interpolations(match: re.Match[str]) -> str:
        return " " + " ".join(re.findall(r"\$\{([^}]*)\}", match.group(0))) + " "

    text = re.sub(r'"""(?:.|\n)*?"""', interpolations, text)
    return re.sub(r'"(?:\\.|[^"\\\n])*"', interpolations, text)


def check_unused_imports(path: str, text: str, imports: list[str]) -> None:
    """Imports que no usa nadie. Es `NoUnusedImports` de ktlint, adelantado unos minutos.

    Buscar el nombre simple como palabra cubre más de lo que parece: una función de extensión
    importada **sí** aparece en la llamada (`.catch { }`), y una anotación de opt-in también
    (`@OptIn(ExperimentalTime::class)`).

    Lo que no cubre son los **operadores**, y esa lección salió cara en el sitio adecuado: la primera
    versión de esta comprobación dio quince hallazgos y los quince eran falsos positivos. `getValue`
    y `setValue` los usa `by` sin nombrarlos, y `set` lo usa `settings[clave] = valor`. Kotlin los
    resuelve **por sintaxis**, así que su nombre no aparece en ninguna parte del cuerpo.

    Por eso está [OPERATORS], y por eso el resto de reglas que dependen del árbol sintáctico siguen
    fuera de este archivo: una comprobación que falla donde detekt aprueba es peor que no tenerla.
    """
    # Los comentarios **no** cuentan como uso: nombrar un tipo en un KDoc no lo utiliza, y esa fue
    # la segunda lección de esta comprobación — la primera versión daba por usado un import que solo
    # aparecía entre comillas invertidas en la documentación del propio archivo.
    #
    # Lo que sí cuenta es un enlace `[Algo]` de KDoc, porque ese enlace se rompe si el import se va.
    linked = set(re.findall(r"\[(\w+)", text))
    body = "\n".join(line for line in text.split("\n") if not line.startswith("import "))

    # Las cadenas se vacían **antes** que los comentarios, y de las cadenas se conserva lo que hay
    # dentro de `${...}`. Las dos cosas son arreglos de falsos positivos reales:
    #
    #  - Quitar `//…` primero se come el resto de la línea en cualquier `"https://ejemplo.com"`, y
    #    con él los usos que vinieran después. Este proyecto está lleno de URLs de prueba.
    #  - Una plantilla de cadena **contiene código**: en `"${stringResource(Res.string.x)}"` el uso
    #    está dentro de las llaves. Borrar la cadena entera lo borraba con ella.
    body = strip_string_literals(body)
    body = re.sub(r"/\*.*?\*/", " ", body, flags=re.S)
    body = re.sub(r"//.*", " ", body)

    for imported in imports:
        if " as " in imported or imported.endswith(".*"):
            continue
        simple = imported.rsplit(".", 1)[-1]
        if simple in OPERATORS or simple in linked:
            continue
        if not re.search(rf"\b{re.escape(simple)}\b", body):
            report(path, f"import sin usar: {imported}")


def check_labels_resolve(path: str, text: str) -> None:
    """`return@algo` donde `algo` no es ninguna lambda del archivo.

    Es un error de compilación —`Unresolved label`— y aun así se puede ver sin compilador: basta con
    que el nombre de la etiqueta aparezca como una llamada en alguna parte del archivo.

    Existe por un caso concreto. Al meter la red de seguridad se sustituyeron los
    `viewModelScope.launch` por `launchSafely` con un reemplazo mecánico, y dos `return@launch`
    quedaron apuntando a una lambda que ya no se llamaba así. La sustitución era correcta en todo lo
    demás, que es lo que la hizo fácil de dar por buena.
    """
    for label in set(re.findall(r"return@(\w+)", text)):
        # La etiqueta puede venir de una llamada —`launch {`— o de una etiqueta explícita —`bucle@`—.
        if re.search(rf"\b{re.escape(label)}\s*(?:\(|\{{|@)", text.replace(f"return@{label}", "")):
            continue
        report(path,
               f"la etiqueta '{label}' de un return@ no corresponde a ninguna lambda del archivo")


def check_privacy_guarantee() -> None:
    """La app promete que lo escaneado no sale del dispositivo. Esto comprueba que sea verdad.

    Es la comprobación más barata del archivo y la que cubre el fallo más caro. La garantía está
    escrita en el README, en la pantalla de Ajustes y en el propio manifiesto, y descansa en dos
    cosas: que no se pida `INTERNET` y que la copia de seguridad del sistema esté apagada.

    Lo segundo estuvo mal durante toda la vida del proyecto. `allowBackup="true"` subía
    `databases/` a la cuenta de Drive del usuario, y ahí va el `rawValue` literal de cada código —
    un QR de WiFi incluye la contraseña—. La app no pide permiso de internet, pero el backup **no lo
    hace la app**: lo hace un proceso del sistema que no necesita ese permiso.

    Nada avisaba, porque no es un error de compilación ni de lint: es una promesa de producto que
    depende de tres líneas de XML. Por eso vive aquí y no en la cabeza de nadie.
    """
    manifest = os.path.join(REPO, "androidApp", "src", "main", "AndroidManifest.xml")
    if not os.path.exists(manifest):
        report(manifest, "no existe: ¿se movió el módulo de Android?")
        return

    root = ET.parse(manifest).getroot()
    android = "{http://schemas.android.com/apk/res/android}"

    for permission in root.iter("uses-permission"):
        if permission.get(f"{android}name") == "android.permission.INTERNET":
            report(manifest, "declara INTERNET: la garantía de privacidad deja de ser cierta")

    application = root.find("application")
    if application is None:
        report(manifest, "sin <application>")
        return

    if application.get(f"{android}allowBackup") != "false":
        report(manifest, 'allowBackup debe ser "false": el historial acabaría en Drive')
    if not application.get(f"{android}dataExtractionRules"):
        report(
            manifest,
            "falta dataExtractionRules: desde Android 12 la transferencia entre dispositivos"
            " es un canal aparte que allowBackup no cierra",
        )


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
    check_privacy_guarantee()
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
