package com.whyscan.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whyscan.core.domain.concurrency.launchCatching
import com.whyscan.core.domain.repository.AppLanguage
import com.whyscan.core.domain.repository.AppPreferencesRepository
import com.whyscan.core.domain.repository.ThemeMode
import com.whyscan.core.platform.PlatformActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Ajustes de la app: aspecto, accesibilidad, idioma y modo avanzado.
 *
 * **Ningún ajuste avisa de que se guardó, y no es un olvido**: aquí cada cambio *es* su propio
 * feedback, porque el tema y el idioma se ven en la propia pantalla en cuanto se tocan. Un aviso de
 * "guardado" sobre algo que ya cambió delante del usuario es ruido.
 *
 * El canal de efectos existe **solo** por lo que sí puede fallar en silencio: abrir la política de
 * privacidad o los términos de uso sale de la app, y si no hay quien los abra no pasa nada y nadie
 * sabe por qué. Ver [SettingsEffect].
 *
 * [canChooseLanguage] llega por constructor y no se consulta aquí dentro para que el ViewModel se
 * pueda testear en `commonTest` con los dos valores, que es donde vive la única lógica que tiene.
 */
class SettingsViewModel(
    private val preferences: AppPreferencesRepository,
    private val platformActions: PlatformActions,
    canChooseLanguage: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(canChooseLanguage = canChooseLanguage))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>()
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    init {
        launchSafely {
            preferences.observePreferences().collect { current ->
                _state.update { it.copy(preferences = current, isLoading = false) }
            }
        }
    }

    /**
     * Como `viewModelScope.launch`, pero un fallo al persistir no mata la app.
     *
     * **El `onFailure` está vacío a propósito, y esta vez no es tragarse el error.** El estado de
     * esta pantalla sale del repositorio y no de un eco local: el repositorio actualiza su flujo
     * **después** de escribir, así que si la escritura falla el estado no cambia y el interruptor
     * vuelve solo a donde estaba. El usuario ve que no se guardó, que es exactamente lo que un
     * mensaje le diría, y lo ve en el sitio donde acaba de tocar.
     *
     * Es la misma razón por la que esta pantalla no tiene canal de efectos: aquí cada cambio **es**
     * su propio feedback. Lo que se arregla con esto es lo otro — que la excepción subiera hasta el
     * manejador del hilo y cerrara el proceso por no haber podido escribir un booleano.
     */
    private fun launchSafely(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launchCatching(onFailure = { }, block = block)

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> setThemeMode(action.mode)
            is SettingsAction.SetLanguage -> setLanguage(action.language)
            is SettingsAction.SetAdvancedMode -> setAdvancedMode(action.enabled)
            is SettingsAction.SetDyslexiaFriendly -> setDyslexiaFriendly(action.enabled)
            is SettingsAction.OpenLink -> openLink(action.url)
        }
    }

    /**
     * Abre un documento legal fuera de la app.
     *
     * La dirección llega ya resuelta desde la pantalla —ver [SettingsAction.OpenLink]— y
     * `PlatformActions` la comprueba otra vez contra la lista blanca de esquemas antes de
     * entregársela al sistema. Que aquí solo lleguen direcciones nuestras es una propiedad del
     * grafo de llamadas, y esa comprobación existe justamente para no depender de eso.
     */
    private fun openLink(url: String) {
        launchSafely {
            if (!platformActions.openUrl(url)) {
                _effects.emit(SettingsEffect.ShowMessage(SettingsMessage.LinkFailed))
            }
        }
    }

    private fun setThemeMode(mode: ThemeMode) {
        launchSafely { preferences.setThemeMode(mode) }
    }

    /**
     * Cambiar el idioma en una plataforma que no puede honrarlo dejaría una preferencia guardada que
     * la app ignora: al reinstalar en otro dispositivo aparecería un idioma que el usuario no eligió
     * ahí. Se ignora el intento en lugar de persistir una mentira.
     */
    private fun setLanguage(language: AppLanguage) {
        if (!_state.value.canChooseLanguage) return
        launchSafely { preferences.setLanguage(language) }
    }

    private fun setAdvancedMode(enabled: Boolean) {
        launchSafely { preferences.setAdvancedMode(enabled) }
    }

    private fun setDyslexiaFriendly(enabled: Boolean) {
        launchSafely { preferences.setDyslexiaFriendly(enabled) }
    }
}
