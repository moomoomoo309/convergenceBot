import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}
group = "convergence.bot"
version = "1.0-SNAPSHOT"

description = """console plugin for convergence bot"""

dependencies {
    implementation(rootProject)
    implementation(kotlin("stdlib-jdk8"))
}

sourceSets.main {
    withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
        kotlin.srcDirs("src")
    }
    resources.srcDir("resources")
}
val pluginClass: String by project

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = pluginClass
    }
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = JavaVersion.VERSION_11.toString()
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = JavaVersion.VERSION_11.toString()
}
