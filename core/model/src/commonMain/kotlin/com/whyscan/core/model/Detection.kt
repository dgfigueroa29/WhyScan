package com.whyscan.core.model

/**
 * El evento de haber reconocido un [Barcode] con un motor concreto en un instante concreto.
 *
 * La separación entre [Barcode] y [Detection] es lo que habilita el objetivo del producto:
 * comparar motores. El mismo código físico produce detecciones distintas — distinto motor,
 * distinta latencia — y esas diferencias son el dato interesante.
 */
data class Detection(
    val id: String,
    val barcode: Barcode,
    val engineId: ScannerEngineId,
    val detectedAtMillis: Long,
    val latencyMillis: Long? = null,
    val source: ScanSource = ScanSource.LiveCamera,
) {
    companion object {
        /**
         * Identificador estable: dos lecturas del mismo código, con el mismo motor, en el mismo
         * milisegundo, son la misma detección. Evita duplicados en modo continuo sin necesidad de
         * un generador de UUID multiplataforma.
         *
         * ### Por qué no es `rawValue.hashCode()`
         *
         * Lo era, y el espacio de un hash de 32 bits es pequeño para lo que hoy cuelga de este id.
         * Dos valores distintos con el mismo hash, leídos por el mismo motor en el mismo
         * milisegundo, producen el mismo id — y con el `INSERT OR IGNORE` del historial la segunda
         * lectura **se descarta en silencio**, que es la peor forma de perder un dato. La
         * probabilidad es ínfima; lo que cambió es la consecuencia: desde que existen las notas,
         * de este id cuelga texto que escribió una persona.
         *
         * Se usa FNV-1a de 64 bits sobre el valor, escrito aquí en diez líneas en vez de traer una
         * dependencia de hashing por una función. No es criptográfico y no hace falta que lo sea:
         * esto no defiende de nadie, solo separa lecturas distintas. Con 64 bits la colisión deja
         * de ser algo que valga la pena pensar.
         *
         * `String.hashCode()` habría servido igual de bien en cuanto a **determinismo** —Kotlin lo
         * especifica igual en las cuatro plataformas—, así que el cambio es solo de anchura.
         */
        fun idOf(engineId: ScannerEngineId, rawValue: String, detectedAtMillis: Long): String =
            "${engineId.id}:$detectedAtMillis:${rawValue.fnv1a64()}"

        /**
         * FNV-1a de 64 bits, en hexadecimal.
         *
         * Recorre los `Char` y no los bytes UTF-8 a propósito: es una operación sobre una cadena de
         * Kotlin y así no depende de la codificación de ninguna plataforma. Para lo que se usa
         * —distinguir valores— da exactamente la misma garantía.
         *
         * Se formatea sobre el `Long` con signo, así que la mitad de los ids llevan un `-` delante.
         * Es feo y da igual: esto es una clave, no algo que nadie vaya a leer.
         */
        private fun String.fnv1a64(): String {
            var hash = FNV_OFFSET_BASIS
            for (char in this) {
                hash = (hash xor char.code.toLong()) * FNV_PRIME
            }
            return hash.toString(HEX_RADIX)
        }

        private const val FNV_OFFSET_BASIS = -3750763034362895579L // 14695981039346656037 sin signo
        private const val FNV_PRIME = 1099511628211L
        private const val HEX_RADIX = 16

        fun of(
            barcode: Barcode,
            engineId: ScannerEngineId,
            detectedAtMillis: Long,
            latencyMillis: Long? = null,
            source: ScanSource = ScanSource.LiveCamera,
        ): Detection = Detection(
            id = idOf(engineId, barcode.rawValue, detectedAtMillis),
            barcode = barcode,
            engineId = engineId,
            detectedAtMillis = detectedAtMillis,
            latencyMillis = latencyMillis,
            source = source,
        )
    }
}
