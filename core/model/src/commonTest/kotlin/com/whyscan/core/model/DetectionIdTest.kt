package com.whyscan.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * El id de una detección.
 *
 * Dejó de ser un detalle interno cuando aparecieron las notas: de este id cuelga texto que escribió
 * una persona, así que dos lecturas distintas que compartan id son una nota perdida.
 */
class DetectionIdTest {

    @Test
    fun `la misma lectura da el mismo id`() {
        // Es lo que evita que treinta frames del mismo código se apilen en el historial, y lo que
        // permite que `INSERT OR IGNORE` sea idempotente.
        val first = Detection.idOf(ScannerEngineId.ManualInput, "7501234567893", detectedAtMillis = 1_000)
        val second = Detection.idOf(ScannerEngineId.ManualInput, "7501234567893", detectedAtMillis = 1_000)

        assertEquals(first, second)
    }

    @Test
    fun `cambiar el motor, el instante o el valor cambia el id`() {
        val base = Detection.idOf(ScannerEngineId.ManualInput, "hola", detectedAtMillis = 1_000)

        assertNotEquals(base, Detection.idOf(ScannerEngineId.MlKitCameraX, "hola", detectedAtMillis = 1_000))
        assertNotEquals(base, Detection.idOf(ScannerEngineId.ManualInput, "hola", detectedAtMillis = 1_001))
        assertNotEquals(base, Detection.idOf(ScannerEngineId.ManualInput, "adios", detectedAtMillis = 1_000))
    }

    @Test
    fun `dos valores que colisionan en hashCode dan ids distintos`() {
        // "Aa" y "BB" es la colisión de manual de `String.hashCode()`: los dos dan 2112. Con el hash
        // de 32 bits que había antes, estas dos lecturas eran indistinguibles para el historial y la
        // segunda se descartaba en silencio.
        assertEquals("Aa".hashCode(), "BB".hashCode())

        val first = Detection.idOf(ScannerEngineId.ManualInput, "Aa", detectedAtMillis = 1_000)
        val second = Detection.idOf(ScannerEngineId.ManualInput, "BB", detectedAtMillis = 1_000)

        assertNotEquals(first, second)
    }

    @Test
    fun `un valor largo no alarga el id`() {
        // El id es la clave primaria del historial y la `key` de la lista: un QR puede traer un
        // vCard entero, y meterlo dentro haría de cada fila su propio problema.
        val long = "x".repeat(10_000)

        val id = Detection.idOf(ScannerEngineId.ManualInput, long, detectedAtMillis = 1_000)

        assertTrue(id.length < MAX_REASONABLE_ID_LENGTH, "id de ${id.length} caracteres")
    }

    private companion object {
        const val MAX_REASONABLE_ID_LENGTH = 64
    }
}
