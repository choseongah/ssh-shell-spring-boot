plugins {
    id("org.springframework.boot")
}

description = "Ssh shell spring boot basic sample"

val starterCoordinates = "${project.group}:ssh-shell-spring-boot-starter:${project.version}"

base {
    archivesName.set("ssh-shell-spring-boot-basic-sample")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(starterCoordinates)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module(starterCoordinates)).using(project(":ssh-shell-spring-boot-starter"))
    }
}

val replacements = mapOf(
    $$"${project.groupId}" to project.group.toString(),
    $$"${project.artifactId}" to "ssh-shell-spring-boot-basic-sample",
    $$"${project.version}" to project.version.toString(),
)

tasks.processResources {
    filteringCharset = "UTF-8"
    inputs.properties(replacements)
    filesMatching(listOf("**/*.yml", "**/*.txt")) {
        filter { line ->
            replacements.entries.fold(line) { current, (placeholder, value) ->
                current.replace(placeholder, value)
            }
        }
    }
}
