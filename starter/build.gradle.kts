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
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    api("jakarta.annotation:jakarta.annotation-api")
    api("org.apache.sshd:sshd-core")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.shell:spring-shell-starter")
    api("org.springframework.shell:spring-shell-jline")
    compileOnly("tools.jackson.core:jackson-databind")
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")
    compileOnly("org.springframework.boot:spring-boot-session")
    compileOnly("org.springframework.boot:spring-boot-starter-security")

    testAnnotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-session")
    testImplementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security")
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

val signingKeyId = providers.gradleProperty("signing.keyId")
val signingPassword = providers.gradleProperty("signing.password")
val signingSecretKeyRingFile = providers.gradleProperty("signing.secretKeyRingFile")

signing {
    if (signingKeyId.isPresent && signingPassword.isPresent && signingSecretKeyRingFile.isPresent) {
        sign(publishing.publications["mavenJava"])
    }
}
