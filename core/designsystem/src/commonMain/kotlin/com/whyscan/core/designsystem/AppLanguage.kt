package com.whyscan.core.designsystem

import androidx.compose.runtime.Composable
import ar.net.faro.foundation.PlatformSupportsLanguageOverride
import ar.net.faro.foundation.ProvideAppLanguage

/**
 * Reexportación de compatibilidad — la implementación vive en :core:foundation.
 */
@Deprecated(
    message = "Usar ar.net.faro.foundation.ProvideAppLanguage directamente.",
    replaceWith = ReplaceWith("ProvideAppLanguage(tag, content)", "ar.net.faro.foundation.ProvideAppLanguage"),
    level = DeprecationLevel.WARNING,
)
@Composable
public fun ProvideAppLanguage(tag: String?, content: @Composable () -> Unit) {
    ProvideAppLanguage(tag, content)
}

/**
 * Reexportación de compatibilidad — la implementación vive en :core:foundation.
 */
@Deprecated(
    message = "Usar ar.net.faro.foundation.PlatformSupportsLanguageOverride directamente.",
    replaceWith = ReplaceWith(
        "PlatformSupportsLanguageOverride",
        "ar.net.faro.foundation.PlatformSupportsLanguageOverride"
    ),
    level = DeprecationLevel.WARNING,
)
public val PlatformSupportsLanguageOverride: Boolean
    get() = PlatformSupportsLanguageOverride
