package com.whyscan.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whyscan.core.domain.concurrency.launchCatching
import com.whyscan.core.domain.repository.ScanPreferences
import com.whyscan.core.domain.repository.ScannerEngineRepository
import com.whyscan.core.domain.scan.ResultAction
import com.whyscan.core.domain.usecase.ScanHistory
import com.whyscan.core.domain.usecase.ScanSessions
import com.whyscan.core.domain.usecase.ScanSettings
import com.whyscan.core.model.BarcodeFormat
import com.whyscan.core.model.Permission
import com.whyscan.core.model.ScanImage
import com.whyscan.core.model.ScannerEngineId
import com.whyscan.core.permissions.PermissionController
import com.whyscan.core.platform.ImagePicker
import com.whyscan.core.platform.PickImageResult
import com.whyscan.core.scanner.CameraControlEngine
import com.whyscan.core.scanner.ScanEvent
import com.whyscan.core.scanner.TextInputEngine
import com.whyscan.core.scanner.capability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * MVI de la pantalla de escaneo.
 *
 * El ViewModel **no conoce ningún motor concreto**: pide una sesión a [ScanSessions] y reacciona a
 * los [ScanEvent] que llegan. Toda la lógica de selección y degradación vive en el dominio, así
 * que añadir un motor nuevo no toca este archivo (RNF-07).
 *
 * ### Por qué sigue teniendo supresiones
 * La deuda D16 se saldó agrupando: los ajustes en [ScanSettings], la sesión y el guardado en
 * [ScanSessions], y las acciones sobre el resultado en [ResultActionRunner]. De **doce
 * dependencias quedan siete**, y `LongParameterList` ya no hace falta silenciarla.
 *
 * La séptima es [ScanHistory], y entró con las notas. No contradice que guardar sea de
 * [ScanSessions]: guardar una lectura es un hecho del motor y anotarla es una acción del usuario,
 * que es exactamente la línea por la que están separados esos dos colaboradores.
 *
 * `TooManyFunctions` sobrevive, y es un dato honesto: esta pantalla tiene veintitrés acciones de
 * usuario y cada una necesita su función. Partirla por partir movería el recuento a otro archivo
 * sin que nadie entienda mejor la pantalla. La supresión se pone aquí, a la vista, y no subiendo el
 * umbral global —que dejaría la regla midiendo siempre lo que hubiera.
 */
