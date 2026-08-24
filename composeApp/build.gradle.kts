import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Único módulo que NO usa `whyscan.kmp.compose`: necesita frameworks de iOS, un ejecutable de
// Wasm y el target JVM nombrado "desktop" para el plugin de escritorio. Meter todo eso en un
// convention plugin que solo usaría este módulo sería una abstracción de un solo cliente.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    androidTarget()

    // Se llama "desktop" por convención de Compose Multiplatform: el plugin de escritorio busca
    // ese nombre. Las librerías declaran `jvm()` a secas y resuelven igual, porque el emparejado
    // es por atributos de la variante y no por el nombre del target.
    jvm("desktop")

    // Sin iosX64 (simulador Intel): CMP 1.11.1 no lo publica. Ver el convention plugin.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:data"))
            implementation(project(":core:domain"))
            implementation(project(":core:permissions"))
            implementation(project(":core:platform"))
            implementation(project(":core:designsystem"))
            implementation(project(":feature:scanner"))
            implementation(project(":feature:history"))
            implementation(project(":feature:settings"))

            // Único módulo que conoce a todos los motores: es el composition root.
            implementation(project(":engines:manual"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            // Llega de rebote con material3, pero `App.kt` usa `AnimatedContent` directamente y una
            // dependencia que se usa a la cara se declara.
            implementation(compose.animation)
            implementation(compose.material3)
            implementation(compose.components.resources)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.multiplatform.settings)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        // El grafo de Android necesita un `Context` y nada más; Robolectric lo aporta sin
        // emulador. Ver `AndroidKoinGraphTest` — es la mitad que le faltaba a D18.
        //
        // `by getting` y no el accesor con nombre: `androidUnitTest` es el source set que crea
        // `androidTarget()`, y pedirlo por nombre no depende de qué accesores de conveniencia traiga
        // la versión de Kotlin que toque.
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.robolectric)
                // `@RunWith` es de JUnit 4. Llega de rebote con Robolectric y con
                // `kotlin-test-junit`, pero el código lo nombra a la cara.
                implementation(libs.junit)
            }
        }

        val desktopMain by getting

        // Componer la raíz de la app de verdad (`AppCompositionTest`). Escritorio es la única de
        // las cuatro plataformas donde eso se puede hacer en un test JVM normal, sin emulador ni
        // navegador.
        //
        // Los dos artefactos de lifecycle se nombran aquí aunque ya estén en el classpath de
        // ejecución: llegan como `implementation` de los módulos de feature, que no es transitivo
        // para compilar, y el test los usa a la cara para proveer el `LifecycleOwner` y el
        // `ViewModelStoreOwner` que en la app pone la plataforma.
        val desktopTest by getting {
            dependencies {
                // `compose.uiTest` está marcado como experimental por el propio plugin de Compose,
                // y sin este `@OptIn` la **compilación del script** falla —no es un aviso—. Lo que
                // hay detrás es `runComposeUiTest`, la API multiplataforma de test de composición:
                // experimental en el sentido de que su firma puede cambiar, no en el de que no
                // funcione.
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.lifecycle.viewmodel.compose)
            }
        }

        androidMain.dependencies {
            // Los motores de Android se enlazan SOLO aquí: el binario de iOS, Desktop y Web no
            // debe cargar ML Kit ni Play Services (RNF-06).
            implementation(project(":engines:gms-code-scanner"))
            implementation(project(":engines:mlkit-camerax"))
            implementation(project(":engines:mlkit-ocr"))
            implementation(project(":engines:zxing-cpp"))

            // Room KMP no soporta wasmJs, así que la base de datos se enlaza en los tres targets
            // que sí la admiten; Web usa el historial en memoria de :core:data.
            implementation(project(":core:database"))

            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            implementation(project(":core:database"))
            // Motor de iOS: no debe enlazarse en ningún otro binario (RNF-06).
            implementation(project(":engines:vision-ios"))
            implementation(project(":engines:zxing-cpp"))
        }

        desktopMain.dependencies {
            implementation(project(":engines:zxing-java"))
            implementation(project(":core:database"))
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        wasmJsMain.dependencies {
            // El decodificador lo pone el navegador, así que este módulo no añade peso al bundle.
            implementation(project(":engines:browser-detector"))
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.whyscan.resources"
    generateResClass = always
}

android {
    namespace = "com.whyscan.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric monta el `Context` a partir del manifiesto y los recursos ya fusionados.
            // Sin esto los tests no fallan: arrancan con un entorno vacío, que es peor — darían por
            // bueno un grafo que en el teléfono no se parece al que se probó.
            isIncludeAndroidResources = true
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.whyscan.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "WhyScan"
            packageVersion = "1.0.0"
        }
    }
}
