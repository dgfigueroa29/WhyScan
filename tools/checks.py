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


def check_privacy_guarantee(manifest: str | None = None) -> None:
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

    ## Lo que esta función **no** cubre, y por qué hay otra

    Mira el manifiesto **fuente**, que es lo único que existe antes de Gradle. Una dependencia que
    aporte `INTERNET` al fusionar manifiestos pasa por aquí sin que nadie se entere, y ese es
    precisamente el caso que la auditoría del 30-08-2026 marcó como el hueco más grande de toda la
    garantía: es el mismo error de forma que `allowBackup`, mirar solo lo que hace el código propio.

    El manifiesto fusionado solo existe después de `assembleDebug`, así que no puede comprobarse
    aquí sin romper lo que hace útil a este archivo —segundos, sin red y sin Gradle—. Lo hace
    `tools/merged_manifest.py`, en el job de Android de `Verify`, reutilizando esta misma función.
    """
    manifest = manifest or os.path.join(REPO, "androidApp", "src", "main", "AndroidManifest.xml")
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

        strings_en, plurals_en = declared(english)
        strings_es, plurals_es = declared(spanish)
        total_keys += len(strings_en) + len(plurals_en)

        for kind, en, es in (("cadena", strings_en, strings_es), ("plural", plurals_en, plurals_es)):
            for key in sorted(set(en) - set(es)):
                report(spanish, f"falta el {kind} '{key}', que sí está en inglés")
            for key in sorted(set(es) - set(en)):
                # El catálogo sin calificador es el respaldo de **cualquier** idioma, así que una
                # clave que solo existe en español revienta para todo el que no hable español.
                report(english, f"falta el {kind} '{key}', que sí está en español")

        # Un `<plurals>` al que le falta una cantidad no rompe la compilación: revienta en ejecución
        # y solo con el número que la usa. Es justo la clase de fallo que aparece con un elemento en
        # la lista y no con dos, así que se comprueba que las dos lenguas declaren las mismas.
        for key in sorted(set(plurals_en) & set(plurals_es)):
            if plurals_en[key] != plurals_es[key]:
                falta_es = plurals_en[key] - plurals_es[key]
                falta_en = plurals_es[key] - plurals_en[key]
                if falta_es:
                    report(spanish, f"al plural '{key}' le faltan cantidades: {sorted(falta_es)}")
                if falta_en:
                    report(english, f"al plural '{key}' le faltan cantidades: {sorted(falta_en)}")

        module = os.path.dirname(os.path.dirname(catalog))
        used_strings, used_plurals, imported = set(), set(), set()
        for source in walk(module, ".kt"):
            text = open(source, encoding="utf-8").read()
            used_strings |= set(re.findall(r"Res\.string\.(\w+)", text))
            used_plurals |= set(re.findall(r"Res\.plurals\.(\w+)", text))
            imported |= set(re.findall(r"^import [\w.]+\.resources\.(\w+)$", text, re.M))

        for accessor, used, declared_keys in (
            ("string", used_strings, set(strings_en)),
            ("plurals", used_plurals, set(plurals_en)),
        ):
            for key in sorted(used - imported):
                report(module, f"se usa Res.{accessor}.{key} sin importarla")
            for key in sorted(used - declared_keys):
                report(module, f"Res.{accessor}.{key} no está declarada en ningún catálogo")

        huerfanas = (set(strings_en) | set(plurals_en)) - used_strings - used_plurals
        for key in sorted(huerfanas):
            report(module, f"clave huérfana '{key}': está declarada y no la usa nadie")

    return total_keys


def declared(path: str) -> tuple[list[str], dict[str, set[str]]]:
    """Las cadenas y los plurales de un catálogo.

    Los plurales vienen con el conjunto de `quantity` que declaran, que es lo que hay que comparar
    entre idiomas: `one` y `other` no son las mismas en todas las lenguas y olvidar una no lo dice
    nadie hasta que un usuario da con ese número.
    """
    strings, plurals = [], {}
    for element in ET.parse(path).getroot():
        name = element.get("name")
        if element.tag == "plurals":
            plurals[name] = {item.get("quantity") for item in element}
        else:
            strings.append(name)
    return strings, plurals


def check_adr() -> int:
    """Cabeceras de los ADR y paridad con su índice.

    Los ADR son el registro de por qué este proyecto es como es, y su valor depende de dos cosas
    aburridas que nadie comprueba a ojo: que cada uno lleve estado y fecha, y que el índice de
    `docs/adr/README.md` no se separe de los archivos. Lo segundo se rompe siempre igual — se añade
    un ADR y se olvida la fila—, y el resultado es un índice que parece completo y no lo está, que
    es peor que no tenerlo.

    No comprueba el contenido: si un ADR merece existir o si su tabla de alternativas dice algo útil
    no lo decide un script.
    """
    directory = os.path.join(REPO, "docs", "adr")
    index_path = os.path.join(directory, "README.md")
    if not os.path.isdir(directory):
        return 0
    if not os.path.exists(index_path):
        report(index_path, "falta el índice de los ADR")
        return 0

    index = open(index_path, encoding="utf-8").read()
    linked = set(re.findall(r"\((ADR-\d{4}[^)]*\.md)\)", index))

    files = sorted(f for f in os.listdir(directory) if re.match(r"ADR-\d{4}.*\.md$", f))
    for name in files:
        path = os.path.join(directory, name)
        text = open(path, encoding="utf-8").read()

        title = re.match(r"# ADR-(\d{4}) — .+", text)
        if not title:
            report(path, "la primera línea debe ser '# ADR-NNNN — <la decisión>'")
        elif title.group(1) != name[4:8]:
            report(path, f"el título dice ADR-{title.group(1)} y el archivo ADR-{name[4:8]}")

        if not re.search(r"^- \*\*Estado:\*\* .+", text, re.M):
            report(path, "falta la línea '- **Estado:**'")
        if not re.search(r"^- \*\*Fecha:\*\* \d{4}-\d{2}-\d{2}", text, re.M):
            report(path, "falta la línea '- **Fecha:** AAAA-MM-DD'")

        for section in ("## Contexto", "## Decisión", "## Consecuencias"):
            if section not in text:
                report(path, f"falta la sección '{section}'")

        if name not in linked:
            report(index_path, f"{name} no está en el índice")

    for name in sorted(linked):
        if not os.path.exists(os.path.join(directory, name)):
            report(index_path, f"el índice enlaza {name}, que no existe")

    return len(files)


def check_agent_contract() -> None:
    """`AGENTS.md` y `CLAUDE.md` existen y se enlazan entre sí.

    Son dos archivos que dicen lo mismo en dos idiomas, y eso garantiza que algún día se separen
    (ADR-0016). Comparar el contenido no se puede; lo que sí se puede es cerrar el caso real: que
    alguien mueva o renombre uno y el otro se quede apuntando al vacío, con un agente leyendo la
    mitad de las reglas sin saber que le falta la otra.
    """
    agents = os.path.join(REPO, "AGENTS.md")
    claude = os.path.join(REPO, "CLAUDE.md")

    for path in (agents, claude):
        if not os.path.exists(path):
            report(path, "no existe: es el contrato que leen los agentes (ADR-0016)")
            return

    if "CLAUDE.md" not in open(agents, encoding="utf-8").read():
        report(agents, "no enlaza CLAUDE.md, que es su espejo en castellano")
    if "AGENTS.md" not in open(claude, encoding="utf-8").read():
        report(claude, "no enlaza AGENTS.md, que es el contrato normativo")


DELTA_HEADINGS = ("## ADDED Requirements", "## MODIFIED Requirements", "## REMOVED Requirements")


def check_requirements(path: str, text: str) -> None:
    """Todo `### Requirement:` lleva al menos un `#### Scenario:`.

    Es la única regla de OpenSpec que se puede comprobar sin entender el dominio, y es justo la que
    separa un requisito de un deseo: sin escenario no hay forma de decir si se cumple. Un directorio
    de especificaciones lleno de frases que nadie puede contrastar es decoración cara.
    """
    current, has_scenario = None, False
    for line in text.split("\n") + ["### Requirement: fin"]:
        if line.startswith("### Requirement:"):
            if current and not has_scenario:
                report(path, f"'{current}' no tiene ningún '#### Scenario:'")
            current, has_scenario = line[len("### Requirement:"):].strip(), False
        elif line.startswith("#### Scenario:"):
            has_scenario = True


def check_openspec() -> int:
    """La forma de los cambios en curso y de las especificaciones vigentes.

    La CLI de OpenSpec valida esto mismo, y aquí no se puede usar: el entorno de desarrollo no tiene
    red (ADR-0017). Lo que se comprueba es la estructura, no el criterio — que un cambio esté bien
    planteado lo dice una persona.
    """
    root = os.path.join(REPO, "openspec")
    if not os.path.isdir(root):
        return 0

    changes = os.path.join(root, "changes")
    if os.path.isdir(changes):
        for name in sorted(os.listdir(changes)):
            change = os.path.join(changes, name)
            if name == "archive" or not os.path.isdir(change):
                continue

            for required in ("proposal.md", "tasks.md"):
                if not os.path.exists(os.path.join(change, required)):
                    report(change, f"al cambio le falta {required}")

            # Un cambio de herramienta de build no tiene delta que escribir, y obligarle a
            # inventarse una capacidad sería peor que la regla: metería en `specs/` requisitos
            # sobre cosas que el usuario no puede observar. Para eximirse hay que **decirlo** en
            # la cabecera de la propuesta, que es justo la afirmación que se quiere revisar.
            proposal = os.path.join(change, "proposal.md")
            exempt = (os.path.exists(proposal)
                      and "**Capability:** none" in open(proposal, encoding="utf-8").read())

            deltas = sorted(walk(os.path.join(change, "specs"), "spec.md"))
            if not deltas and not exempt:
                report(change, "sin delta en specs/<capacidad>/spec.md: no dice qué cambia."
                               " Si de verdad no cambia comportamiento observable, la propuesta"
                               " tiene que declarar '**Capability:** none' y decir por qué")
            if deltas and exempt:
                report(change, "declara '**Capability:** none' y trae delta: una de las dos sobra")

            for delta in deltas:
                text = open(delta, encoding="utf-8").read()
                headings = re.findall(r"^## .*Requirements\s*$", text, re.M)
                if not headings:
                    report(delta, "sin '## ADDED|MODIFIED|REMOVED Requirements'")
                for heading in headings:
                    if heading.strip() not in DELTA_HEADINGS:
                        report(delta, f"cabecera de delta no válida: '{heading.strip()}'")
                check_requirements(delta, text)

    specs = os.path.join(root, "specs")
    total = 0
    for spec in sorted(walk(specs, "spec.md")) if os.path.isdir(specs) else []:
        text = open(spec, encoding="utf-8").read()
        total += len(re.findall(r"^### Requirement:", text, re.M))

        for section in ("## Purpose", "## Requirements"):
            if section not in text:
                report(spec, f"falta la sección '{section}'")
        for heading in DELTA_HEADINGS:
            if heading in text:
                # Una especificación vigente describe lo que el sistema hace hoy. Si conserva la
                # cabecera del delta es que `/spec-apply` se quedó a medias, y lo que queda es una
                # fuente de verdad que miente sobre su propio estado.
                report(spec, f"conserva '{heading}': el delta no llegó a integrarse")
        check_requirements(spec, text)

    return total


# Los identificadores de `ScannerEngineId` en el estilo de cada sitio. El código usa `PascalCase`
# —`GmsCodeScanner`— y `docs/ENGINES.md` usa `SCREAMING_SNAKE_CASE` —`GMS_CODE_SCANNER`—, así que
# hay que traducir para compararlos. No se deriva de uno a otro con una regla porque `ZXingCpp` y
# `MlKitOcr` no la cumplirían: `ZXING_CPP` y `MLKIT_OCR` no salen de partir por mayúsculas.
ENGINE_IDS = {
    "GmsCodeScanner": "GMS_CODE_SCANNER",
    "MlKitCameraX": "MLKIT_CAMERAX",
    "VisionIos": "VISION_IOS",
    "ZXingCpp": "ZXING_CPP",
    "ZXingJava": "ZXING_JAVA",
    "BrowserDetector": "BROWSER_DETECTOR",
    "MlKitOcr": "MLKIT_OCR",
    "VisionOcr": "VISION_OCR",
    "ManualInput": "MANUAL_INPUT",
}

PLATFORMS = {"Android": "Android", "Ios": "iOS", "Desktop": "Desktop", "Web": "Web"}


def check_engine_catalog() -> int:
    """La tabla maestra de `docs/ENGINES.md` contra `ScannerEngineCatalog`.

    ## Esta comprobación existía en la documentación antes que en ninguna parte

    Catorce archivos —`AGENTS.md` entre ellos, como regla de cabecera— decían que "un test verifica
    que `ENGINES.md` y el catálogo no divergen". **No lo verificaba nadie.**
    `ScannerEngineCatalogTest` comprueba invariantes internos del catálogo —que cubre todos los
    identificadores, que no hay duplicados, que la fase es válida— y no abre el documento; no puede,
    porque un test de `commonTest` en KMP no tiene sistema de archivos.

    Que la garantía más citada del repositorio fuera imaginaria lo destapó una auditoría, y la
    respuesta correcta no era borrar la frase de catorce sitios: era hacerla cierta donde sí se
    puede, que es aquí. Este archivo ya lee Markdown y XML y ya corre el primero en `Verify`.

    Compara identificador, fase y plataformas. El nombre visible y la dependencia se quedan fuera a
    propósito: son texto de producto, cambian de redacción sin cambiar de significado, y exigir que
    coincidan carácter a carácter convierte la comprobación en un estorbo.
    """
    doc = os.path.join(REPO, "docs", "ENGINES.md")
    code = os.path.join(REPO, "core", "scanner-api", "src", "commonMain", "kotlin", "com",
                        "whyscan", "core", "scanner", "catalog", "ScannerEngineCatalog.kt")
    if not (os.path.exists(doc) and os.path.exists(code)):
        report(doc, "falta ENGINES.md o ScannerEngineCatalog.kt: no se pueden contrastar")
        return 0

    declared: dict[str, tuple[int, set[str]]] = {}
    for entry in re.findall(r"ScannerEngineDescriptor\((.*?)\n    \)", open(code, encoding="utf-8")
                            .read(), re.S):
        name = re.search(r"id = ScannerEngineId\.(\w+)", entry)
        phase = re.search(r"plannedPhase = (\d+)", entry)
        platforms = re.search(r"platforms = (.+)", entry)
        if not (name and phase and platforms):
            continue
        readable = ENGINE_IDS.get(name.group(1))
        if readable is None:
            report(code, f"ScannerEngineId.{name.group(1)} no está en ENGINE_IDS de checks.py")
            continue
        # La entrada manual declara `ScannerPlatform.entries.toSet()` en vez de enumerar: está en
        # todas y escribirlas una a una se quedaría corto el día que aparezca una quinta.
        if "entries" in platforms.group(1):
            targets = set(PLATFORMS.values())
        else:
            targets = {PLATFORMS.get(p, p)
                       for p in re.findall(r"ScannerPlatform\.(\w+)", platforms.group(1))}
        declared[readable] = (int(phase.group(1)), targets)

    documented: dict[str, tuple[int, set[str]]] = {}
    for row in re.findall(r"^\| `(\w+)` *\|([^|]*)\|([^|]*)\|[^|]*\|([^|]*)\|",
                          open(doc, encoding="utf-8").read(), re.M):
        identifier, _, platforms, phase = row
        number = re.search(r"\d+", phase)
        if not number:
            continue
        listed = {p.strip() for p in platforms.split(",") if p.strip()}
        documented[identifier] = (
            int(number.group()),
            set(PLATFORMS.values()) if listed == {"Todas"} else listed,
        )

    for identifier in sorted(set(declared) - set(documented)):
        report(doc, f"{identifier} está en el catálogo y no en la tabla maestra")
    for identifier in sorted(set(documented) - set(declared)):
        report(doc, f"la tabla maestra lista {identifier}, que no está en el catálogo")

    for identifier in sorted(set(declared) & set(documented)):
        (phase, platforms), (doc_phase, doc_platforms) = declared[identifier], documented[identifier]
        if phase != doc_phase:
            report(doc, f"{identifier}: la tabla dice fase {doc_phase} y el catálogo {phase}")
        if platforms != doc_platforms:
            report(doc, f"{identifier}: la tabla dice {sorted(doc_platforms)} y el catálogo"
                        f" {sorted(platforms)}")

    return len(declared)


# Los tres archivos de `:core:designsystem` que hoy no dependen de la marca y se podrían compartir
# con otra app tal cual (ADR-0018). No es una lista de deseos: es lo que la comprobación de abajo
# mantiene cierto. Radius, Typography y Theme quedan fuera **porque sus valores son de WhyScan**,
# aunque su mecánica sea genérica; separarlos es el cambio `federate-design-system`, no un hecho.
FOUNDATION = ("Contrast.kt", "AppLanguage.kt", "LocalSnackbarHostState.kt")


def check_design_system() -> int:
    """Higiene del sistema de diseño: paridad claro/oscuro, colores sueltos y fuga de marca.

    ## Por qué la paridad de roles está aquí y no en un test

    Sí hay tests de contraste, y no cubren esto. El defecto es **declarar un rol en un tema y no en
    el otro**: `lightColorScheme()` rellena lo que no se le pasa con la paleta de fábrica de
    Material —morados y granates—, así que el rol olvidado no falla, *sale de otro color*. Ya pasó
    dos veces, y está contado en el KDoc de `Theme.kt`: primero con los `on*` —el texto de un botón
    primario salía morado en oscuro— y después con los `*Container`, que pintan el `FilterChip`
    seleccionado, la `Card` y el `NavigationBar`.

    Un test de contraste no lo caza porque mide los pares que se le nombran, y el rol olvidado no
    está en ninguna lista. Comparar los dos esquemas sí, y cuesta veinte líneas.

    ## Por qué los colores literales

    Un `Color(0x...)` fuera de la paleta es un color que no mide nadie: no entra en `ContrastTest`,
    no cambia con el tema y no aparece cuando alguien busca "de qué color es esto". Había uno —el
    verde del contorno de las detecciones, escrito a mano en `ScanOverlay`—, y estaba en el único
    sitio donde nadie lo iba a buscar: encima del vídeo, que es justo donde el contraste importa.
    """
    directory = os.path.join(REPO, "core", "designsystem", "src", "commonMain", "kotlin",
                             "com", "whyscan", "core", "designsystem")
    if not os.path.isdir(directory):
        return 0

    def block(text: str, pattern: str, item: str) -> set[str]:
        found = re.search(pattern, text, re.S)
        return set(re.findall(item, found.group(1), re.M)) if found else set()

    palette_path = os.path.join(directory, "ScannerPalette.kt")
    theme_path = os.path.join(directory, "Theme.kt")

    roles = 0
    if os.path.exists(palette_path):
        palette = open(palette_path, encoding="utf-8").read()
        light = block(palette, r"object Light \{(.*?)\n    \}", r"const val (\w+)")
        dark = block(palette, r"object Dark \{(.*?)\n    \}", r"const val (\w+)")
        roles = len(light & dark)
        for name in sorted(light - dark):
            report(palette_path, f"ScannerPalette.Dark no declara {name}, que sí está en Light")
        for name in sorted(dark - light):
            report(palette_path, f"ScannerPalette.Light no declara {name}, que sí está en Dark")

    if os.path.exists(theme_path):
        theme = open(theme_path, encoding="utf-8").read()
        light = block(theme, r"lightColorScheme\((.*?)\n\)", r"^\s{4}(\w+) =")
        dark = block(theme, r"darkColorScheme\((.*?)\n\)", r"^\s{4}(\w+) =")
        for name in sorted(light - dark):
            # Sin declarar, Material lo rellena con su paleta de fábrica: no falla, cambia de color.
            report(theme_path, f"el esquema oscuro no fija '{name}': Material lo pondrá de fábrica")
        for name in sorted(dark - light):
            report(theme_path, f"el esquema claro no fija '{name}': Material lo pondrá de fábrica")

    for source in ("core", "engines", "feature", "composeApp", "androidApp"):
        for path in walk(os.path.join(REPO, source), ".kt"):
            if os.path.basename(path) == "ScannerPalette.kt":
                continue
            for literal in set(re.findall(r"Color\((0x[0-9A-Fa-f]{6,8})", open(path, encoding="utf-8").read())):
                report(path, f"color literal {literal} fuera de ScannerPalette: nadie lo mide")

    for name in FOUNDATION:
        path = os.path.join(directory, name)
        if not os.path.exists(path):
            report(directory, f"falta {name}, declarado como base sin marca en ADR-0018")
            continue
        text = open(path, encoding="utf-8").read()
        for brand in ("ScannerPalette", "BrandMark"):
            if re.search(rf"\b{brand}\b", text):
                report(path, f"depende de {brand}: deja de ser compartible sin la marca (ADR-0018)")

    return roles


# Nombres que terminan en `Test` y **no** son una clase de test: son *source sets* de KMP o tareas
# de Gradle. Sin esta lista, `jvmTest` o `commonTest` se leerían como tests inexistentes.
TEST_SOURCE_SETS = {
    "commonTest", "jvmTest", "desktopTest", "androidTest", "androidUnitTest",
    "iosTest", "wasmJsTest", "testDebugUnitTest", "unitTest",
}

# Los archivos que describen **la verdad de hoy**. Las propuestas de `openspec/changes/` quedan
# fuera a propósito: nombran tests que todavía no existen, que es exactamente su trabajo.
HARNESS_TRUTH = ("AGENTS.md", "CLAUDE.md", "CONTRIBUTING.md", "CONTRIBUTING.es.md")
HARNESS_DIRS = (".claude", os.path.join("docs", "ai"), os.path.join("openspec", "specs"))


def frontmatter(text: str) -> dict[str, str]:
    """Los campos del bloque YAML de cabecera. No es un parser: son pares `clave: valor`."""
    match = re.match(r"---\n(.*?)\n---\n", text, re.S)
    if not match:
        return {}
    return {
        key.strip(): value.strip()
        for key, _, value in (line.partition(":") for line in match.group(1).split("\n"))
        if key.strip() and not key.startswith(" ")
    }


def check_harness() -> int:
    """Que el harness no afirme cosas que no existen.

    ## Por qué existe

    La auditoría del 30-08-2026 encontró que catorce archivos —`AGENTS.md` incluido— prometían "un
    test que verifica que `ENGINES.md` y el catálogo no divergen", y **ese test no existía**. Nadie
    lo notó en dos años porque nada comprobaba las afirmaciones del propio contrato.

    Esto no mide si el harness *sirve* —eso necesita evaluaciones de verdad, y está anotado como el
    hueco abierto nº1 en `docs/ai/state-of-the-art.md`—. Mide algo más modesto y que se puede hacer
    hoy: **que no mienta**. Un agente que sigue instrucciones que citan un test inexistente hace
    trabajo inútil con total confianza, que es la peor combinación.

    Comprueba tres cosas:

    - Que cada agente, skill y comando lleve su cabecera, y que el nombre de una skill coincida con
      su carpeta — si no coinciden, la skill no carga y no lo dice nadie.
    - Que cada `XxxTest` citado en un archivo que describe la verdad de hoy exista de verdad.
    - Que cada `check_xxx()` citado exista en `tools/`.

    Las propuestas de `openspec/changes/` quedan fuera: nombran lo que todavía no existe, y esa es
    su función.
    """
    root = os.path.join(REPO, ".claude")
    if not os.path.isdir(root):
        return 0

    pieces = 0

    for kind, directory, required in (
        ("agente", os.path.join(root, "agents"), ("name", "description")),
        ("comando", os.path.join(root, "commands"), ("description",)),
    ):
        for path in sorted(walk(directory, ".md")) if os.path.isdir(directory) else []:
            pieces += 1
            fields = frontmatter(open(path, encoding="utf-8").read())
            for key in required:
                if not fields.get(key):
                    report(path, f"al {kind} le falta '{key}' en la cabecera YAML")

    skills = os.path.join(root, "skills")
    for path in sorted(walk(skills, "SKILL.md")) if os.path.isdir(skills) else []:
        pieces += 1
        fields = frontmatter(open(path, encoding="utf-8").read())
        for key in ("name", "description"):
            if not fields.get(key):
                report(path, f"a la skill le falta '{key}' en la cabecera YAML")
        folder = os.path.basename(os.path.dirname(path))
        if fields.get("name") and fields["name"] != folder:
            # Si no coinciden, la skill no carga — y el fallo es silencioso.
            report(path, f"se llama '{fields['name']}' y vive en '{folder}': no cargará")

    existing_tests = {
        os.path.basename(path)[: -len(".kt")]
        for path in walk(REPO, "Test.kt")
    }
    functions = set()
    for path in walk(os.path.join(REPO, "tools"), ".py"):
        functions |= set(re.findall(r"^def (\w+)", open(path, encoding="utf-8").read(), re.M))

    targets = [os.path.join(REPO, name) for name in HARNESS_TRUTH]
    for directory in HARNESS_DIRS:
        targets += sorted(walk(os.path.join(REPO, directory), ".md"))

    for path in targets:
        if not os.path.exists(path):
            continue
        text = open(path, encoding="utf-8").read()
        for name in sorted(set(re.findall(r"`(\w+Test)`", text))):
            if name not in existing_tests and name not in TEST_SOURCE_SETS:
                report(path, f"cita el test '{name}', que no existe")
        for name in sorted(set(re.findall(r"`(check_\w+)\(\)`", text))):
            if name not in functions:
                report(path, f"cita '{name}()' de tools/, que no existe")

    return pieces


def check_markdown_links() -> None:
    """Enlaces relativos entre archivos del repositorio que no llevan a ninguna parte.

    La documentación de este proyecto es fuente de verdad y está cosida con enlaces: el README
    apunta a los ADR, los ADR a las secciones del SDD, el contrato de los agentes a todo lo demás.
    Un renombrado rompe unos cuantos en silencio, y un enlace roto en la guía de entrada es lo
    primero que ve quien llega.

    Solo mira enlaces a archivos. Las anclas dentro de un documento y las URL externas quedan fuera:
    comprobarlas exige interpretar el Markdown la primera y tener red la segunda.
    """
    for path in walk(REPO, ".md"):
        text = open(path, encoding="utf-8").read()
        for target in set(re.findall(r"\]\(([^)\s]+)\)", text)):
            if target.startswith(("http://", "https://", "#", "mailto:")):
                continue
            relative = target.split("#")[0]
            if not relative:
                continue
            resolved = os.path.normpath(os.path.join(os.path.dirname(path), relative))
            if not os.path.exists(resolved):
                report(path, f"enlace roto: {target}")


def main() -> int:
    check_kotlin_sources()
    check_privacy_guarantee()
    check_xml_is_well_formed()
    keys = check_compose_resources()
    adrs = check_adr()
    check_agent_contract()
    requirements = check_openspec()
    roles = check_design_system()
    engines = check_engine_catalog()
    pieces = check_harness()
    check_markdown_links()

    print(f"{keys} claves de recursos con paridad inglés/español")
    print(f"{adrs} ADR indexados, {requirements} requisitos vigentes en openspec/specs")
    print(f"{roles} roles de color con paridad claro/oscuro")
    print(f"{engines} motores con la tabla maestra y el catálogo de acuerdo")
    print(f"{pieces} piezas del harness con cabecera válida y sin citar nada inexistente")

    if problems:
        print(f"\n{len(problems)} hallazgos:\n")
        for problem in problems:
            print(f"  - {problem}")
        return 1

    print("sin hallazgos")
    return 0


if __name__ == "__main__":
    sys.exit(main())
