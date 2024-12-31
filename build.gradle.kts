@file:Suppress("LocalVariableName")

import java.nio.file.Paths

plugins {
    val kotlin_version = "2.1.0"
    kotlin("jvm") version kotlin_version
    id("application")
    antlr
    id("com.gradleup.shadow") version "8.3.0"
}

group = "convergence"
version = "1.0-SNAPSHOT"

description = """Convergence Bot"""


repositories {
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencies {
    implementation(libs.coroutines)
    implementation(libs.kotlin.reflect)

    implementation(libs.natty)
    implementation(libs.argparse4j)
    implementation(libs.humanize)
    antlr(libs.antlr)
    implementation(libs.logback)

    implementation(libs.jda) {
        exclude(module = "opus-java")
    }
    implementation(libs.jackson.databind)
    implementation(libs.jackson.jsr310)
    implementation(libs.jackson.kotlin)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
    mainClass.set("convergence.ConvergenceBot")
}

tasks.register<Copy>("copyBot") {
    from(tasks.named("shadowJar"))
    into(Paths.get(System.getProperty("user.home"), ".convergence"))
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.wrapper {
    gradleVersion = "8.9"
}

tasks {
    build {
        dependsOn(named("generateGrammarSource"))
    }
    compileKotlin {
        dependsOn(named("generateGrammarSource"))
    }
}
