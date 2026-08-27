package com.whyscan.core.domain.scan

import com.whyscan.core.model.Barcode
import com.whyscan.core.model.BarcodeValueType

/**
 * El valor de un código preparado para que un lector de pantalla lo diga **carácter a carácter**.
 *
 * ### Por qué existe
 * Un EAN-13 escrito seguido es, para TalkBack o VoiceOver, un número de trece cifras: lo pronuncia
 * como *"siete billones quinientos un mil…"*. Para casi cualquier app eso sería un detalle; para
 * esta es el producto entero. El motivo de que el valor se pinte en monoespaciada es que alguien lo
 * **coteja carácter a carácter** contra una etiqueta impresa, y esa es exactamente la operación que
 * se le está negando a quien no puede ver la pantalla (RNF-05).
 *
 * ### Por qué está en el dominio, si hablar es cosa de la UI
 * Porque lo que se decide aquí no es *cómo suena* sino **qué clase de valor es**: si es un código
 * que se lee cifra a cifra o si es prosa que se lee como prosa. Eso es una afirmación sobre el
 * significado, que es justo lo que [BarcodeValueType] modela, y es la misma en los cuatro idiomas y
 * las cuatro plataformas. Lo que sigue siendo de la UI —y no está aquí— es la frase que envuelve al
 * valor: "Copiar %s" lo pone la pantalla con sus recursos traducibles, como manda D15.
 *
 * ### Qué se deletrea y qué no
 * Deletrear una URL sería peor que no hacer nada: `h t t p s d o s p u n t o s…` no lo entiende
 * nadie. Y un vCard entero, absurdo. Así que solo se deletrea lo que **no es una palabra**:
 *
 * - un [BarcodeValueType.Product], que es un GTIN por definición;
 * - un texto corto, sin espacios y mayoritariamente numérico — un número de serie, un lote, un
 *   Code 128 que ningún motor clasificó como producto.
 *
 * *Sin comprobar en dispositivo:* que la separación por espacios produzca exactamente la prosodia
 * esperada en TalkBack y VoiceOver. Es la técnica habitual y no depende del idioma, pero cómo suena
 * de verdad solo lo dice un teléfono — y eso ya está en el bloque de pendientes que necesitan uno.
 */
fun spokenValue(barcode: Barcode): String =
    if (isSpelledOut(barcode)) barcode.rawValue.toSpelledOut() else barcode.rawValue

/**
 * Longitud a partir de la cual deletrear deja de ayudar.
 *
 * Un código de producto o un número de serie caben de sobra; una cadena más larga que esto ya no es
 * algo que nadie coteje cifra a cifra, y oírla deletreada sería una tortura sin propósito.
 */
private const val MAX_SPELLED_LENGTH = 32

private fun isSpelledOut(barcode: Barcode): Boolean = when (barcode.valueType) {
    // Un GTIN es un código por definición: no hay caso en que convenga oírlo como cantidad.
    is BarcodeValueType.Product -> true

    // El resto de lo que llega como texto plano hay que mirarlo por dentro. Se exige mayoría de
    // dígitos y no "algún dígito" para que una palabra con un número detrás —`hola2`— se siga
    // leyendo como palabra, que es lo que es.
    is BarcodeValueType.Text -> barcode.rawValue.looksLikeACode()

    // URL, WiFi, correo, teléfono, contacto, evento y punto geográfico se leen como lo que son.
    else -> false
}

private fun String.looksLikeACode(): Boolean {
    if (isEmpty() || length > MAX_SPELLED_LENGTH) return false
    if (any { it.isWhitespace() }) return false

    val digits = count { it.isDigit() }
    return digits * 2 >= length
}

/**
 * Separa los caracteres con espacios, que es lo que hace que un lector los diga de uno en uno.
 *
 * No se toca nada más: ni se quitan guiones ni se agrupa de tres en tres. Lo que se anuncia tiene
 * que ser **lo mismo** que hay en la pantalla y en la etiqueta, o el cotejo deja de valer.
 */
private fun String.toSpelledOut(): String = toCharArray().joinToString(separator = " ")
