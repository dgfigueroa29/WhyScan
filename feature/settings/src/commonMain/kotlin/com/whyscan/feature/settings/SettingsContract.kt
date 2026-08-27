package com.whyscan.feature.settings

import com.whyscan.core.domain.repository.AppLanguage
import com.whyscan.core.domain.repository.AppPreferences
import com.whyscan.core.domain.repository.ThemeMode

/**
 * Estado de la pantalla de Ajustes.
 *
 * Es un envoltorio fino sobre [AppPreferences] y no una copia de sus campos: duplicarlos obligaría a
 * añadir cada preferencia nueva en dos sitios, y el día que uno se olvidara la pantalla mostraría un
 * valor viejo sin que nada fallara.
 */
data class SettingsState(
    val preferences: AppPreferences = AppPreferences(),
    /**
     * Si esta plataforma puede honrar un idioma distinto al del sistema.
     *
     * En el navegador no puede, así que el selector no se dibuja. Preferimos no ofrecer el control
     * a ofrecerlo roto.
     */
    val canChooseLanguage: Boolean = true,
    val isLoading: Boolean = true,
)

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data class SetLanguage(val language: AppLanguage) : SettingsAction
    data class SetAdvancedMode(val enabled: Boolean) : SettingsAction
    data class SetDyslexiaFriendly(val enabled: Boolean) : SettingsAction

    /**
     * Abrir un documento legal —la política de privacidad o los términos de uso— fuera de la app.
     *
     * Lleva la dirección ya resuelta, por lo mismo que `ScannerAction.RunResultAction` lleva el
     * texto ya redactado: **las direcciones son un recurso traducible**, porque cada una apunta al
     * documento en el idioma que el usuario tiene puesto, y los recursos se resuelven donde hay
     * composición. El ViewModel no tiene por qué saber que existen dos idiomas.
     */
    data class OpenLink(val url: String) : SettingsAction
}

/**
 * Eventos de una sola vez.
 *
 * Esta pantalla no los tuvo durante mucho tiempo, y el motivo estaba escrito: aquí cada cambio *es*
 * su propio feedback, porque el tema y el idioma se ven en la propia pantalla en cuanto se tocan.
 * **Abrir un enlace rompió esa premisa**: es la primera acción de Ajustes cuyo resultado no se ve —
 * si no hay ninguna app que sepa abrir el documento, no pasa nada y nadie sabe por qué.
 */
sealed interface SettingsEffect {
    data class ShowMessage(val message: SettingsMessage) : SettingsEffect
}

/** Qué pasó, sin decidir con qué palabras se cuenta: eso es cosa de la pantalla. */
sealed interface SettingsMessage {
    data object LinkFailed : SettingsMessage
}
