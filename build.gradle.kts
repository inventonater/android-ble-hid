// Top-level build file where you can add configuration options common to all sub-projects/modules

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
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
            force("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:1.8.10")
            force("org.jetbrains.kotlin:kotlin-reflect:1.8.10")
            
            // Force consistent AndroidX versions
            force("androidx.appcompat:appcompat:1.6.1")
            force("androidx.core:core:1.10.1")
            force("androidx.constraintlayout:constraintlayout:2.1.4")
            force("androidx.activity:activity:1.6.0")
            force("androidx.fragment:fragment:1.3.6")
            force("androidx.lifecycle:lifecycle-runtime:2.5.1")
            force("androidx.lifecycle:lifecycle-viewmodel:2.5.1")
            force("androidx.savedstate:savedstate:1.2.0")
            force("androidx.drawerlayout:drawerlayout:1.1.1")
            force("androidx.annotation:annotation:1.6.0")
            force("androidx.annotation:annotation-experimental:1.3.0")
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Task to print dependency tree for debugging
tasks.register("showDeps") {
    doLast {
        println("\nDependency Report for project: ${project.name}")
        project.configurations.getByName("implementation").resolvedConfiguration.lenientConfiguration.allModuleDependencies.forEach { dep ->
            println("Implementation depends on: ${dep.name}")
        }
    }
}

// Apply the task to all projects
subprojects {
    tasks.register("showDependencies") {
        doLast {
            println("\nDependency Report for project: ${project.name}")
            configurations.filter { it.isCanBeResolved }.forEach { config ->
                println("\n${config.name}:")
                config.resolvedConfiguration.resolvedArtifacts.forEach { art ->
                    println("${art.moduleVersion.id}:${art.type}")
                }
            }
        }
    }
}
