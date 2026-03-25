import groovy.util.Node
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `maven-publish`
    signing
}

description = "Ssh shell spring boot starter"

base {
    archivesName.set("ssh-shell-spring-boot-starter")
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-session")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-configuration-processor")
    api("jakarta.annotation:jakarta.annotation-api")
    api("tools.jackson.core:jackson-databind")
    api("org.apache.sshd:sshd-core")
    api("org.springframework.shell:spring-shell-starter")
    api("org.springframework.shell:spring-shell-jline")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.session:spring-session-jdbc")
    testImplementation("com.h2database:h2")
    testImplementation("com.jcraft:jsch")
    testImplementation("org.awaitility:awaitility")
}

val optionalDependencies = setOf(
    "org.springframework.boot:spring-boot-starter-actuator",
    "org.springframework.boot:spring-boot-session",
    "org.springframework.boot:spring-boot-starter-security",
    "org.springframework.boot:spring-boot-configuration-processor",
    "tools.jackson.core:jackson-databind",
)

fun Node.findChild(name: String): Node? =
    children()
        .filterIsInstance<Node>()
        .firstOrNull { it.name().toString() == name }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "ssh-shell-spring-boot-starter"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name.set("Ssh shell spring boot starter")
                description.set("Ssh shell for Spring Boot")
                url.set("https://github.com/choseongah/ssh-shell-spring-boot/")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/")
                    }
                }
                developers {
                    developer {
                        id.set("choseongah")
                        name.set("Cho Seong-ah")
                        email.set("choseongah@kakao.com")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:choseongah/ssh-shell-spring-boot.git")
                    developerConnection.set("scm:git:git@github.com:choseongah/ssh-shell-spring-boot.git")
                    url.set("https://github.com/choseongah/ssh-shell-spring-boot/")
                }
            }

            pom.withXml {
                val dependenciesNode = asNode().findChild("dependencies") ?: return@withXml
                dependenciesNode.children()
                    .filterIsInstance<Node>()
                    .forEach { dependencyNode ->
                        val groupId = dependencyNode.findChild("groupId")?.text()
                        val artifactId = dependencyNode.findChild("artifactId")?.text()
                        if ("$groupId:$artifactId" in optionalDependencies && dependencyNode.findChild("optional") == null) {
                            dependencyNode.appendNode("optional", "true")
                        }
                    }
            }
        }
    }
}

val signingKey = providers.gradleProperty("signingKey")
val signingPassword = providers.gradleProperty("signingPassword")

signing {
    if (signingKey.isPresent && signingPassword.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
        sign(publishing.publications["mavenJava"])
    }
}
