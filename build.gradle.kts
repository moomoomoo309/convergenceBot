@file:Suppress("LocalVariableName")

import java.nio.file.Paths

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    application
    antlr
}

group = "convergence"
version = "1.0-SNAPSHOT"

description = """Convergence Bot"""


repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencies {
    antlr(libs.antlr)
    implementation(libs.argparse4j)
    implementation(libs.caldav4j)
    implementation(libs.commonstext)
    implementation(libs.coroutines)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.jsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.jda) {
        exclude(module = "opus-java")
    }
    implementation(libs.kotlin.reflect)
    implementation(libs.logback)
    implementation(libs.natty)
    implementation(libs.prettytime)
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
    gradleVersion = libs.versions.gradle.get()
}

tasks {
    build {
        dependsOn(named("generateGrammarSource"))
    }
    compileKotlin {
        dependsOn(named("generateGrammarSource"))
    }
    compileTestKotlin {
        dependsOn(named("generateTestGrammarSource"))
    }
}
