group = "convergence.test"
version = "1.0-SNAPSHOT"

description = """Discord plugin for convergence bot"""

sourceSets.main {
    kotlin.srcDirs("src")
    resources.srcDir("resources")
}

sourceSets.test {
    kotlin.srcDirs("test/kotlin")
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-beta.15") {
        exclude(module = "opus-java")
    }
    implementation("net.sf.trove4j:trove4j:3.0.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
}

repositories {
    maven(url = "https://m2.dv8tion.net/releases")
}
