import org.gradle.api.Project

object Pom {
    const val url = "http://www.halfbit.de"

    object License {
        const val name = "The Apache License, Version 2.0"
        const val url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    }

    object Developer {
        const val id = "beworker"
        const val name = "Sergej Shafarenka"
        const val email = "info@halfbit.de"
    }

    object Github {
        const val cloneUrl = "scm:git:ssh://github.com:beworker/knot.git"
        const val url = "https://github.com/beworker/knot"
    }

    object MavenCentral {
        const val name = "central"
        const val url = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    }
}

fun Project.getNexusUser() = this.findProperty("NEXUS_USERNAME") as? String ?: ""
fun Project.getNexusPassword() = this.findProperty("NEXUS_PASSWORD") as? String ?: ""
fun Project.hasSigningKey() = this.hasProperty("signing.keyId")