@Suppress("TooManyFunctions")
class ScannerViewModel(
    private val settings: ScanSettings,
    private val sessions: ScanSessions,
    private val history: ScanHistory,
    private val engineRepository: ScannerEngineRepository,
    private val permissionController: PermissionController,
    private val imagePicker: ImagePicker,
    private val resultActions: ResultActionRunner,
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerState(canShare = resultActions.canShare))
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ScannerEffect>()
    val effects: SharedFlow<ScannerEffect> = _effects.asSharedFlow()

    private var sessionJob: Job? = null

    /**
     * Hay un arranque automático esperando a que el catálogo diga si se puede. Ver [startIfPending].
     */
    private var autoStartPending: Boolean = false

    /**
     * La sesión estaba corriendo cuando la app se fue al fondo, así que hay que devolverla al volver.
     *
     * Es distinto de [autoStartPending] y esa distinción **es** el arreglo del defecto de
     * navegación: uno responde a "el usuario llegó a esta pantalla" y el otro a "la app volvió al
     * primer plano". Ver `ScannerAction.Backgrounded`.
     */
    private var resumeOnForeground: Boolean = false

    init {
        observeCatalogChanges()
        observePreferenceChanges()
        observeNotes()
    }

    /**
     * `CyclomaticComplexMethod` cuenta veintitrés ramas y tiene razón en el número, no en lo que
     * significa: es una tabla de despacho sobre un `sealed interface`, donde cada rama es una línea
     * y el compilador exige que estén todas. Partirla en dos `when` la haría peor de leer y bajaría
     * la métrica, que es justo la clase de arreglo que no sirve para nada.
     *
     * Se suprime aquí y no con `ignoreSimpleWhenEntries` en la configuración: esa opción dejaría de
     * contar los `when` de una línea en todo el proyecto, incluidos los que sí esconden complejidad.
     */
    @Suppress("CyclomaticComplexMethod")
    fun onAction(action: ScannerAction) {
        when (action) {
            ScannerAction.Refresh -> refresh()
            ScannerAction.ScreenShown -> screenShown()
            ScannerAction.ScreenHidden -> screenHidden()
            ScannerAction.Backgrounded -> backgrounded()
            ScannerAction.Foregrounded -> foregrounded()
            ScannerAction.ClearDetections -> _state.update { it.copy(detections = emptyList()) }
            ScannerAction.StartSession -> startSession()
            ScannerAction.StopSession -> stopSession()
            is ScannerAction.SelectEngine -> selectEngine(action.id)
            is ScannerAction.TryEngine -> tryEngine(action.id)
            ScannerAction.CloseFullScreen -> _state.update { it.copy(fullScreenPreview = false) }
            is ScannerAction.ToggleFormat -> toggleFormat(action.format)
            is ScannerAction.SetContinuous -> setContinuous(action.enabled)
            is ScannerAction.ManualInputChanged -> _state.update { it.copy(manualInput = action.value) }
            ScannerAction.SubmitManualInput -> submitManualInput()
            ScannerAction.ScanFromImage -> scanFromImage()
            ScannerAction.UseManualEntry -> useManualEntry()
            is ScannerAction.RunResultAction -> runResultAction(action.action, action.text)
            ScannerAction.ToggleTorch -> toggleTorch()
            is ScannerAction.SetZoom -> setZoom(action.ratio)
            ScannerAction.RequestCameraPermission -> requestCameraPermission()
            ScannerAction.DismissError -> _state.update { it.copy(error = null) }
            is ScannerAction.EditNote -> editNote(action.detectionId)
            is ScannerAction.NoteDraftChanged -> _state.update { it.copy(noteDraft = action.value) }
            ScannerAction.SaveNote -> saveNote()
            ScannerAction.DismissNote -> dismissNote()
        }
    }

    /**
     * Como `viewModelScope.launch`, pero un fallo se le cuenta al usuario en vez de matar la app.
     *
     * Debajo de esta pantalla hay disco por todas partes: el historial guarda cada lectura, los
     * ajustes se persisten al tocarlos y las notas se leen de la base. Antes, una excepción de Room
     * —disco lleno, base corrupta— subía por el `launch` hasta el manejador por defecto del hilo y
     * cerraba el proceso **en mitad de un escaneo**. Ver `launchCatching`.
     *
     * Se usa en todos los `launch` de esta clase, incluido el de la sesión: que un motor de cámara
     * lance no debería llevarse la app por delante, y aquí el `sessionJob` sigue siendo el `Job` que
     * devuelve, así que parar y reanudar funciona igual.
     */
    private fun launchSafely(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launchCatching(
            onFailure = { _effects.emit(ScannerEffect.ShowMessage(ScannerMessage.OperationFailed)) },
            block = block,
        )

    private fun observeCatalogChanges() {
        launchSafely {
            engineRepository.observeCatalog().collect { catalog ->
                _state.update { it.copy(catalog = catalog, isLoading = false) }
                startIfPending()
            }
        }
    }

    /**
     * El arranque automático se resuelve **aquí** y no dentro de [screenShown], y no es un detalle.
     *
     * `refresh()` actualiza el catálogo publicando en un `Flow` que se colecta en otra corrutina, así
     * que cuando `refresh()` devuelve el estado todavía puede tener la disponibilidad vieja. Decidir
     * ahí si arrancar era una carrera: en un arranque en frío el catálogo aún estaba vacío, el
     * `hasLiveCameraEngine` daba `false` y la cámara no se abría nunca. Aquí llega ya resuelto.
     */
    private fun startIfPending() {
        if (!autoStartPending) return

        val state = _state.value
        // Sin permiso no se arranca: la pantalla enseña la explicación y el botón. Pedirlo sin que
        // el usuario haya tocado nada es la forma más rápida de que lo deniegue para siempre.
        if (state.needsCameraPermission || !state.hasLiveCameraEngine) return
        if (state.sessionStatus == SessionStatus.Scanning || state.sessionStatus == SessionStatus.Starting) {
            return
        }

        autoStartPending = false
        startSession()
    }

    private fun screenShown() {
        autoStartPending = true

        // Se intenta **ya** y además se refresca, y hacen falta las dos cosas:
        //
        //  - Volver a la pantalla con el catálogo ya cargado no produce ninguna emisión nueva —el
        //    `StateFlow` no reemite un valor igual—, así que esperar a `observeCatalogChanges` para
        //    arrancar dejaba la cámara apagada para siempre a partir de la segunda visita.
        //  - En un arranque en frío el catálogo todavía está vacío y aquí no se puede decidir nada;
        //    lo resuelve la emisión que llega después.
        startIfPending()
        refresh()
    }

    /**
     * La pantalla dejó de verse: se apaga la cámara y se cierra la pantalla completa.
     *
     * Lo segundo no es limpieza por gusto. El ViewModel sobrevive a la navegación, así que sin esto
     * quien saliera al historial con el visor a pantalla completa abierto volvería a encontrárselo
     * —sin cámara detrás, porque la sesión sí se paró— tapando el banco de motores que venía a ver.
     */
    private fun screenHidden() {
        // Salir de la pantalla no es irse al fondo: aquí no queda nada que reanudar después. Y el
        // orden importa — al desmontarse la composición se dispara antes `Backgrounded`, que sí deja
        // la marca puesta.
        resumeOnForeground = false
        _state.update { it.copy(fullScreenPreview = false) }
        stopSession()
    }

    /**
     * La app se fue al fondo.
     *
     * **Salvo que el motor activo abra su propia pantalla.** Ahí, estar en segundo plano no
     * significa que el usuario se haya ido: significa que el motor está trabajando, en otro proceso,
     * porque lo hemos arrancado nosotros. Parar la sesión cancelaría el escaneo que el usuario está
     * haciendo **ahora mismo**, y su resultado se perdería en una corrutina ya cancelada.
     *
     * Del resto de motores sí se apaga la cámara, y se apunta que estaba corriendo para devolverla
     * al volver. Si el usuario la había pausado a mano, no se apunta nada: reanudar algo que él
     * apagó es contestarle que no.
     */
    private fun backgrounded() {
        if (activeEngineProvidesOwnUi) return

        val running = _state.value.sessionStatus == SessionStatus.Scanning ||
            _state.value.sessionStatus == SessionStatus.Starting
        if (!running) return

        resumeOnForeground = true
        stopSession()
    }

    /** La app volvió al primer plano: se devuelve la cámara **solo** si la habíamos quitado nosotros. */
    private fun foregrounded() {
        if (!resumeOnForeground) return

        resumeOnForeground = false
        startSession()
    }

    /**
     * El motor que está corriendo abre su propia pantalla fuera de la app.
     *
     * Se lee de la capacidad declarada y no de una lista de motores: hoy es el Google Code Scanner,
     * y el día que haya otro hereda el comportamiento sin tocar esta clase (RNF-07).
     */
    private val activeEngineProvidesOwnUi: Boolean
        get() = _state.value.let { state ->
            state.catalog
                .firstOrNull { it.id == state.activeEngineId }
                ?.descriptor?.capabilities?.providesOwnUi == true
        }

    private fun observePreferenceChanges() {
        launchSafely {
            settings.observe().collect { preferences ->
                _state.update {
                    it.copy(
                        selectedEngineId = preferences.preferredEngineId,
                        formats = preferences.formats,
                        continuous = preferences.continuous,
                    )
                }
            }
        }
    }

    /**
     * Las notas del historial, para que el escáner pueda mostrarlas y editarlas sin inventárselas.
     *
     * Se observa el historial entero y se reduce al mapa de las que tienen nota, con
     * `distinctUntilChanged` **sobre el mapa ya reducido**: el historial emite en cada lectura
     * guardada —treinta veces en una sesión continua de un minuto— y sin ese filtro cada una
     * recompondría la hoja de resultados sin que ninguna nota hubiera cambiado.
     *
     * La alternativa era guardarlas en el estado del escáner según se escriben. Se descartó porque
     * abre un agujero real: al releer un código ya anotado el id es el mismo, el campo se abriría
     * vacío y guardar se llevaría por delante la nota que había.
     */
    private fun observeNotes() {
        launchSafely {
            history.observe()
                .map { entries ->
                    entries.mapNotNull { entry -> entry.note?.let { entry.id to it } }.toMap()
                }
                .distinctUntilChanged()
                .collect { notes -> _state.update { it.copy(notes = notes) } }
        }
    }

    private fun refresh() {
        launchSafely { engineRepository.refresh() }
    }

    /** Abre el campo con lo que la lectura ya tuviera escrito, para editarlo y no para sustituirlo. */
    private fun editNote(detectionId: String) {
        _state.update {
            it.copy(noteTargetId = detectionId, noteDraft = it.noteOf(detectionId).orEmpty())
        }
    }

    private fun dismissNote() {
        _state.update { it.copy(noteTargetId = null, noteDraft = "") }
    }

    /**
     * Guarda la nota y cierra el campo.
     *
     * No rechaza el texto vacío: vaciar el campo **es** cómo se quita una nota, y
     * [ScanHistory.setNote] normaliza los blancos a `null` en un solo sitio para las tres
     * plataformas. El estado del escáner no toca su propio mapa de notas — lo actualiza la emisión
     * del historial, que es la única fuente.
     */
    private fun saveNote() {
        val detectionId = _state.value.noteTargetId ?: return
        val note = _state.value.noteDraft

        dismissNote()
        launchSafely {
            history.setNote(detectionId, note)
            val message = if (note.isBlank()) ScannerMessage.NoteRemoved else ScannerMessage.NoteSaved
            _effects.emit(ScannerEffect.ShowMessage(message))
        }
    }

    private fun selectEngine(id: ScannerEngineId?) {
        launchSafely {
            settings.preferEngine(id)
            if (_state.value.sessionStatus == SessionStatus.Scanning) {
                stopSession()
                startSession()
            }
        }
    }

    /**
     * Probar un motor en el acto: se elige, se reinicia la sesión con él y el visor se va a pantalla
     * completa.
     *
     * La sesión se reinicia **siempre**, y ahí está la diferencia con [selectEngine], que solo lo
     * hace si ya estaba escaneando. Aquí no hay ambigüedad posible: el usuario acaba de pedir ver a
     * este motor trabajando, así que dejarlo en pausa sería contestar que no a lo que pulsó.
     */
    private fun tryEngine(id: ScannerEngineId) {
        launchSafely {
            settings.preferEngine(id)
            _state.update { it.copy(fullScreenPreview = true) }
            stopSession()
            startSession()
        }
    }

    private fun toggleFormat(format: BarcodeFormat) {
        launchSafely {
            val current = _state.value.formats
            settings.setFormats(if (format in current) current - format else current + format)
        }
    }

    private fun setContinuous(enabled: Boolean) {
        launchSafely { settings.setContinuous(enabled) }
    }

    /**
     * Arranca una sesión escribiendo el código a mano, **sin tocar las preferencias** (G4).
     *
     * El motor preferido se sustituye solo para esta sesión copiando las preferencias en memoria.
     * Así `ScanSessions.sourceFor()` pone la fuente en `ManualInput` —que es lo que hace que el
     * selector no descarte el motor manual— y `settings.preferEngine` no se llama en ningún
     * momento. Ver el KDoc de [ScannerAction.UseManualEntry].
     */
    private fun useManualEntry() {
        autoStartPending = false
        startSession { it.copy(preferredEngineId = ScannerEngineId.ManualInput) }
    }

    /**
     * @param adjust cambia las preferencias **solo para esta sesión**. Por defecto no cambia nada,
     * que es el caso normal: la sesión usa lo que el usuario tenga guardado.
     */
    private fun startSession(adjust: (ScanPreferences) -> ScanPreferences = { it }) {
        sessionJob?.cancel()
        _state.update {
            it.copy(
                sessionStatus = SessionStatus.Starting,
                detections = emptyList(),
                switchedFrom = null,
                error = null,
            )
        }

        sessionJob = launchSafely {
            sessions.start(adjust(settings.current())).collect(::reduce)
        }
    }

    private fun stopSession() {
        // Parar es también renunciar a un arranque pendiente: si no, volver de los ajustes
        // reabriría la cámara que el usuario acaba de cerrar.
        autoStartPending = false
        sessionJob?.cancel()
        sessionJob = null
        _state.update {
            it.copy(
                sessionStatus = SessionStatus.Idle,
                activeEngineId = null,
                torchEnabled = false,
                zoomRatio = 1f,
            )
        }
    }

    private suspend fun reduce(event: ScanEvent) {
        when (event) {
            is ScanEvent.SessionStarted -> _state.update {
                it.copy(sessionStatus = SessionStatus.Scanning, activeEngineId = event.engineId)
            }

            is ScanEvent.Detected -> {
                sessions.save(event.detections)
                // Con tope: una sesión continua larga acumulaba resultados sin límite y la lista
                // crecía hasta donde aguantara la memoria. Lo que se recorta aquí no se pierde —el
                // historial lo guarda todo—, solo deja de ocupar RAM en una pantalla donde nadie va
                // a desplazarse cien lecturas hacia abajo.
                _state.update {
                    it.copy(detections = (event.detections + it.detections).take(MAX_VISIBLE_DETECTIONS))
                }
            }

            is ScanEvent.EngineSwitched -> {
                _state.update { it.copy(switchedFrom = event.from, activeEngineId = event.to) }
                _effects.emit(ScannerEffect.ShowMessage(ScannerMessage.EngineSwitched))
            }

            is ScanEvent.Failed -> if (event.error.isFatal) {
                _state.update { it.copy(error = event.error, sessionStatus = SessionStatus.Finished) }
            } else {
                _state.update { it.copy(error = event.error) }
            }

            is ScanEvent.SessionEnded -> _state.update {
                it.copy(
                    sessionStatus = SessionStatus.Finished,
                    activeEngineId = null,
                    // La pantalla completa existe para ver leer a un motor. Si la sesión terminó
                    // **sin nada que enseñar** —el usuario cerró la pantalla del Google Code
                    // Scanner, o se agotó el plazo— no queda nada que mirar, y dejarla abierta le
                    // pide un segundo atrás para salir de donde ya quiso salir. Con lecturas se
                    // queda: ahí sí hay algo que leer y qué hacer con ello.
                    fullScreenPreview = it.fullScreenPreview && it.detections.isNotEmpty(),
                )
            }

            is ScanEvent.FrameAnalyzed -> Unit
        }
    }

    private fun submitManualInput() {
        val value = _state.value.manualInput
        if (value.isBlank()) return

        launchSafely {
            val engine = engineRepository.engine(ScannerEngineId.ManualInput)
                ?.capability<TextInputEngine>()

            if (engine != null) {
                engine.submit(value)
                _state.update { it.copy(manualInput = "") }
            } else {
                _effects.emit(ScannerEffect.ShowMessage(ScannerMessage.ManualInputUnavailable))
            }
        }
    }

    /**
     * Escanea una imagen elegida por el usuario (RF-07).
     *
     * Detiene la sesión en vivo antes de abrir el selector: en Android la cámara y el selector del
     * sistema compiten por la pantalla, y dejarla corriendo detrás gastaría batería mientras el
     * usuario busca la foto.
     *
     * No pide permiso de galería a propósito. El selector moderno de cada plataforma —el *photo
     * picker* de Android, `PHPicker` en iOS, el diálogo de archivos en escritorio y web— corre
     * fuera de la app y devuelve solo lo que el usuario elige, así que **no hay nada que pedir**.
     * Es la misma ventaja que hace que el Google Code Scanner encabece la cadena en Android.
     */
    private fun scanFromImage() {
        if (_state.value.isDecodingImage) return

        stopSession()
        _state.update { it.copy(isDecodingImage = true, error = null) }

        launchSafely {
            try {
                when (val picked = imagePicker.pickImage()) {
                    is PickImageResult.Cancelled -> Unit

                    is PickImageResult.Failed ->
                        _effects.emit(ScannerEffect.ShowMessage(ScannerMessage.Raw(picked.reason)))

                    is PickImageResult.Picked -> decodePickedImage(picked.image)
                }
            } finally {
                _state.update { it.copy(isDecodingImage = false) }
            }
        }
    }

    private suspend fun decodePickedImage(image: ScanImage) {
        sessions.decode(image, settings.current())
            .onSuccess { detections ->
                if (detections.isEmpty()) {
                    _effects.emit(ScannerEffect.ShowMessage(ScannerMessage.NoCodeInImage))
                    return@onSuccess
                }
                // La imagen es una sesión puntual: sus resultados sustituyen a los anteriores, igual
                // que al arrancar una sesión de cámara.
                _state.update {
                    it.copy(
                        detections = detections,
                        activeEngineId = detections.first().engineId,
                        sessionStatus = SessionStatus.Finished,
                    )
                }
                sessions.save(detections)
            }
            .onFailure { failure ->
                val reason = failure.message?.let(ScannerMessage::Raw) ?: ScannerMessage.NoCodeInImage
                _effects.emit(ScannerEffect.ShowMessage(reason))
            }
    }

    private fun runResultAction(action: ResultAction, text: String) {
        launchSafely {
            resultActions.run(action, text)
                ?.let { _effects.emit(ScannerEffect.ShowMessage(it)) }
        }
    }

    /**
     * La linterna se pide a través de la capacidad opcional, no del motor concreto. Si el motor
     * activo no la implementa no pasa nada: la UI ya no muestra el control, y aquí el `as?` cierra
     * el caso sin excepciones.
     */
    private fun toggleTorch() {
        launchSafely {
            val control = cameraControlOfActiveEngine() ?: return@launchSafely

            val enabled = !_state.value.torchEnabled
            control.setTorch(enabled)
            _state.update { it.copy(torchEnabled = enabled) }
        }
    }

    private fun setZoom(ratio: Float) {
        launchSafely {
            val control = cameraControlOfActiveEngine() ?: return@launchSafely
            control.setZoomRatio(ratio)
            _state.update { it.copy(zoomRatio = ratio) }
        }
    }

    private fun cameraControlOfActiveEngine(): CameraControlEngine? =
        _state.value.activeEngineId
            ?.let(engineRepository::engine)
            ?.capability<CameraControlEngine>()

    /**
     * Tras conceder el permiso hay que refrescar el catálogo: la disponibilidad de los motores de
     * cámara cambia bajo los pies y el estado que la UI muestra quedaría obsoleto.
     */
    private fun requestCameraPermission() {
        launchSafely {
            val status = permissionController.request(Permission.Camera)
            engineRepository.refresh()

            if (!status.isGranted) {
                _effects.emit(ScannerEffect.ShowMessage(ScannerMessage.CameraPermissionDenied))
            }
        }
    }

    override fun onCleared() {
        sessionJob?.cancel()
        super.onCleared()
    }

    private companion object {
        /**
         * Cuántas lecturas se conservan en pantalla. El historial no tiene este tope: ahí sí se
         * guarda todo, porque su razón de ser es justamente conservarlo.
         */
        const val MAX_VISIBLE_DETECTIONS = 100
    }
}
