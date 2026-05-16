/*
 * Copyright (c) 7/12/2022, Pexers (https://github.com/Pexers)
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.6.20"
}

group = "com.pexers.quickfaas"
version = "1.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("controller.MainKt")
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        ) // We need this for Gradle optimization to work
        archiveClassifier.set("fat") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes["Main-Class"] = application.mainClass }
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}

dependencies {
    val ktorVersion = "2.0.3"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")                 // Sessions for tokens
    implementation("io.ktor:ktor-server-auth:$ktorVersion")                     // OAuth
    implementation("io.ktor:ktor-client-cio:$ktorVersion")                      // CIO engine for HTTP client
    implementation("ch.qos.logback:logback-classic:1.2.11")                     // Required by some libraries
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}