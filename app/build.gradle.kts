import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tartari.cajuwidget"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.tartari.cajuwidget"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Saldo real do cartão no momento da instalação, em centavos.
        // Prioridade: -PsaldoInicialCentavos=... > env SALDO_INICIAL_CENTAVOS > "0".
        val saldoInicialCentavos: Long =
            ((project.findProperty("saldoInicialCentavos") as String?)
                ?: System.getenv("SALDO_INICIAL_CENTAVOS")
                ?: "0")
                .trim()
                .toLongOrNull()
                ?: error("saldoInicialCentavos / SALDO_INICIAL_CENTAVOS deve ser um inteiro em centavos")
        buildConfigField("long", "SALDO_INICIAL_CENTAVOS", "${saldoInicialCentavos}L")

        // Valor recebido por dia útil no vale, em centavos.
        // Prioridade: -PvalorDiarioCentavos=... > env VALOR_DIARIO_CENTAVOS > "3000" (R$30,00).
        val valorDiarioCentavos: Long =
            ((project.findProperty("valorDiarioCentavos") as String?)
                ?: System.getenv("VALOR_DIARIO_CENTAVOS")
                ?: "3000")
                .trim()
                .toLongOrNull()
                ?: error("valorDiarioCentavos / VALOR_DIARIO_CENTAVOS deve ser um inteiro em centavos")
        buildConfigField("long", "VALOR_DIARIO_CENTAVOS", "${valorDiarioCentavos}L")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    testImplementation("junit:junit:4.13.2")
}
