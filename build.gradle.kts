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
val moshi_version = "1.15.1"
val natty_version = "0.13"
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

    implementation("com.joestelmach:natty:$natty_version")
    implementation("net.sourceforge.argparse4j:argparse4j:$argparse4j_version")
    implementation("com.github.mfornos:humanize-slim:$humanize_version")
    antlr("org.antlr:antlr4:$antlr_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("com.squareup.moshi:moshi-kotlin:$moshi_version")

    implementation("org.mapdb:mapdb:$mapdb_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("org.pf4j:pf4j:$pf4j_version")
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

// here we define the tasks which will build the plugins in the subprojects
subprojects {
    repositories {
        mavenCentral()
    }
    // if the variable definitions are put here they are resolved for each subproject
    val pluginId: String by project
    val pluginClass: String by project
    val pluginProvider: String by project

    val project = this
    // we have to apply the gradle jvm plugin, because it provides the jar and build tasks
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    // for the jar task we have to set the plugin properties, so they can be written to the manifest
    // Yes, it's needed on this one _and_ the plugin task.
    tasks.named<Jar>("jar") {
        manifest {
            attributes["Plugin-Class"] = pluginClass
            attributes["Plugin-Id"] = pluginId
            attributes["Plugin-Version"] = archiveVersion
            attributes["Plugin-Provider"] = pluginProvider
        }
    }

    // the plugin task will put the files into a zip file
    tasks.register<Jar>("plugin") {
        archiveBaseName.set("plugin-${pluginId}")

        into("classes") {
            with(tasks.named<Jar>("jar").get())
        }
        dependsOn(configurations.runtimeClasspath)

        archiveExtension.set("zip")

        // For whatever reason, gradle refuses to put the correct files in META-INF and instead puts them in
        // classes/META-INF. For some things, this is fine. For PF4J, this is not. So, in order to put those things
        // in the right folder, I grab them from the tmp directory where they live while it's building the jar.
        // This is an ugly hack, but it's gradle's fault for not putting stuff in the root META-INF when I ask it to.
        // Thanks for coming to my TED Talk. - Nick DeLello
        into("META-INF") {
            from("build/tmp/jar", "build/tmp/kapt3/classes/main/META-INF")
        }

        manifest {
            attributes["Plugin-Class"] = pluginClass
            attributes["Plugin-Id"] = pluginId
            attributes["Plugin-Version"] = archiveVersion
            attributes["Plugin-Provider"] = pluginProvider
        }
    }

    // the assemblePlugin will copy the zip file into the common plugins directory
    tasks.register<Copy>("assemblePlugin") {
        from(project.tasks.named("plugin"))
        into(pluginsDir)
    }


    tasks.named("build") {
        dependsOn(tasks.named("plugin"))
    }

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
        implementation("org.jetbrains.kotlin:kotlin-reflect")

        implementation("com.joestelmach:natty:$natty_version")
        implementation("net.sourceforge.argparse4j:argparse4j:$argparse4j_version")
        implementation("com.github.mfornos:humanize-slim:$humanize_version")
        implementation("ch.qos.logback:logback-classic:$logback_version")

        implementation("com.squareup.moshi:moshi-kotlin:$moshi_version")
        implementation("com.squareup.moshi:moshi-adapters:$moshi_version")

        implementation("org.mapdb:mapdb:$mapdb_version")

        implementation("org.pf4j:pf4j:$pf4j_version")

        testImplementation(kotlin("test"))
        implementation(rootProject)
    }
}

application {
    mainClass.set("convergence.core")
}

tasks.register<Copy>("assemblePlugins") {
    dependsOn(subprojects.map { it.tasks.named("assemblePlugin") })
}

tasks.register<Copy>("copyBot") {
    dependsOn("assemblePlugins")
    from("./build/libs/")
    into(Paths.get(System.getProperty("user.home"), ".convergence"))
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    dependsOn("buildDependents")
}

tasks.named<Task>("buildDependents") {
    finalizedBy("copyBot")
}

tasks.wrapper {
    gradleVersion = "8.9"
}

tasks {
    build {
        dependsOn(named("assemblePlugins"))
        dependsOn(named("generateGrammarSource"))
    }
    compileKotlin {
        dependsOn(named("generateGrammarSource"))
    }
    withType<KaptGenerateStubsTask> {
        dependsOn(named("generateGrammarSource"))
    }
}
