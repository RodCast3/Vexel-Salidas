plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.vexel_salida"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vexel_salida"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    //Camara
    val version = "1.4.2"
    implementation("androidx.camera:camera-core:${version}")
    implementation("androidx.camera:camera-camera2:${version}")
    implementation("androidx.camera:camera-lifecycle:${version}")
    implementation("androidx.camera:camera-view:${version}")
    implementation ("androidx.camera:camera-extensions:${version}")

    //ML Kit para lectura de QR
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    //ML kit para la lectura de caras
    implementation ("com.google.mlkit:face-detection:16.1.7")
    implementation ("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")

    //Permisos
    implementation("com.google.accompanist:accompanist-permissions:0.28.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}