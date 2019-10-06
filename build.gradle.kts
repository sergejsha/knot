plugins {
    base
    kotlin("jvm") version Deps.Version.kotlin apply false
}

allprojects {
    group = "de.halfbit"
    version = "1.7.0-alpha1"

    repositories {
        mavenCentral()
        jcenter()
    }
}
