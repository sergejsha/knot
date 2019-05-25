plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.gradle.signing")
    id("org.jetbrains.dokka") version Deps.Version.dokka
    id("jacoco")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = Deps.Version.jvmTarget
}

dependencies {
    implementation(kotlin(Deps.kotlinJdk))
    implementation(Deps.rxJava)

    testImplementation(Deps.junit)
    testImplementation(Deps.truth)
    testImplementation(Deps.mockito)
    testImplementation(Deps.mockitoKotlin)
}

tasks {
    jacocoTestReport {
        reports {
            xml.isEnabled = true
            with(html) {
                isEnabled = true
                destination = file("$buildDir/reports/jacoco/html")
            }
        }
    }
    check {
        dependsOn(jacocoTestReport)
    }
}

publishing {

    repositories {
        maven {
            name = "local"
            url = uri("$buildDir/repository")
        }
        maven {
            name = Pom.MavenCentral.name
            url = uri(Pom.MavenCentral.url)
            credentials {
                username = project.getNexusUser()
                password = project.getNexusPassword()
            }
        }
    }

    val dokka by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
        noStdlibLink = false
    }

    val sourcesJar by tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by tasks.creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from(dokka)
    }

    publications {
        create("Knot", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("Knot")
                description.set("Reactive state container library for Kotlin")
                url.set(Pom.url)
                licenses {
                    license {
                        name.set(Pom.License.name)
                        url.set(Pom.License.url)
                    }
                }
                developers {
                    developer {
                        id.set(Pom.Developer.id)
                        name.set(Pom.Developer.name)
                        email.set(Pom.Developer.email)
                    }
                }
                scm {
                    connection.set(Pom.Github.url)
                    developerConnection.set(Pom.Github.cloneUrl)
                    url.set(Pom.Github.url)
                }
            }
        }
    }
}

if (project.hasSigningKey()) {
    signing {
        sign(publishing.publications["Knot"])
    }
}