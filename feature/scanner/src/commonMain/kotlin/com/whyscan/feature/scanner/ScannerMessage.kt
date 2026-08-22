package com.whyscan.feature.scanner

/**
 * Lo que el ViewModel quiere contarle al usuario, **sin texto**.
 *
 * El ViewModel emitía cadenas en español directamente. Eso hacía dos cosas mal a la vez: ataba la
 * lógica al idioma —imposible traducir la app sin tocar el ViewModel— y obligaba a los tests a
 * afirmar sobre una frase, de modo que cambiar una coma rompía un test que no verificaba nada sobre
 * la coma.
 *
 * Ahora el ViewModel dice *qué pasó* y la pantalla decide cómo se llama, con `composeResources`.
 */
sealed interface ScannerMessage {

    /** La cadena de fallback degradó de motor. Es la señal visible del objetivo G4. */
    data object EngineSwitched : ScannerMessage

    data object CameraPermissionDenied : ScannerMessage

    data object ManualInputUnavailable : ScannerMessage

    data object Copied : ScannerMessage

    data object CopyFailed : ScannerMessage

    data object ShareFailed : ScannerMessage

    data object OpenFailed : ScannerMessage

    data object NoCodeInImage : ScannerMessage

    /**
     * La nota quedó guardada, o se quitó la que había.
     *
     * Son dos mensajes y no uno porque vaciar el campo es cómo se borra una nota, y un "Nota
     * guardada" tras haberla borrado diría lo contrario de lo que pasó. Son los mismos dos que usa
     * el historial: la acción es la misma en las dos pantallas y no tiene por qué contarse distinto.
     */
    data object NoteSaved : ScannerMessage

    data object NoteRemoved : ScannerMessage

    /**
     * Texto que ya viene resuelto de fuera: el motivo que da el selector de imágenes del sistema o
     * el mensaje de una excepción del decodificador.
     *
     * Es la única puerta que queda para texto sin traducir, y está abierta a propósito: son cadenas
     * que produce la plataforma, no la app, y sustituirlas por un mensaje genérico perdería la única
     * pista útil que tiene el usuario.
     */
    data class Raw(val text: String) : ScannerMessage
}
