import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("org.springframework.boot")
}

val springBootVersion: String by project
val springShellVersion: String by project
val jacksonVersion: String by project

description = "Ssh shell spring boot complete sample"

base {
    archivesName.set("ssh-shell-spring-boot-complete-sample")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(project(":ssh-shell-spring-boot-starter"))
    implementation("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

val replacements = mapOf(
    "project.groupId" to project.group.toString(),
    "project.artifactId" to "ssh-shell-spring-boot-complete-sample",
    "project.version" to project.version.toString(),
    "spring-boot.version" to springBootVersion,
    "spring-shell.version" to springShellVersion,
    "jackson.version" to jacksonVersion,
)

tasks.processResources {
    filteringCharset = "UTF-8"
    inputs.properties(replacements)
    filesMatching(listOf("**/*.yml", "**/*.txt")) {
        filter<ReplaceTokens>(
            "tokens" to replacements,
            "beginToken" to $$"${",
            "endToken" to "}"
        )
    }
}
