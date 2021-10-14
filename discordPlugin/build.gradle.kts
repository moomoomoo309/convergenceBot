group = "convergence.test"
version = "1.0-SNAPSHOT"

description = """Discord plugin for convergence bot"""

sourceSets.main {
    withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
        kotlin.srcDirs("src")
    }
    resources.srcDir("resources")
}

sourceSets.test {
    withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
        kotlin.srcDirs("test/kotlin")
    }
}

dependencies {
    implementation("net.dv8tion:JDA:4.3.0_307") {
        exclude(module = "opus-java")
    }
    implementation("net.sf.trove4j:trove4j:3.0.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.1")
}

repositories {
    maven(url = "https://m2.dv8tion.net/releases")
}
