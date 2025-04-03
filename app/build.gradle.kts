plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.inventonater.hid.app"
    compileSdk = Versions.compileSdk

    defaultConfig {
        applicationId = "com.inventonater.hid.app"
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        versionCode = Versions.versionCode
        versionName = Versions.versionName

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core module
    implementation(project(ModulesDependency.core))

    // Android Dependencies
    implementation(Dependencies.Android.coreKtx)
    implementation(Dependencies.Android.appCompat)
    implementation(Dependencies.Android.constraintLayout)
    implementation(Dependencies.Android.material)

    // Kotlin
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Kotlin.coroutinesCore)
    implementation(Dependencies.Kotlin.coroutinesAndroid)
    
    // Lifecycle - Use specific versions to fix the Companion object error
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.5.1")

    // Testing
    testImplementation(Dependencies.Test.junit)
    androidTestImplementation(Dependencies.Test.androidJunit)
    androidTestImplementation(Dependencies.Test.espresso)
}
