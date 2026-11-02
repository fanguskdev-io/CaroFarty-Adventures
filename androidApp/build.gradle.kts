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
        versionCode = 1
        versionName = "1.0"
        buildConfigField("boolean", "MEASURE", "false")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
            // Firmada con la clave de depuración para instalarla desde el ordenador (README); publicarla en Google
            // Play pediría una clave propia.
            signingConfig = signingConfigs.getByName("debug")
        }
        // Para medir 30 minutos en el teléfono de Fangusk (AUD-29): optimizada como la de lanzamiento, que es la que se
        // juega, pero otra app, «CaroFarty medida», para no tocar sus récords, que juega sola con el piloto automático y
        // enseña la línea de depuración desde que se abre (entregas/LEEME.md).
        create("medida") {
            initWith(getByName("release"))
            applicationIdSuffix = ".medida"
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "MEASURE", "true")
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
