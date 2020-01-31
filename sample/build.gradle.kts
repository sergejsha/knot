plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}

repositories {
    google()
    jcenter()
}

android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "de.halfbit.knot.sample"
        minSdkVersion(23)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = Deps.Version.jvmTarget
    }
    sourceSets.all { java.srcDir("src/$name/kotlin") }
}

dependencies {
    implementation(kotlin(Deps.kotlinJdk))
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.core:core-ktx:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")

    val lifecycleVersion = "2.2.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    kapt("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")

    implementation(project(":knot"))
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
}