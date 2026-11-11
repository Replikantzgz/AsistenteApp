import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.iatualcance.alfred"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.iatualcance.alfred"
        minSdk = 26
        targetSdk = 35
        // En GitHub Actions el número sube solo con cada compilación (ALFRED_VERSION_CODE); en local, valor fijo.
        versionCode = (System.getenv("ALFRED_VERSION_CODE") ?: "4").toInt()
        versionName = System.getenv("ALFRED_VERSION_NAME") ?: "1.3.0"
        buildConfigField("String", "UPDATE_URL", "\"https://github.com/Replikantzgz/AsistenteApp/releases/latest/download/update.json\"")
    }

    // La firma no está en el repositorio. Se lee de keystore.properties (local, ignorado por git)
    // o de variables de entorno (GitHub Actions). Sin ella se firma con la clave de depuración,
    // y entonces el inicio de sesión de Google no funcionará porque la SHA-1 será otra.
    val ksProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun ks(name: String): String? = ksProps.getProperty(name) ?: System.getenv("ALFRED_" + name.uppercase())
    val ksFile = ks("file")?.let { rootProject.file(it) }?.takeIf { it.exists() }

    signingConfigs {
        if (ksFile != null) create("alfred") {
            storeFile = ksFile
            storePassword = ks("password")
            keyAlias = ks("alias") ?: "alfred"
            keyPassword = ks("password")
        }
    }
    val signing = signingConfigs.findByName("alfred") ?: signingConfigs.getByName("debug")

    buildTypes {
        debug {
            signingConfig = signing
        }
        release {
            isMinifyEnabled = false
            signingConfig = signing
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Inicio de sesión y permisos de Google (Calendar, Gmail, Contactos, Drive)
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    // Escáner de documentos
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    testImplementation("junit:junit:4.13.2")
}
