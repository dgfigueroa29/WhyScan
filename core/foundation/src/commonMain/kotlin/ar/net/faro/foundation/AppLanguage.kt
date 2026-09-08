package ar.net.faro.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

/**
 * Mecanismo de cambio de idioma de la app por encima del idioma del sistema.
 *
 * `tag` es un BCP-47 como `"es"` o `"en"`. `null` significa "seguir al sistema". Cada plataforma
 * implementa [ApplyPlatformLanguage] de forma diferente:
 *
 * - Android: `Locale.setDefault` (Compose lo propaga vía `LocalLocale`).
 * - iOS: `NSUserDefaults["AppleLanguages"]`.
 * - JVM/Desktop: `Locale.setDefault`.
 * - wasmJs/Web: no-op; el idioma sale de `navigator.language`, que la página no puede escribir.
 *
 * [PlatformSupportsLanguageOverride] es `false` solo en wasmJs. La pantalla de Ajustes lo lee para
 * decidir si muestra el selector.
 *
 * Ver ADR-0011.
 */
@Composable
public fun ProvideAppLanguage(tag: String?, content: @Composable () -> Unit) {
    ApplyPlatformLanguage(tag)
    key(tag) { content() }
}

@Composable
internal expect fun ApplyPlatformLanguage(tag: String?)

public expect val PlatformSupportsLanguageOverride: Boolean
