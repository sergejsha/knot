plugins {
    base
    kotlin("jvm") version Deps.Version.kotlin apply false
}

allprojects {
    group = "de.halfbit"
    version = "1.8.1"

    repositories {
        mavenCentral()
        jcenter()
    }
}
