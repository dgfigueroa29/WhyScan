package com.whyscan.feature.scanner

import com.whyscan.core.domain.model.EngineStatus
import com.whyscan.core.domain.scan.ResultAction
import com.whyscan.core.model.BarcodeFormat
import com.whyscan.core.model.Detection
import com.whyscan.core.model.ScanError
import com.whyscan.core.model.ScanSource
import com.whyscan.core.model.ScannerEngineId
import com.whyscan.core.scanner.EngineAvailability

/** Estado de la sesión de escaneo, tal y como lo ve la UI. */
enum class SessionStatus { Idle, Starting, Scanning, Finished }

/**
 * Estado de la pantalla de escaneo.
 *
 * [selectedEngineId] es lo que el usuario eligió; [activeEngineId] es el motor que está corriendo
 * de verdad. Pueden diferir cuando la cadena de fallback degrada, y la UI necesita ambos para
 * poder explicárselo al usuario en lugar de mostrar un error (G4).
 */
data class ScannerState(
    val isLoading: Boolean = true,
    val catalog: List<EngineStatus> = emptyList(),
    val selectedEngineId: ScannerEngineId? = null,
    val activeEngineId: ScannerEngineId? = null,
    val switchedFrom: ScannerEngineId? = null,
    val formats: Set<BarcodeFormat> = BarcodeFormat.all,
    val continuous: Boolean = false,
    val sessionStatus: SessionStatus = SessionStatus.Idle,
    val detections: List<Detection> = emptyList(),
    val manualInput: String = "",
    val torchEnabled: Boolean = false,
    val zoomRatio: Float = 1f,
    /** Si el sistema ofrece hoja de compartir; en escritorio no la hay. */
    val canShare: Boolean = false,
    /** Hay una imagen decodificándose (RF-07). Puede tardar: bloquea el botón y muestra progreso. */
    val isDecodingImage: Boolean = false,
    /**
     * Las notas del historial, por id de lectura. Solo están las que **tienen** nota.
     *
     * La pantalla de escaneo las necesita por una razón concreta y no por completitud: sin ellas, el
     * campo de anotar se abriría vacío sobre un código que ya estaba anotado, y guardar borraría lo
     * que hubiera. El escáner no guarda notas en su propio estado — las lee de donde viven.
     */
    val notes: Map<String, String> = emptyMap(),
    /** La lectura cuyo campo de nota está abierto, si hay alguno. */
    val noteTargetId: String? = null,
    /** Lo que hay escrito en ese campo ahora mismo. */
    val noteDraft: String = "",
    /**
     * El visor está ocupando la pantalla entera para probar un motor concreto (`Probar ahora`).
     *
     * Es estado y no un parámetro de la UI porque quien lo enciende es una acción —elegir el motor y
     * arrancar la sesión ocurren con él— y quien lo apaga puede ser el usuario **o** el propio
     * ciclo de vida: salir de la pantalla lo cierra, para que volver devuelva el banco de motores
     * donde estaba y no un visor a pantalla completa sin contexto.
     */
    val fullScreenPreview: Boolean = false,
    val error: ScanError? = null,
) {
    val usableEngines: List<EngineStatus> get() = catalog.filter { it.isUsable }

    /**
     * Escanear desde imagen se ofrece solo si algún motor sabe hacerlo. Es la misma regla que
     * oculta la linterna: la UI no nombra motores, lee capacidades.
     *
     * La condición es `canDecodeImages` y no `isUsable` porque un motor al que le falta el permiso
     * de cámara sigue sabiendo leer un archivo — y ese es justo el momento en que la foto es la
     * única salida. La regla vive en el dominio para que el selector aplique la misma.
     */
    val canScanFromImage: Boolean get() = catalog.any { it.canDecodeImages }

    /** La entrada manual se muestra solo si el motor activo se alimenta de texto. */
    val isManualEntryActive: Boolean get() = activeEngineId == ScannerEngineId.ManualInput

    private val activeCapabilities
        get() =
            catalog.firstOrNull { it.id == activeEngineId }?.descriptor?.capabilities

    /**
     * Los controles de cámara se derivan de las capacidades declaradas, no de una lista de motores
     * que los tengan. Por eso el Google Code Scanner esconde la linterna sin que la UI lo nombre.
     */
    val canControlTorch: Boolean get() = activeCapabilities?.supportsTorch == true

    val canControlZoom: Boolean get() = activeCapabilities?.supportsZoom == true

    /** Motores que el usuario puede desbloquear concediendo un permiso o descargando un modelo. */
    val actionableEngines: List<EngineStatus>
        get() = catalog.filter { it.installed && it.availability.isActionable }

    /**
     * Hay motores de cámara instalados esperando **solo** a que se conceda un permiso.
     *
     * Se distingue de [actionableEngines] —que incluye los que esperan una descarga— porque la
     * pantalla hace cosas distintas con cada caso: el permiso es una pregunta al usuario y merece
     * ocupar el visor entero con su explicación; una descarga pendiente no le impide escanear con
     * otro motor.
     */
    val needsCameraPermission: Boolean
        get() = catalog.any { it.installed && it.availability is EngineAvailability.RequiresPermission }

    /**
     * Algún motor disponible sabe leer de la cámara en vivo.
     *
     * Es lo que separa "todavía no se ha arrancado" de "aquí no hay cámara y no la va a haber",
     * que es el estado permanente del escritorio: hay decodificador de archivos y entrada manual,
     * pero ninguna captura de webcam. Sin esta distinción, la pantalla mostraría eternamente un
     * visor negro esperando algo que no puede pasar.
     */
    val hasLiveCameraEngine: Boolean
        get() = catalog.any { it.isUsable && ScanSource.LiveCamera in it.descriptor.capabilities.sources }

    /** La lectura más reciente, que es la que la hoja de resultados destaca. */
    val latestDetection: Detection? get() = detections.firstOrNull()

    /**
     * Cómo se llama el motor que está leyendo, tal y como se le enseña a una persona.
     *
     * Sale del descriptor y no de una tabla en la UI, que es la misma regla de siempre: la pantalla
     * no conoce ningún motor. El id —`mlkit-camerax`— no vale aquí: la pantalla completa la abre
     * quien está probando un motor y quiere leer el nombre que vio en la ficha.
     */
    val activeEngineName: String?
        get() = catalog.firstOrNull { it.id == activeEngineId }?.descriptor?.displayName

    /** La nota que ya tiene una lectura, o `null` si no tiene. */
    fun noteOf(detectionId: String): String? = notes[detectionId]
}

