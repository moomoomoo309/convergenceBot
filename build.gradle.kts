@file:Suppress("LocalVariableName")

import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import java.nio.file.Path
import java.nio.file.Paths

val pluginsDir: Path = Paths.get(System.getProperty("user.home"), ".convergence", "plugins")
val antlr_version = "4.13.2"
val argparse4j_version = "0.9.0"
val coroutines_version = "1.8.1"
val humanize_version = "1.2.2"
val logback_version = "1.5.6"
val mapdb_version = "3.1.0"
val natty_version = "1.0.1"
val pf4j_version = "3.12.0"

plugins {
    val kotlin_version = "2.0.10"
    kotlin("jvm") version kotlin_version
    id("application")
    antlr
    id("com.gradleup.shadow") version "8.3.0"
    kotlin("kapt") version kotlin_version
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
    implementation("com.github.mfornos:humanize-slim:$humanize_version")
    antlr("org.antlr:antlr4:$antlr_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("org.mapdb:mapdb:$mapdb_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("org.pf4j:pf4j:$pf4j_version")
    implementation(kotlin("stdlib-jdk8"))

    implementation("net.dv8tion:JDA:5.0.2") {
        exclude(module = "opus-java")
    }
    implementation("club.minnced:jda-ktx:0.12.0")
    implementation("net.sf.trove4j:trove4j:3.0.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
    mainClass.set("convergence.core")
}

tasks.register<Copy>("copyBot") {
    dependsOn("shadowJar")
    from("./build/libs/")
    into(Paths.get(System.getProperty("user.home"), ".convergence"))
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    dependsOn("buildDependents")
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
    withType<KaptGenerateStubsTask> {
        dependsOn(named("generateGrammarSource"))
    }
}
