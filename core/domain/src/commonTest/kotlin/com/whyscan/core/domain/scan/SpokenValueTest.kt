package com.whyscan.core.domain.scan

import com.whyscan.core.model.Barcode
import com.whyscan.core.model.BarcodeFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class SpokenValueTest {

    @Test
    fun `un EAN-13 se deletrea, que es el defecto que abrio la ronda 10`() {
        // Sin esto, el lector de pantalla dice "siete billones quinientos un mil…": el valor deja
        // de poder cotejarse contra la etiqueta impresa, que es para lo que existe la pantalla.
        assertEquals(
            "7 5 0 1 2 3 4 5 6 7 8 9 3",
            spokenValue(barcodeOf("7501234567893", BarcodeFormat.Ean13)),
        )
    }

    @Test
    fun `una URL se lee como una URL y no letra a letra`() {
        // Deletrear esto sería peor que no hacer nada.
        val url = "https://example.com/producto?id=42"

        assertEquals(url, spokenValue(barcodeOf(url)))
    }

    @Test
    fun `un vCard tampoco se deletrea`() {
        val vcard = "BEGIN:VCARD\nFN:Ana Ruiz\nTEL:+34600000000\nEND:VCARD"

        assertEquals(vcard, spokenValue(barcodeOf(vcard)))
    }

    @Test
    fun `un numero de serie leido como texto plano si se deletrea`() {
        // Un Code 128 que ningún motor clasificó como producto sigue siendo algo que alguien coteja
        // carácter a carácter contra una caja.
        assertEquals("A B 1 2 3 4", spokenValue(barcodeOf("AB1234")))
    }

    @Test
    fun `una palabra con un numero detras se sigue leyendo como palabra`() {
        // Es lo que separa "mayoría de dígitos" de "algún dígito": con la regla laxa, esto se
        // deletrearía y el resultado sería peor que el defecto que se está arreglando.
        assertEquals("edificio2", spokenValue(barcodeOf("edificio2")))
    }

    @Test
    fun `un texto largo no se deletrea aunque sea todo digitos`() {
        // Nadie coteja cifra a cifra algo de esta longitud, y oírlo deletreado sería una tortura.
        val largo = "1".repeat(33)

        assertEquals(largo, spokenValue(barcodeOf(largo)))
    }

    @Test
    fun `los separadores del valor se conservan al deletrear`() {
        // Lo que se anuncia tiene que ser lo mismo que hay en pantalla y en la etiqueta. Quitar el
        // guion "para que suene mejor" rompería justo el cotejo que esto existe para permitir.
        assertEquals("1 2 3 - 4 5 6", spokenValue(barcodeOf("123-456")))
    }

    @Test
    fun `un valor vacio no revienta`() {
        assertEquals("", spokenValue(barcodeOf("")))
    }

    private fun barcodeOf(
        rawValue: String,
        format: BarcodeFormat = BarcodeFormat.QrCode,
    ): Barcode = Barcode(
        rawValue = rawValue,
        format = format,
        // Como en producción: el tipo lo pone el parser, no el constructor.
        valueType = BarcodeValueParser.parse(rawValue, format),
    )
}
