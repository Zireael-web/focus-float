import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun signingValue(propertyName: String, envName: String): String? {
    return keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
}

val internalStoreFilePath = signingValue("internalStoreFile", "FOCUSFLOAT_INTERNAL_STORE_FILE")
    ?: "keystores/focusfloat-internal.jks"
val internalStorePassword = signingValue("internalStorePassword", "FOCUSFLOAT_INTERNAL_STORE_PASSWORD")
val internalKeyAlias = signingValue("internalKeyAlias", "FOCUSFLOAT_INTERNAL_KEY_ALIAS")
val internalKeyPassword = signingValue("internalKeyPassword", "FOCUSFLOAT_INTERNAL_KEY_PASSWORD")
val hasInternalSigningCredentials = rootProject.file(internalStoreFilePath).isFile &&
    listOf(internalStorePassword, internalKeyAlias, internalKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.focusfloat.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.focusfloat.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "0.1.12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("internal") {
            storeFile = rootProject.file(internalStoreFilePath)
            storePassword = internalStorePassword
            keyAlias = internalKeyAlias
            keyPassword = internalKeyPassword
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        create("internal") {
            initWith(getByName("debug"))
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("internal")
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasInternalSigningCredentials) {
                signingConfig = signingConfigs.getByName("internal")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")

    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    ksp("androidx.room:room-compiler:2.8.4")

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
