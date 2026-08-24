package com.whyscan.core.domain.scan

/**
 * Codificación en porcentaje para meter texto ajeno dentro de un URI.
 *
 * ## Por qué hace falta
 *
 * En un lector de códigos **el atacante controla el contenido entero** y la víctima solo apunta la
 * cámara. Los destinos que ofrece [ResultActionsFactory] se componían concatenando: si la dirección
 * de un correo trae `a@b.com?cc=otro@atacante.com&body=…`, esa concatenación produce un `mailto:`
 * con copia oculta y cuerpo puestos por quien imprimió el código, no por quien lo leyó.
 *
 * No es ejecución de nada: en Android y en iOS el URI acaba en un compositor que el usuario ve
 * **antes** de enviar. Es asistencia a phishing —un correo que parece que escribió él, dirigido a
 * quien el código diga— y se arregla en una línea por campo, así que no hay razón para dejarlo.
 *
 * El mismo argumento vale para `tel:` y `sms:`: un `#` sin codificar convierte el resto del número
 * en un fragmento de URI, que no es lo que el usuario cree estar marcando.
 *
 * ## Qué se conserva
 *
 * Todo lo que no sea *unreserved* de RFC 3986 se codifica, salvo lo que cada sitio declare en
 * [alsoKeep] porque **significa algo ahí**: la `@` que separa buzón de dominio, el `+` inicial de un
 * teléfono internacional —codificarlo como `%2B` confunde a algunos marcadores— y los separadores
 * visuales de un número. Ese es el criterio: se conserva lo que un destino legítimo necesita y se
 * codifica todo lo demás, no al revés.
 *
 * Se codifica **byte a byte sobre UTF-8**, que es lo que exige RFC 3986 para lo que no es ASCII: un
 * asunto con eñes o acentos tiene que llegar entero al cliente de correo.
 */
internal fun percentEncode(value: String, alsoKeep: String = ""): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val code = byte.toInt()
        val char = code.toChar()
        // `code >= 0` deja fuera todo lo no ASCII: en UTF-8 esos bytes son negativos como `Byte` y
        // van codificados uno a uno, que es justo lo que pide la norma.
        if (code >= 0 && (char in UNRESERVED || char in alsoKeep)) {
            append(char)
        } else {
            append('%')
            append(HEX_DIGITS[(code shr NIBBLE_BITS) and NIBBLE_MASK])
            append(HEX_DIGITS[code and NIBBLE_MASK])
        }
    }
}

/** Los `unreserved` de RFC 3986: nunca hace falta codificarlos y hacerlo solo afea el URI. */
private const val UNRESERVED =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

/** En mayúsculas porque es lo que recomienda RFC 3986 para las secuencias de escape. */
private const val HEX_DIGITS = "0123456789ABCDEF"

private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0xF
