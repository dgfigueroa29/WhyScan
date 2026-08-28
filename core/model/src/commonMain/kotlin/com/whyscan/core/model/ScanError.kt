package com.whyscan.core.model

/**
 * Errores del dominio de escaneo.
 *
 * Se modelan como jerarquía sellada y nunca como excepciones crudas: un fallo de escaneo es un
 * resultado esperado del negocio, no una condición excepcional del programa.
 *
 * [isFatal] es lo que distingue "este frame no se pudo analizar" de "esta sesión no puede
 * continuar". La cadena de fallback solo cambia de motor ante errores fatales; un fallo transitorio
 * no debe apagar la cámara ni degradar a un motor peor.
 *
 * [allowsFallback] es la segunda mitad de esa pregunta, y **no es la misma**: ver su documentación.
 *
 * @param allowsFallback si el fallo justifica **degradar al motor siguiente**.
 *
 *   Confundirlo con [isFatal] costó un defecto que atrapaba al usuario dentro de la app. El Google
 *   Code Scanner encabeza la cadena en Android y abre su propia pantalla a pantalla completa; al
 *   cerrarla con el botón atrás emite [Cancelled], que es fatal —esa sesión, desde luego, no puede
 *   continuar— y la cadena hacía con él lo que hace con cualquier fallo fatal: pasar al motor
 *   siguiente y **volver a abrir la cámara**. Cerrar la cámara la hacía aparecer otra vez, una y
 *   otra vez.
 *
 *   Cancelar no es una avería del motor: es el usuario diciendo que no quiere seguir, y la única
 *   respuesta correcta a eso es dejar de escanear. Por defecto es `true` porque **todos los demás
 *   errores sí son averías**, que es exactamente para lo que la cadena existe (G4).
 */
sealed class ScanError(val isFatal: Boolean, val allowsFallback: Boolean = true) {

    /** El usuario denegó el permiso de cámara. */
    data class PermissionDenied(val permission: Permission) : ScanError(isFatal = true)

    /** No hay cámara, está ocupada por otra app, o el SO la revocó. */
    data class CameraUnavailable(val reason: String) : ScanError(isFatal = true)

    /**
     * El motor no puede arrancar: SDK ausente, modelo no descargado, API no soportada.
     * [engineId] es `null` cuando ni siquiera hubo un motor al que preguntar.
     */
    data class EngineUnavailable(val engineId: ScannerEngineId?, val reason: String) :
        ScanError(isFatal = true)

    /** Un frame o una imagen no se pudo decodificar. Transitorio por naturaleza. */
    data class DecodeFailed(val reason: String) : ScanError(isFatal = false)

    /** El formato leído no está entre los solicitados. Transitorio: el siguiente frame puede servir. */
    data class FormatRejected(val format: BarcodeFormat) : ScanError(isFatal = false)

    /** Se agotó [ScanRequest.timeoutMillis] sin detección. */
    data object Timeout : ScanError(isFatal = true)

    /**
     * El usuario canceló la sesión — hoy, cerrando la pantalla del Google Code Scanner.
     *
     * Fatal y **sin degradación**: la sesión termina aquí y la cadena no prueba el motor siguiente.
     * Ver el KDoc de [allowsFallback].
     */
    data object Cancelled : ScanError(isFatal = true, allowsFallback = false)

    data class Unexpected(val message: String, val cause: Throwable? = null) :
        ScanError(isFatal = true)
}

/** Permisos del sistema que el escaneo puede necesitar. */
enum class Permission(val displayName: String) {
    Camera("Cámara"),
    PhotoLibrary("Fotos"),
}
