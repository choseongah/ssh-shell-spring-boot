plugins {
    id("org.springframework.boot")
}

description = "Ssh shell spring boot basic sample"

base {
    archivesName.set("ssh-shell-spring-boot-basic-sample")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(project(":ssh-shell-spring-boot-starter"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
