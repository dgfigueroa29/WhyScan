import org.gradle.api.artifacts.VersionCatalogsExtension

/** Shell de aplicación Android. Solo lo usa `:androidApp`. */
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun version(alias: String): String = libs.findVersion(alias).get().requiredVersion

android {
    compileSdk = version("compileSdk").toInt()

    defaultConfig {
        minSdk = version("minSdk").toInt()
        targetSdk = version("targetSdk").toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

kotlin {
    jvmToolchain(version("jvmTarget").toInt())
}