sealed interface ScannerAction {
    data object Refresh : ScannerAction

    /**
     * La pantalla apareció. Refresca el catálogo y **arranca la sesión sola** si se puede.
     *
     * Que el usuario tenga que pulsar "Escanear" para que un escáner escanee es una fricción que no
     * se gana nada: abrió la app de leer códigos. Lo que sí se pregunta antes es el permiso, y por
     * eso el arranque automático no lo dispara — pedirlo sin que el usuario haya hecho nada es la
     * forma más rápida de que lo deniegue para siempre.
     */
    data object ScreenShown : ScannerAction

    /**
     * La pantalla dejó de verse: apaga la cámara.
     *
     * El ViewModel sobrevive a la navegación, así que sin esto la cámara seguía capturando mientras
     * el usuario mira el historial o los ajustes. Es batería, y sobre todo es una app de escaneo
     * grabando cuando nadie se lo pidió.
     *
     * **Es navegación, no ciclo de vida**, y confundir las dos cosas costó un defecto que dejaba al
     * usuario encerrado: ver [Backgrounded].
     */
    data object ScreenHidden : ScannerAction

    /**
     * La app se fue a segundo plano. **No es lo mismo que salir de la pantalla**, y tratarlo como si
     * lo fuera fue exactamente el defecto.
     *
     * La cámara se apaga al irse al fondo —batería, y una app de escaneo no graba cuando nadie la
     * mira— y se reanuda al volver, **si estaba corriendo**. Lo que no se hace es re-armar el
     * arranque automático: ese es un privilegio de *llegar a la pantalla*, no de *volver al primer
     * plano*.
     *
     * ### El defecto que esto cierra
     *
     * El Google Code Scanner abre **su propia pantalla, en otro proceso**, así que arrancar la
     * sesión manda a WhyScan al fondo. Con el arranque automático atado al primer plano, la
     * secuencia era circular: el motor abre su pantalla → WhyScan al fondo → el usuario cierra esa
     * pantalla → WhyScan al primer plano → arranca la sesión → el motor abre su pantalla otra vez.
     * **El usuario no podía salir**, ni con la X ni con atrás ni con el gesto, y daba igual que la
     * lectura hubiera funcionado.
     */
    data object Backgrounded : ScannerAction

