plugins {
    base
    kotlin("jvm") version "1.3.31" apply false
}

allprojects {
    group = "de.halfbit"
    version = "1.0-beta8"

    repositories {
        mavenCentral()
        jcenter()
    }
}
