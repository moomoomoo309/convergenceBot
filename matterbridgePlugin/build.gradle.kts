plugins {
    id("de.undercouch.download")
}


group = "convergence.test"
version = "1.0-SNAPSHOT"

description = "Discord plugin for convergence bot"

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
}