    /** La app volvió al primer plano. Ver [Backgrounded]. */
    data object Foregrounded : ScannerAction

    /** Vaciar los resultados en pantalla. El historial no se toca: eso se borra desde su pantalla. */
    data object ClearDetections : ScannerAction
    data object StartSession : ScannerAction
    data object StopSession : ScannerAction
    data class SelectEngine(val id: ScannerEngineId?) : ScannerAction

    /**
     * Probar un motor **ahora**: lo elige, reinicia la sesión con él y abre el visor a pantalla
     * completa.
     *
     * Es lo que [SelectEngine] no hace y nunca prometió hacer: guardar la preferencia y devolver al
     * usuario a una lista de fichas donde, a la vista, no cambia nada. La pregunta que se hace
     * delante del catálogo es "¿qué tal lee **este**?", y esa solo la contesta la cámara abierta.
     */
    data class TryEngine(val id: ScannerEngineId) : ScannerAction

    /**
     * Cerrar el visor a pantalla completa.
     *
     * **No para la sesión**: se vuelve al banco de motores con la cámara viva y el motor probado ya
     * elegido, que es justo el estado en el que uno quiere seguir mirando las fichas.
     */
    data object CloseFullScreen : ScannerAction
    data class ToggleFormat(val format: BarcodeFormat) : ScannerAction
    data class SetContinuous(val enabled: Boolean) : ScannerAction
    data class ManualInputChanged(val value: String) : ScannerAction
    data object SubmitManualInput : ScannerAction

    /** Elegir una imagen del dispositivo y decodificarla (RF-07). */
    data object ScanFromImage : ScannerAction

    /**
     * Ejecutar una acción sobre un resultado (RF-13).
     *
     * Lleva el texto ya redactado porque redactarlo es cosa de la pantalla: el dominio dice qué
     * datos son relevantes (`ShareableContent`) y la UI los pasa por sus recursos traducibles.
     */
    data class RunResultAction(val action: ResultAction, val text: String) : ScannerAction
    data object ToggleTorch : ScannerAction
    data class SetZoom(val ratio: Float) : ScannerAction
    data object RequestCameraPermission : ScannerAction
    data object DismissError : ScannerAction

    /**
     * Abrir el campo de nota sobre una lectura recién hecha.
     *
     * Anotar ya se podía desde el historial, y aun así hacía falta aquí: **el momento en que uno
     * sabe para qué es un código es justo cuando lo acaba de leer**. Obligar a terminar de escanear,
     * cambiar de pantalla y reconocer la lectura entre las demás es pedirle al usuario que recuerde
     * lo que sabía hace diez segundos.
     */
    data class EditNote(val detectionId: String) : ScannerAction

    data class NoteDraftChanged(val value: String) : ScannerAction

    /** Guardar lo escrito. Con el campo vacío borra la nota, que es como se quita una. */
    data object SaveNote : ScannerAction

    /** Cerrar el campo sin guardar. */
    data object DismissNote : ScannerAction
}

/**
 * Eventos de una sola vez. No forman parte del estado: no deben re-emitirse al recomponer.
 *
 * Un solo caso basta: abrir un enlace no es un efecto de la UI sino una acción de plataforma que
 * ejecuta `PlatformActions` (RF-13), y lo único que vuelve aquí es si hay algo que contar.
 */
sealed interface ScannerEffect {
    data class ShowMessage(val message: ScannerMessage) : ScannerEffect
}
