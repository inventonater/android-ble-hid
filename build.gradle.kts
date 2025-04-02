// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.gradlePlugin}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
    
    // Fix dependency conflicts by enforcing consistent versions
    configurations.all {
        resolutionStrategy {
            // Force consistent Kotlin stdlib version across all dependencies
            force("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:${Versions.kotlin}")
            force("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
            
            // Force consistent AndroidX versions
            force("androidx.appcompat:appcompat:${Versions.appcompat}")
            force("androidx.core:core:${Versions.core}")
            force("androidx.core:core-ktx:${Versions.core}")
            force("androidx.constraintlayout:constraintlayout:${Versions.constraint}")
            
            // Allow issues to be reported
            failOnVersionConflict()
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
