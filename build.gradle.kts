plugins {
    kotlin("jvm") version Deps.Version.kotlin apply false
    id("com.android.application") version Deps.Version.agp apply false // Only required for the sample
}

allprojects {
    group = "de.halfbit"
    version = "1.8.1"

    repositories {
        mavenCentral()
        jcenter()
    }
}
