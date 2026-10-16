import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Optional release signing: keystore.properties (git-ignored) with storeFile, storePassword, keyAlias, keyPassword.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "com.repforge.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.repforge.app"
        minSdk = 26
        targetSdk = 36
        // The Expo app shipped versionCode 9 under this applicationId; stay above it.
        versionCode = 10
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) create("release") {
            storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets { getByName("androidTest").assets.srcDir("$projectDir/schemas") }
    // Keeps the per-app language picker (Android 13+) in sync with the shipped translations.
    androidResources { localeFilters += listOf("en", "pt-rBR") }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-runtime:2.8.1")
    implementation("androidx.room:room-ktx:2.8.1")
    ksp("androidx.room:room-compiler:2.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.android.billingclient:billing-ktx:9.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation("androidx.room:room-testing:2.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    // room-testing needs serialization >= 1.8, but the debug app ships 1.7.3 transitively and the test APK reuses it.
    debugImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    testImplementation("junit:junit:4.13.2")
    // android.jar ships org.json as throwing stubs; unit tests need the real one.
    testImplementation("org.json:json:20240303")
}
