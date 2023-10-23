group = "convergence.bot"
version = "1.0-SNAPSHOT"

description = """console plugin for convergence bot"""

dependencies {
    implementation(rootProject)
}

sourceSets.main {
    kotlin.srcDirs("src")
    resources.srcDir("resources")
}
val pluginClass: String by project

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = pluginClass
    }
}
