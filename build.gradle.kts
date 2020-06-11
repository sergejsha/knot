plugins {
    kotlin("jvm") version Deps.Version.kotlin apply false
    id("org.jetbrains.dokka") version Deps.Version.dokka apply false
    id("com.android.application") version Deps.Version.agp apply false
}

allprojects {
    group = "de.halfbit"
    version = "3.2-alpha1"

    repositories {
        mavenCentral()
        jcenter()
    }
}
