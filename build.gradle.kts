
import java.nio.file.Path
import java.nio.file.Paths

val pluginsDir: Path = Paths.get(System.getProperty("user.home"), ".convergence", "plugins")

plugins {
    `version-catalog`
    alias(libs.plugins.kotlin)
    id("application")
    antlr
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.kapt)
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
    }
}

dependencies {
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.reflect)

    implementation(libs.natty) {
        exclude("commons-codec:commons-codec")
    }
    implementation("commons-codec:commons-codec:1.16.0")

    implementation(libs.argparse4j)

    implementation(libs.humanize) {
        exclude("com.google.guava:guava")
    }
    implementation("com.google.guava:guava:32.1.3-jre")

    antlr(libs.antlr)
    implementation(libs.logback)

    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)

    implementation(libs.mapdb)

    implementation(libs.pf4j)

    testImplementation(libs.kotlin.test)
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
            // Add dependencies to plugins declared in that plugin's dependencies block, but not this one's.
            from(
                    project.configurations.compileClasspath.get().mapNotNull { originalFile ->
                        if (!rootProject.configurations.compileClasspath.get().contains(originalFile)
                                && !originalFile.toPath().startsWith(rootProject.projectDir.toPath())) {
                            if (originalFile.isDirectory) originalFile else zipTree(originalFile)
                        } else
                            null
                    }
            ).also { it.duplicatesStrategy = DuplicatesStrategy.WARN }.exclude {
                it.path.contains("META-INF/MANIFEST.MF")
            }
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
        implementation(rootProject.libs.kotlin.coroutines)
        implementation(rootProject.libs.kotlin.reflect)

        implementation(rootProject.libs.natty)
        implementation(rootProject.libs.argparse4j)
        implementation(rootProject.libs.humanize)
        implementation(rootProject.libs.logback)

        implementation(rootProject.libs.moshi.kotlin)
        implementation(rootProject.libs.moshi.adapters)

        implementation(rootProject.libs.mapdb)

        implementation(rootProject.libs.pf4j)

        implementation(rootProject.libs.antlr)

        implementation(rootProject)
    }
}

sourceSets.main {
    kotlin.srcDirs("src")
    java.srcDirs("src/main/convergence")
    resources.srcDir("resources")
}

sourceSets.test {
    kotlin.srcDirs("test/kotlin")
}

kotlin {
    jvmToolchain(17)
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
    gradleVersion = "8.2"
}

tasks {
    build {
        dependsOn(named("assemblePlugins"))
    }
}
