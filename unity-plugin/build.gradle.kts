plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.inventonater.hid.unity"
    compileSdk = Versions.compileSdk
    
    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

// Custom task to copy the AAR to Unity project
tasks.register<Copy>("copyToUnity") {
    dependsOn("assembleRelease")
    from("${buildDir}/outputs/aar/unity-plugin-release.aar")
    into("../unity-test/Assets/Plugins/Android")
    rename("unity-plugin-release.aar", "BleHidPlugin.aar")
}

dependencies {
    implementation(project(":core"))
    
    // Testing dependencies
    testImplementation(Dependencies.Test.junit)
    androidTestImplementation(Dependencies.Test.androidJunit)
    
    // Add Kotlin dependencies
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Kotlin.coroutinesCore) 
    implementation(Dependencies.Kotlin.coroutinesAndroid)
}
