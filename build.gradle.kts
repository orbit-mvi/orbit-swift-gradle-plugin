import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm") version "1.7.10"
    id("com.gradle.plugin-publish") version "0.15.0"
}

gradlePlugin {
    plugins {
        create("org.orbit-mvi.orbit.swift") {
            id = "org.orbit-mvi.orbit.swift"
            displayName = "orbitswiftplugin"
            description = "Generate swift code for Orbit Multiplatform"
            implementationClass = "org.orbitmvi.orbit.swift.OrbitSwiftPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/orbit-mvi/orbit-swift-gradle-plugin"
    vcsUrl = "https://github.com/orbit-mvi/orbit-swift-gradle-plugin.git"
    tags = listOf("kotlin", "mvi", "swift", "multiplatform")

    mavenCoordinates {
        groupId = "org.orbit-mvi"
        artifactId = "org.orbit-mvi.orbit.swift"
    }
}

version = (System.getenv("GITHUB_REF") ?: System.getProperty("GITHUB_REF"))
    ?.replaceFirst("refs/tags/", "") ?: "unspecified"
group = "org.orbit-mvi"

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")

    implementation(kotlin("stdlib-jdk8", "1.7.10"))
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:0.0.3")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.7.10")
    implementation("com.samskivert:jmustache:1.15")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
