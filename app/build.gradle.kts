plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.ghonti.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.ghonti.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("ghonti.keystore")
            storePassword = "ghonti123"
            keyAlias = "ghonti"
            keyPassword = "ghonti123"
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
