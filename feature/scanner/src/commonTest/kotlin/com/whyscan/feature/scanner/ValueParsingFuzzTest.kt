package com.whyscan.feature.scanner

import com.whyscan.core.domain.scan.BarcodeValueParser
import com.whyscan.core.domain.scan.OpenKind
import com.whyscan.core.domain.scan.ResultAction
import com.whyscan.core.domain.scan.ResultActionsFactory
import com.whyscan.core.domain.scan.spokenValue
import com.whyscan.core.model.Barcode
import com.whyscan.core.model.BarcodeFormat
import com.whyscan.core.platform.isOpenableUri
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Barrido sobre el parseo semántico y las acciones que produce.
 *
 * ## Por qué un barrido y no más casos
 *
 * Los tests de `BarcodeValueParser` y de `ResultActionsFactory` comprueban **casos que alguien
 * pensó**. Eso deja fuera, por construcción, lo que nadie pensó — y en un lector de códigos ese
 * hueco no es teórico: **el atacante escribe el valor entero** y la víctima solo tiene que apuntar
 * la cámara. No hace falta que engañe a nadie para que su cadena llegue hasta aquí; le basta con
 * imprimirla.
 *
 * Lo que se afirma no es "el resultado es X" —eso ya lo hacen los otros tests— sino **invariantes**:
 * propiedades que tienen que valer para *cualquier* entrada. Un invariante es lo único que se puede
 * comprobar sobre entradas que nadie ha visto.
 *
 * ## Cómo se generan las entradas
 *
 * No con bytes al azar, que casi nunca producen nada interesante: se ensamblan de piezas que
 * **significan algo en este dominio** —prefijos de esquema, separadores de URI, nombres de parámetro
 * que un atacante querría inyectar, control y bidi, y trozos de valor legítimo—. Un fuzzer sin
 * gramática sobre un parser de texto pasa el rato explorando cadenas que el primer `startsWith`
 * descarta.
 *
 * ## Reproducible
 *
 * La semilla es fija, así que el corpus es **el mismo en cada ejecución y en las cuatro
 * plataformas** — `kotlin.random.Random(seed)` está especificado y no depende del host. Un fallo
 * aquí no es un fantasma: el mensaje lleva la entrada exacta, y esa entrada se pega tal cual en un
 * test de caso. Subir [CASOS] o cambiar [SEMILLA] explora más, y es la forma de usarlo cuando se
 * toca el parser.
 *
 * ## Por qué vive en `:feature:scanner`
 *
 * Por lo mismo que `OpenableUriDriftTest`: el parseo está en `:core:domain` y la lista blanca del
 * borde en `:core:platform`, dos módulos que no se conocen entre sí. Este es el primer sitio desde
 * el que se ven los dos, que es exactamente donde se puede afirmar que encajan.
 */
class ValueParsingFuzzTest {

    @Test
    fun `ninguna entrada hace saltar el parseo ni las acciones`() {
        // La primera propiedad, y la más aburrida hasta el día que falla: una excepción aquí sube
        // por el `Flow` del motor y se lleva la sesión de escaneo por delante.
        forEachCase { value, format ->
            val barcode = Barcode(value, format, BarcodeValueParser.parse(value, format))

            ResultActionsFactory.actionsFor(barcode, canShare = true)
            ResultActionsFactory.shareableContent(barcode)
            spokenValue(barcode)
        }
    }

    @Test
    fun `todo destino que se ofrece abrir pasa la lista blanca del borde`() {
        // `OpenableUriDriftTest` afirma esto sobre seis valores escritos a mano. Aquí sobre lo que
        // salga: si algún día el parser aprende un esquema nuevo, el borde lo rechazaría en silencio
        // y la acción no haría nada. Es una avería que no rompe la compilación.
        forEachCase { value, format ->
            openActionsOf(value, format).forEach { open ->
                assertTrue(
                    isOpenableUri(open.uri),
                    "el dominio ofrece abrir '${open.uri}' (${open.kind}) y el borde lo rechazaría" +
                        fallo(value, format),
                )
            }
        }
    }

    @Test
    fun `un enlace solo puede ser http o https`() {
        // Más fuerte que la lista blanca, y a propósito: la lista permite seis esquemas, pero de
        // ellos `BarcodeValueType.Url` solo debería producir dos. Si un día produjera un `tel:`, el
        // borde lo dejaría pasar y la UI diría "Abrir enlace" sobre algo que marca un número.
        forEachCase { value, format ->
            openActionsOf(value, format)
                .filter { it.kind == OpenKind.Link }
                .forEach { open ->
                    val scheme = open.uri.lowercase()
                    assertTrue(
                        scheme.startsWith("http://") || scheme.startsWith("https://"),
                        "un enlace con esquema inesperado: '${open.uri}'" + fallo(value, format),
                    )
                }
        }
    }

    @Test
    fun `nada de lo que trae el codigo puede componer el destino`() {
        // Es la propiedad que `percentEncode` existe para dar, dicha como invariante. Concatenando
        // crudo, una dirección con `?cc=…&body=…` dentro producía un `mailto:` con destinatarios y
        // cuerpo puestos por quien imprimió el código, y una `#` dentro de un teléfono partía el URI
        // en un fragmento — de modo que lo que se marcaba no era lo que el usuario leía en pantalla.
        forEachCase { value, format ->
            openActionsOf(value, format)
                .filter { it.kind in COMPUESTOS }
                .forEach { open ->
                    val cuerpo = open.uri.substringAfter(':')
                    val intrusos = cuerpo.filter { it in DELIMITADORES || it.isWhitespace() }

                    assertTrue(
                        intrusos.isEmpty(),
                        "el destino '${open.uri}' lleva delimitadores sin codificar: '$intrusos'" +
                            fallo(value, format),
                    )
                }
        }
    }

