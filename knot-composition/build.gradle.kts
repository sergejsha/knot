plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.gradle.signing")
    id("org.jetbrains.dokka") version Deps.dokka
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {

    implementation(project(":knot"))
    implementation(kotlin(Deps.kotlinJdk))
    implementation(Deps.rxJava)

    testImplementation(Deps.junit)
    testImplementation(Deps.truth)
    testImplementation(Deps.mockito)
    testImplementation(Deps.mockitoKotlin)
}

publishing {

    repositories {
        maven {
            name = "local"
            url = uri("$buildDir/repository")
        }
        maven {
            name = "central"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = project.findProperty("NEXUS_USERNAME") as? String ?: ""
                password = project.findProperty("NEXUS_PASSWORD") as? String ?: ""
            }
        }
    }

    val dokka by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
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
        create("Composition", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("Composition")
                description.set("Composition extension for Knot")
                url.set("http://www.halfbit.de")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("beworker")
                        name.set("Sergej Shafarenka")
                        email.set("info@halfbit.de")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:beworker/knot.git")
                    developerConnection.set("scm:git:ssh://github.com:beworker/knot.git")
                    url.set("http://www.halfbit.de")
                }
            }
        }
    }

}

if (project.hasProperty("signing.keyId")) {
    signing {
        sign(publishing.publications["Composition"])
    }
}
