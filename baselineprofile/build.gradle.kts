import com.android.build.api.dsl.ManagedVirtualDevice

/**
 * Generador del baseline profile de la app de Android.
 *
 * No es una librería ni entra en ningún binario: `com.android.test` produce un APK de test que
 * recorre la app instalada y anota qué métodos se ejecutan. El resultado —una lista de texto— se
 * copia a `androidApp/src/release/generated/baselineProfiles/` y viaja dentro del AAB, para que ART
 * compile ese camino de antemano en vez de interpretarlo el primer día.
 *
 * No usa los convention plugins de `build-logic/`: los tres son para módulos KMP o para la
 * aplicación, y este no es ninguna de las dos cosas.
 */
plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.baselineProfile)
}

/** Nombre del emulador declarado más abajo. Se nombra una vez y se usa en dos sitios. */
val managedDevice = "pixel6Api34"

/**
 * API 34 y no la 36 del `targetSdk`: la imagen de la 34 lleva años estable y el perfil no depende
 * del nivel de API, depende de por dónde pasa el código de la app.
 */
val managedDeviceApi = 34

/**
 * 28 y no el `minSdk` 24 de la app: generar el perfil exige `profman`, que llega en Android 9. Es el
 * mínimo del **generador**, no el de la app — el perfil resultante se instala igualmente desde la 24
 * gracias a `profileinstaller`.
 */
val generatorMinSdk = 28

android {
    namespace = "com.whyscan.baselineprofile"
    compileSdk = libs.versions.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = generatorMinSdk
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Contra qué app se genera.
    targetProjectPath = ":androidApp"

    // Un emulador declarado en la build, no uno que alguien tenga enchufado.
    //
    // Es la única forma de que esto sea reproducible: el perfil depende de por dónde pasa el código,
    // así que dos dispositivos distintos dan dos perfiles distintos y ninguno es "el" perfil. Con un
    // dispositivo gestionado, Gradle descarga la imagen, la arranca, mide y la apaga.
    //
    // `aosp` y no `google`: la imagen sin Play Services es más pequeña y arranca antes, y lo que
    // aquí se quiere medir es el arranque de **esta** app —Compose, Koin, Room— y no el de los
    // servicios de Google. El motor de Play (Google Code Scanner) se declarará no disponible en esa
    // imagen, exactamente igual que en un teléfono sin Play Services; la cadena de fallback existe
    // para eso y recorrerla también forma parte del arranque real.
    testOptions.managedDevices.allDevices {
        create<ManagedVirtualDevice>(managedDevice) {
            device = "Pixel 6"
            apiLevel = managedDeviceApi
            systemImageSource = "aosp"
        }
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

baselineProfile {
    managedDevices += managedDevice

    // Un dispositivo enchufado daría un perfil distinto según quién lance la tarea. Si alguien
    // quiere usar el suyo, que lo ponga a `true` a sabiendas y no por omisión.
    useConnectedDevices = false
}

dependencies {
    implementation(libs.junit)
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}
