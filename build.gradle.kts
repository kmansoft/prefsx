import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id ("com.android.library")
}

android {
    compileSdk = 37

    namespace = "org.kman.prefsx"

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility (JavaVersion.VERSION_11)
        targetCompatibility (JavaVersion.VERSION_11)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    // Support
    implementation ("androidx.appcompat:appcompat:1.8.0")
    implementation ("androidx.recyclerview:recyclerview:1.4.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation ("androidx.collection:collection-ktx:1.6.0")
    implementation ("androidx.preference:preference-ktx:1.2.1")

    testImplementation ("junit:junit:4.13.2")

    androidTestImplementation ("androidx.test.ext:junit:1.3.0")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.7.0")
}
