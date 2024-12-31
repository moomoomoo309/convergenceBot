@file:Suppress("LocalVariableName")

import java.nio.file.Paths

val antlr_version = "4.13.2"
val argparse4j_version = "0.9.0"
val coroutines_version = "1.10.1"
val humanize_version = "0.1.2"
val jackson_version = "2.18.2"
val jda_version = "5.2.2"
val logback_version = "1.5.15"
val natty_version = "1.0.1"
val pf4j_version = "3.12.0"

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.github.natty-parser:natty:$natty_version")
    implementation("net.sourceforge.argparse4j:argparse4j:$argparse4j_version")
    implementation("to.lova.humanize:humanize-time:$humanize_version")
    antlr("org.antlr:antlr4:$antlr_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("net.dv8tion:JDA:$jda_version") {
        exclude(module = "opus-java")
    }
    implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")


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
