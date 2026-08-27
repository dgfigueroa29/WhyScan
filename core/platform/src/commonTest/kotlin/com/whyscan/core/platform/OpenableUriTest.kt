package com.whyscan.core.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenableUriTest {

    @Test
    fun `deja pasar los seis esquemas que el dominio produce`() {
        listOf(
            "https://example.com/a?b=c",
            "http://example.com",
            "mailto:alguien@example.com?subject=hola",
            "tel:+34600000000",
            "sms:+34600000000",
            "geo:41.3874,2.1686",
        ).forEach { uri ->
            assertTrue(isOpenableUri(uri), "debería abrirse: $uri")
        }
    }

    @Test
    fun `rechaza los esquemas que convierten un codigo en una ejecucion`() {
        // `javascript:` es el clásico en Web; `intent:` arranca componentes de otras apps de
        // Android con extras que vienen del código; `file:` y `content:` leen del dispositivo.
        // Ninguno lo produce el dominio, y por eso ninguno debe salir de aquí.
        listOf(
            "javascript:alert(1)",
            "intent://scan/#Intent;scheme=zxing;end",
            "content://com.otra.app/datos/1",
            "file:///etc/passwd",
            "data:text/html,<script>alert(1)</script>",
        ).forEach { uri ->
            assertFalse(isOpenableUri(uri), "no debería abrirse: $uri")
        }
    }

    @Test
    fun `el esquema no distingue mayusculas`() {
        // Un código puede traerlo como sea, y el sistema lo trataría igual: si aquí distinguiera,
        // la comprobación sería fácil de saltar escribiendo `HTTPS://`.
        assertTrue(isOpenableUri("HTTPS://example.com"))
        assertTrue(isOpenableUri("MailTo:alguien@example.com"))
        assertFalse(isOpenableUri("JavaScript:alert(1)"))
    }

    @Test
    fun `falla cerrado cuando no hay esquema que mirar`() {
        listOf(
            "",
            "example.com",
            "//example.com",
            ":https://example.com",
            "  https://example.com",
        ).forEach { uri ->
            assertFalse(isOpenableUri(uri), "no debería abrirse: '$uri'")
        }
    }

    @Test
    fun `un esquema desconocido no se cuela por parecerse a uno permitido`() {
        // El prefijo `http` está dentro de `httpx`, y una comprobación por `startsWith` lo dejaría
        // pasar. Se compara el esquema entero justamente para que esto falle.
        assertFalse(isOpenableUri("httpx://example.com"))
        assertFalse(isOpenableUri("telnet://example.com"))
        assertFalse(isOpenableUri("smsto:+34600000000"))
    }
}
