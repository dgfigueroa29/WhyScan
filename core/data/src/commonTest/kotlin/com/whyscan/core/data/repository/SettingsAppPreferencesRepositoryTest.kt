package com.whyscan.core.data.repository

import app.cash.turbine.test
import com.russhwolf.settings.MapSettings
import com.whyscan.core.domain.repository.AppLanguage
import com.whyscan.core.domain.repository.ThemeMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El almacén de las preferencias de app, que hasta ahora no tenía un solo test.
 *
 * Lo encontró la primera medición de cobertura: `:core:data` estaba en 60,8 % y este fichero era una
 * de las dos razones. No se escribe para subir el número —eso es la peor razón para escribir un
 * test— sino porque el fichero tenía **dos decisiones documentadas en comentarios y nada que las
 * sostuviera**, y las dos se rompen sin que compile nada mal:
 *
 * - los enums se guardan por su `id` estable: renombrar una constante de Kotlin o reordenar el enum
 *   cambiaría en silencio el tema y el idioma de todo el mundo que ya tuviera la app instalada;
 * - `lastSeenNewsRevision` distingue `null` de cero, que es lo único que impide que la pantalla de
 *   novedades salte en el primer arranque, justo cuando más estorba.
 */
class SettingsAppPreferencesRepositoryTest {

    @Test
    fun `sin nada guardado devuelve los valores por defecto`() = runTest {
        val preferences = SettingsAppPreferencesRepository(MapSettings()).current()

        assertEquals(ThemeMode.System, preferences.themeMode)
        assertEquals(AppLanguage.System, preferences.language)
        assertFalse(preferences.advancedMode)
        assertFalse(preferences.dyslexiaFriendly)
        assertNull(preferences.lastSeenNewsRevision, "nunca escrito tiene que ser null, no cero")
    }

    @Test
    fun `lo elegido sobrevive a recrear el repositorio`() = runTest {
        val settings = MapSettings()

        SettingsAppPreferencesRepository(settings).apply {
            setThemeMode(ThemeMode.Dark)
            setLanguage(AppLanguage.Spanish)
            setAdvancedMode(enabled = true)
            setDyslexiaFriendly(enabled = true)
            setLastSeenNewsRevision(revision = 3)
        }

        val reopened = SettingsAppPreferencesRepository(settings).current()

        assertEquals(ThemeMode.Dark, reopened.themeMode)
        assertEquals(AppLanguage.Spanish, reopened.language)
        assertTrue(reopened.advancedMode)
        assertTrue(reopened.dyslexiaFriendly)
        assertEquals(3, reopened.lastSeenNewsRevision)
    }

    @Test
    fun `los enums se guardan por su id estable, no por su nombre de Kotlin`() = runTest {
        // Si alguien cambia `mode.id` por `mode.name` esto se pone rojo, y esa es toda la gracia:
        // el cambio compila, pasa el resto de los tests y le cambia el tema a quien ya tenía la app.
        val settings = MapSettings()

        SettingsAppPreferencesRepository(settings).apply {
            setThemeMode(ThemeMode.Dark)
            setLanguage(AppLanguage.English)
        }

        assertEquals("dark", settings.getStringOrNull("app.theme_mode"))
        assertEquals("en", settings.getStringOrNull("app.language"))
    }

    @Test
    fun `un id que ya no existe vuelve al valor por defecto en vez de romper el arranque`() = runTest {
        // Puede venir de una versión anterior que tuviera un tema o un idioma que ya se quitó.
        val settings = MapSettings().apply {
            putString("app.theme_mode", "sepia")
            putString("app.language", "eo")
        }

        val preferences = SettingsAppPreferencesRepository(settings).current()

        assertEquals(ThemeMode.System, preferences.themeMode)
        assertEquals(AppLanguage.System, preferences.language)
    }

    @Test
    fun `haber visto la tanda cero no es lo mismo que no haber visto ninguna`() = runTest {
        // La distinción entera de `lastSeenNewsRevision`. Con `getInt(…, 0)` en lugar de
        // `getIntOrNull` los dos casos darían cero, y a quien acaba de instalar la app se le
        // estrenarían unas novedades que para él no son novedad de nada.
        val recienInstalado = SettingsAppPreferencesRepository(MapSettings()).current()
        assertNull(recienInstalado.lastSeenNewsRevision)

        val yaLaTenia = SettingsAppPreferencesRepository(
            MapSettings().apply { putInt("app.last_seen_news_revision", 0) },
        ).current()
        assertEquals(0, yaLaTenia.lastSeenNewsRevision)
    }

    @Test
    fun `el flujo observable emite cada cambio`() = runTest {
        // La pantalla de ajustes se dibuja desde este flujo: si un cambio no llega, el interruptor
        // se queda donde estaba aunque la preferencia sí haya cambiado en disco.
        val repository = SettingsAppPreferencesRepository(MapSettings())

        repository.observePreferences().test {
            assertEquals(ThemeMode.System, awaitItem().themeMode)

            repository.setThemeMode(ThemeMode.Light)
            assertEquals(ThemeMode.Light, awaitItem().themeMode)

            repository.setDyslexiaFriendly(enabled = true)
            assertTrue(awaitItem().dyslexiaFriendly)
        }
    }
}
