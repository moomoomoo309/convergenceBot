group = "convergence.test"
version = "1.0-SNAPSHOT"

description = "Matterbridge plugin for convergence bot"

sourceSets.main {
    kotlin.srcDirs("src")
    resources.srcDir("resources")
}

sourceSets.test {
    kotlin.srcDirs("test/kotlin")
}
