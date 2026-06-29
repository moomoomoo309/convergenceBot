@file:Suppress("LocalVariableName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.nio.file.Paths

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.versions)
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

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    antlr(libs.antlr)
    implementation(libs.argparse4j)
    implementation(libs.caldav4j)
    implementation(libs.commonstext)
    implementation(libs.emoji4j)
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
    implementation(libs.poi.core)
    implementation(libs.poi.ooxml)
    implementation(libs.graphviz)
    implementation(libs.graalpolyglot)
    implementation(libs.graaljs)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
}

sourceSets {
    create("lite") {
        kotlin {
            srcDir("src/main/kotlin")
            exclude("**/discord/frat/**")
        }
        java {
            srcDir("src/main/java")
        }
        resources {
            srcDir("src/main/resources")
        }
    }
}

configurations {
    "liteImplementation" {
        extendsFrom(configurations.implementation.get())
    }
    "liteRuntimeOnly" {
        extendsFrom(configurations.runtimeOnly.get())
    }
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

tasks.register<JavaExec>("runLite") {
    classpath = sourceSets["lite"].runtimeClasspath
    mainClass.set("convergence.ConvergenceBot")
    standardInput = System.`in`
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
}

tasks {
    compileKotlin {
        dependsOn(named("generateGrammarSource"))
    }
    compileTestKotlin {
        dependsOn(named("generateTestGrammarSource"))
    }
    named("compileLiteKotlin") {
        dependsOn(named("generateGrammarSource"))
    }
}

tasks.shadowJar {
    // Shadow 9.x tries to open every resolved classpath artifact as a ZIP. GraalVM's transitive dependency
    // org.graalvm.js:js-community is POM-packaged (no JAR), so Shadow fails trying to unzip a .pom file.
    // Fix: use an artifactView filtered to JAR_TYPE so Shadow only sees actual JARs, then disable Shadow's
    // default classpath resolution (configurations = emptyList()) to prevent it from processing the full
    // unfiltered classpath on its own.
    val runtimeJars = project.configurations.runtimeClasspath.get().incoming.artifactView {
        attributes {
            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
        }
        isLenient = true
    }.files
    configurations = emptyList()
    from(runtimeJars.elements.map { it.map { f -> zipTree(f.asFile) } })
    manifest {
        attributes(mapOf("Multi-Release" to "true"))
    }
    isZip64 = true
}

val liteJar by tasks.registering(ShadowJar::class) {
    archiveBaseName.set("convergence.bot-lite")
    from(sourceSets["lite"].output)

    val runtimeJars = project.configurations.getByName("liteRuntimeClasspath").incoming.artifactView {
        attributes {
            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
        }
        isLenient = true
    }.files
    configurations = emptyList()
    from(runtimeJars.elements.map { locs: Set<FileSystemLocation> ->
        locs.map { loc: FileSystemLocation ->
            zipTree(loc.asFile) as Any }
        }
    )
    manifest {
        attributes(mapOf("Multi-Release" to "true"))
    }
    isZip64 = true
}

tasks.register("buildLite") {
    dependsOn("compileLiteKotlin")
    dependsOn(liteJar)
}
