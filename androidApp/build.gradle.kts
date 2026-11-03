import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}

android {
    namespace = "com.fanguskdev.carofarty"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.fanguskdev.carofarty"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // El número de cada APK (TEC-14): los commits del repo, así uno nuevo siempre va encima del anterior y el mismo
        // commit da el mismo. Sin git, 1.
        versionCode = commitCount()
        versionName = "1.0"
        buildConfigField("boolean", "MEASURE", "false")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // La clave propia de Fangusk (TEC-14), fuera del repo: el almacén en CAROFARTY_FIRMA y su contraseña en
    // CAROFARTY_CLAVE, que pone scripts/firmar.sh desde el llavero del Mac (docs/firma.md). Sin ellas, la de depuración.
    val firma = providers.environmentVariable("CAROFARTY_FIRMA").orNull
    val clave = providers.environmentVariable("CAROFARTY_CLAVE").orNull
    signingConfigs {
        if (firma != null && clave != null) {
            create("propia") {
                storeFile = file(firma)
                storePassword = clave
                keyAlias = "carofarty"
                keyPassword = clave
            }
        }
    }
    buildTypes {
        release {
            // R8 optimiza Compose: el juego va más fluido en la versión de lanzamiento que en la de depuración.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Con la clave propia si scripts/firmar.sh la da: la versión definitiva, la que se instala en el teléfono de
            // Caro y las que vengan encima. Si no, la de depuración, para las pruebas y la puerta.
            signingConfig = signingConfigs.findByName("propia") ?: signingConfigs.getByName("debug")
        }
        // Para medir 30 minutos en el teléfono de Fangusk (AUD-29): optimizada como la de lanzamiento, que es la que se
        // juega, pero otra app, «CaroFarty medida», para no tocar sus récords, que juega sola con el piloto automático y
        // enseña la línea de depuración desde que se abre (entregas/LEEME.md).
        create("medida") {
            initWith(getByName("release"))
            applicationIdSuffix = ".medida"
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "MEASURE", "true")
            // Otra app, solo para medir: siempre con la de depuración.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

/** Cuántos commits lleva el repo, o 1 si no se puede saber. */
fun commitCount(): Int = providers
    .exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText
    .get()
    .trim()
    .toIntOrNull() ?: 1