    @Test
    fun `deletrear no cambia ni un caracter del valor`() {
        // Lo que se anuncia tiene que ser **lo mismo** que hay en la pantalla y en la etiqueta, o el
        // cotejo carácter a carácter deja de valer — que es justo para lo que existe (RNF-05).
        forEachCase { value, format ->
            val barcode = Barcode(value, format, BarcodeValueParser.parse(value, format))
            val hablado = spokenValue(barcode)

            assertEquals(
                value.filterNot { it == ' ' },
                hablado.filterNot { it == ' ' },
                "deletrear alteró el valor" + fallo(value, format),
            )
        }
    }

    @Test
    fun `el parseo es una funcion, no un proceso`() {
        // Barato de comprobar y caro de descubrir tarde: si el parseo dependiera de algún estado
        // —una caché, un `lazy` mal puesto—, dos lecturas del mismo código darían acciones distintas
        // y el historial dejaría de ser reproducible.
        forEachCase { value, format ->
            assertEquals(
                BarcodeValueParser.parse(value, format),
                BarcodeValueParser.parse(value, format),
                "el parseo no es determinista" + fallo(value, format),
            )
        }
    }

    private fun openActionsOf(value: String, format: BarcodeFormat): List<ResultAction.Open> {
        val barcode = Barcode(value, format, BarcodeValueParser.parse(value, format))
        return ResultActionsFactory.actionsFor(barcode, canShare = true)
            .filterIsInstance<ResultAction.Open>()
    }

    /** El corpus, generado una vez por test para que cada uno sea independiente del anterior. */
    private fun forEachCase(check: (value: String, format: BarcodeFormat) -> Unit) {
        val random = Random(SEMILLA)
        val formats = BarcodeFormat.known.toList()

        repeat(CASOS) {
            check(randomValue(random), formats.random(random))
        }
    }

    private fun randomValue(random: Random): String = buildString {
        repeat(random.nextInt(0, MAX_PIEZAS)) {
            append(PIEZAS.random(random))
        }
    }

    /** Lo que hay que pegar en un test de caso para reproducir un fallo. */
    private fun fallo(value: String, format: BarcodeFormat): String =
        "\n  valor:   ${value.map { it.code }}\n  formato: ${format.id}\n  semilla: $SEMILLA"

    private companion object {
        /**
         * Fija a propósito: el corpus es el mismo en cada ejecución y en las cuatro plataformas, así
         * que un fallo aquí se puede reproducir. Cambiarla explora otro corpus — que es lo que hay
         * que hacer al tocar el parser, no dejarlo correr al azar en cada CI.
         */
        const val SEMILLA = 20_260_827L

        /**
         * Cuántas entradas. Son operaciones sobre cadenas cortas, así que el coste es despreciable
         * frente a lo que ya tarda arrancar la JVM; lo que de verdad decide qué se explora es
         * [PIEZAS] y no este número.
         */
        const val CASOS = 5_000

        /** Cuántas piezas se concatenan como mucho. Suficiente para un URI con query y fragmento. */
        const val MAX_PIEZAS = 8

        /** Las acciones cuyo destino **compone** la app a partir del contenido del código. */
        val COMPUESTOS = setOf(OpenKind.Email, OpenKind.Phone, OpenKind.Sms)

        /**
         * Lo que no puede aparecer sin codificar en un destino compuesto.
         *
         * `?` y `=` no están: el `mailto:` los pone la propia app al añadir `?subject=`. Lo que
         * ninguno de los tres puede llevar es un separador que el contenido haya colado.
         */
        const val DELIMITADORES = "#&;<>\"'\\"

        /**
         * Las piezas del corpus. No son bytes al azar: cada una **significa algo** para este parser
         * o para el URI que sale de él, que es lo que hace que el barrido llegue a algún sitio.
         */
        val PIEZAS: List<String> = listOf(
            // Prefijos que deciden la interpretación
            "http://", "https://", "HTTPS://", "www.", "WWW.",
            "mailto:", "MAILTO:", "tel:", "sms:", "SMSTO:", "geo:",
            "WIFI:", "wifi:", "BEGIN:VCARD", "BEGIN:VEVENT", "MATMSG:",
            // Esquemas que este producto no debe abrir jamás
            "javascript:", "intent://", "file:///", "content://", "data:text/html,", "vbscript:",
            // Separadores con los que se compone un URI
            "?", "&", "#", "=", ";", ":", "//", "@", ",", ".", "+", "-",
            // Nombres de parámetro que un atacante querría inyectar
            "subject=", "body=", "cc=", "bcc=", "to=", "S:", "P:", "T:WPA", "N:", "TEL:",
            // Espacios y control: separan, cortan líneas y no se ven al leer
            " ", "\t", "\n", "\r", "\u0000", "\u00A0",
            // Bidi y ancho cero. Escritos como escape **a propósito**: un fuente con caracteres
            // invisibles dentro es justo el problema que estos dos casos existen para probar, y
            // pegarlos literalmente lo metería en el repositorio en lugar de comprobarlo.
            "\u202E", "\u200B",
            // No ASCII, que en UTF-8 son varios bytes por carácter
            "ñ", "á", "日本", "🙂",
            // Trozos de valor legítimo
            "a", "0", "42", "7501234567893", "+34600123456", "usuario", "ejemplo.test",
            // Escapes ya escritos, para que no se codifiquen dos veces sin que nadie lo note
            "%00", "%2e%2e", "%41", "%", "\"", "'", "<script>", "\\",
        )
    }
}
