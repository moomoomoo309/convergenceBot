plugins {
    kotlin("jvm")
}
group = "convergence.test"
version = "1.0-SNAPSHOT"

description = """Discord plugin for convergence bot"""


dependencies {
    implementation("net.dv8tion:JDA:5.0.2") {
        exclude(module = "opus-java")
    }
    implementation("net.sf.trove4j:trove4j:3.0.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation(kotlin("stdlib-jdk8"))
}

repositories {
    maven(url = "https://m2.dv8tion.net/releases")
    mavenCentral()
}
