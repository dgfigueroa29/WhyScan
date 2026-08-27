package com.whyscan.feature.scanner

import com.whyscan.core.domain.scan.BarcodeValueParser
import com.whyscan.core.domain.scan.ResultAction
import com.whyscan.core.domain.scan.ResultActionsFactory
import com.whyscan.core.model.Barcode
import com.whyscan.core.model.BarcodeFormat
import com.whyscan.core.platform.isOpenableUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ata la lista blanca del borde a lo que el dominio produce de verdad.
 *
 * `isOpenableUri` vive en `:core:platform` y `ResultActionsFactory` en `:core:domain`, que no se
 * conocen entre sí — y no deben: el dominio no sabe que existe un sistema operativo. El precio de
 * esa separación es que la lista de esquemas está escrita **dos veces**, una en cada lado, y nada
 * obliga a que coincidan.
 *
 * Este test es lo que lo obliga. Vive aquí porque `:feature:scanner` es el primer módulo que ve los
 * dos, y falla en las dos direcciones que importan: si el dominio empieza a producir un esquema
 * nuevo, el borde lo rechazaría en silencio y la acción no haría nada; si alguien recorta la lista
 * blanca, deja de abrirse algo que sí es legítimo. Las dos son averías que no rompen la compilación.
 */
class OpenableUriDriftTest {

    @Test
    fun `todo lo que el dominio ofrece abrir pasa la lista blanca del borde`() {
        val abribles = VALORES_QUE_SE_ABREN.mapNotNull { value ->
            ResultActionsFactory.actionsFor(barcodeOf(value), canShare = true)
                .filterIsInstance<ResultAction.Open>()
                .singleOrNull()
        }

        // Si esto falla, el caso de prueba se quedó corto: la comprobación de abajo no valdría nada
        // sobre una lista más corta de lo que se cree.
        assertEquals(
            VALORES_QUE_SE_ABREN.size,
            abribles.size,
            "algún valor de prueba dejó de producir una acción de abrir",
        )

        abribles.forEach { open ->
            assertTrue(
                isOpenableUri(open.uri),
                "el dominio ofrece abrir '${open.uri}' (${open.kind}) y el borde lo rechazaría",
            )
        }
    }

    @Test
    fun `un valor hostil ni se ofrece abrir ni pasaria por el borde`() {
        // Las dos mitades importan. La primera es la que rige hoy: un QR con `javascript:` dentro se
        // clasifica como texto y no produce acción de abrir. La segunda es la red nueva: aunque
        // llegara al borde por un camino que hoy no existe, tampoco saldría de ahí.
        listOf("javascript:alert(1)", "intent://x#Intent;end", "file:///etc/passwd").forEach { value ->
            val acciones = ResultActionsFactory.actionsFor(barcodeOf(value), canShare = true)

            assertTrue(
                acciones.none { it is ResultAction.Open },
                "'$value' no debería ofrecer abrirse",
            )
            assertTrue(!isOpenableUri(value), "'$value' no debería pasar la lista blanca")
        }
    }

    /**
     * El `valueType` lo pone el parser y no el constructor de [Barcode], que por defecto deja
     * `Text`. Se construye igual que en producción para que un cambio de parseo se note aquí.
     */
    private fun barcodeOf(rawValue: String): Barcode = Barcode(
        rawValue = rawValue,
        format = BarcodeFormat.QrCode,
        valueType = BarcodeValueParser.parse(rawValue, BarcodeFormat.QrCode),
    )

    private companion object {
        /** Un valor por cada `OpenKind`, escritos como vendrían dentro de un código. */
        val VALORES_QUE_SE_ABREN = listOf(
            "https://example.com/a?b=c",
            "www.example.com",
            "mailto:alguien@example.com",
            "tel:+34 600 00 00 00",
            "smsto:+34600000000:hola",
            "geo:41.3874,2.1686",
        )
    }
}
