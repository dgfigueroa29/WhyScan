plugins {
    id("whyscan.android.application")
    alias(libs.plugins.baselineProfile)
}

android {
    namespace = "com.whyscan.android"

    defaultConfig {
        // El `applicationId` es la identidad de la app en Play **para siempre**: es la URL de la
        // ficha y la clave con la que el sistema reconoce una actualización. No se puede cambiar
        // después de la primera publicación, así que se ajusta ahora que todavía no hay ninguna.
        //
        // No tiene por qué coincidir con los paquetes de Kotlin, pero aquí coincide: el proyecto
        // usa `com.whyscan.*` en todas partes —paquetes, `namespace` de cada módulo, plugins de
        // convención y almacenes de datos—, así que no hay dos nombres que mantener sincronizados
        // ni ninguno que explicar.
        //
        // Antes de la primera subida hay que comprobar en Play Console que este id está libre.
        applicationId = "com.whyscan.app"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true

            // Encoger recursos exige encoger código. No toca `assets/`, que es donde Compose
            // Multiplatform empaqueta los `composeResources`: los textos siguen ahí.
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        // El plugin de baseline profile deriva de `release` dos tipos de build propios:
        // `nonMinifiedRelease`, sobre el que se graba el perfil —sin R8, para que los nombres de los
        // métodos sean los de verdad— y `benchmarkRelease`, para medir. Los crea él, así que se
        // configuran aquí en lugar de nombrarlos: `all` alcanza también a lo que llegue después.
        //
        // Firma: `release` no la tiene, y está bien que no la tenga —la de subida a Play no vive en
        // el repositorio—, pero un APK sin firmar no se puede instalar en el emulador y sin
        // instalarlo no hay nada que grabar. Se les presta la de debug, que es de juguete y no sale
        // de la máquina que corre la grabación.
        all {
            if (name != "debug" && name != "release") {
                signingConfig = signingConfigs.getByName("debug")

                // Sin esto, las dependencias —`:composeApp` y los motores— no saben qué variante
                // suya emparejar con una que ellas no declaran.
                if ("release" !in matchingFallbacks) {
                    matchingFallbacks += "release"
                }
            }
        }
    }
}

baselineProfile {
    // El perfil se versiona y la build de release consume el archivo del repositorio. Generarlo es
    // un acto deliberado —el workflow `baseline-profile.yml`— y no un efecto colateral de compilar:
    // si `assembleRelease` arrancara un emulador, nadie podría ensamblar la app sin uno.
    automaticGenerationDuringBuild = false
}

dependencies {
    implementation(project(":composeApp"))
    // La Activity presta sus `ActivityResultLauncher` al controlador de permisos, al selector de
    // imágenes y al guardado de archivos, así que necesita ver esos tres contratos. `:composeApp`
    // los declara como `implementation`, que no es transitivo, y por eso hay que nombrarlos aquí:
    // sin `:core:platform` la compilación fallaba con "Cannot access 'ImagePicker' which is a
    // supertype of 'AndroidImagePicker'".
    implementation(project(":core:permissions"))
    implementation(project(":core:platform"))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    // La pantalla de arranque del sistema. Es la única forma de que el hueco entre que el sistema
    // crea la ventana y Compose pinta la primera pantalla deje de ser un rectángulo de color: ver
    // el comentario de `Theme.WhyScan.Starting` en `themes.xml`.
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.compose.runtime)

    // Instala el baseline profile que viaja en el APK/AAB.
    //
    // No sobra por venir de Play: en Android 12+ el sistema instala el perfil por su cuenta, pero
    // esta app soporta desde la 24 y en el tramo 24-30 no lo hace nadie. Sin esta dependencia, el
    // perfil sería peso muerto justo en los dispositivos más lentos, que son los que lo necesitan.
    implementation(libs.androidx.profileinstaller)

    // De dónde sale el perfil. La generación **no** ocurre en `assembleRelease`: esta línea solo
    // dice qué módulo lo produce cuando se pide `:androidApp:generateBaselineProfile`. Lo que la
    // build de release consume es el archivo ya generado y versionado en `src/release/generated/`.
    baselineProfile(project(":baselineprofile"))
}
