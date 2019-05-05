plugins {
    base
    kotlin("jvm") version Dep.Version.kotlin apply false
}

allprojects {
    group = "de.halfbit"
    version = "1.0-beta8"

    repositories {
        mavenCentral()
        jcenter()
    }
}
