/**
 * Librería Kotlin Multiplatform con los cuatro targets del proyecto.
 *
 * Sustituye a las ~35 líneas idénticas que cada módulo repetía. Con los motores de la Fase 2 y 3
 * esa duplicación llegaría a una docena de copias, y cualquier cambio de `compileSdk` o de
 * toolchain habría que hacerlo doce veces (deuda D1 del roadmap).
 *
 * El `namespace` de Android se deja fuera a propósito: es lo único genuinamente distinto en cada
 * módulo, y ponerlo aquí obligaría a inventar una convención de nombres frágil.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun version(alias: String): String = libs.findVersion(alias).get().requiredVersion

kotlin {
    jvmToolchain(version("jvmTarget").toInt())

    // Los `expect class` siguen en beta y el compilador lo avisa **una vez por compilación**: diez
    // líneas por build para una sola declaración, la de `DatabaseBuilderFactory`. Es el aviso que
    // el propio mensaje propone silenciar con esta bandera, y silenciarlo aquí no tapa nada nuestro:
    // dice que la característica del lenguaje es beta, no que haya un problema en este código.
    //
    // Se acepta a conciencia, y forma parte de la postura sobre avisos de la deuda D19: el ruido que
    // se puede quitar se quita, para que el que quede se pueda leer.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget()
    jvm()
    // Sin `iosX64()`: es el simulador de los Mac con Intel, y Compose Multiplatform 1.11.1 ya no
    // publica artefactos para ese target. Declararlo hacía fallar la resolución de dependencias de
    // `commonMain` en **todos** los módulos con Compose. Los Mac con Apple Silicon usan
    // `iosSimulatorArm64`, que sí está.
    iosArm64()
    iosSimulatorArm64()

    wasmJs { browser() }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    compileSdk = version("compileSdk").toInt()

    defaultConfig {
        minSdk = version("minSdk").toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        targetSdk = version("targetSdk").toInt()
    }

    packaging {
        resources {
            excludes.add("META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/versions/9/previous-compilation-data.bin")
            excludes.add("META-INF/*.kotlin_module")
            excludes.add("**/META-INF/LICENSE*")
            excludes.add("**/META-INF/NOTICE*")
            excludes.add("**/META-INF/*.kotlin_module")
            excludes.add("META-INF/MANIFEST.MF")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    beforeVariants { variantBuilder ->
        variantBuilder.androidTest.enable = false
    }
}
