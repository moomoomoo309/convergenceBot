plugins {
    kotlin("jvm")
}
group = "convergence.bot"
version = "1.0-SNAPSHOT"

description = """test plugin for convergence bot"""

dependencies {
    implementation(rootProject)
    implementation(kotlin("stdlib-jdk8"))
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
