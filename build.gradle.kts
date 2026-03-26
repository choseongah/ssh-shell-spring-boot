import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("org.sonarqube") version "7.2.3.7755"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("org.springframework.boot") version "4.0.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "io.github.choseongah"

val springBootVersion: String by project
val springShellVersion: String by project
val sshdVersion: String by project
val jacksonVersion: String by project
val lombokVersion: String by project

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")
    apply(plugin = "io.spring.dependency-management")

    group = rootProject.group
    version = rootProject.version

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.14"
    }

    extensions.configure<DependencyManagementExtension> {
        imports {
            mavenBom("tools.jackson:jackson-bom:$jacksonVersion")
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
        dependencies {
            dependency("org.apache.sshd:sshd-core:$sshdVersion")
            dependency("org.springframework.shell:spring-shell-starter:$springShellVersion")
            dependency("org.springframework.shell:spring-shell-jline:$springShellVersion")
            dependency("org.awaitility:awaitility:4.3.0")
            dependency("com.jcraft:jsch:0.1.55")
        }
    }

    dependencies {
        add("compileOnly", "org.projectlombok:lombok:$lombokVersion")
        add("annotationProcessor", "org.projectlombok:lombok:$lombokVersion")
        add("annotationProcessor", "org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
        add("testCompileOnly", "org.projectlombok:lombok:$lombokVersion")
        add("testAnnotationProcessor", "org.projectlombok:lombok:$lombokVersion")
        add("testAnnotationProcessor", "org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
                    .get()
                    .asFile
                    .absolutePath
            )
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "choseongah_ssh-shell-spring-boot")
        property("sonar.organization", "choseongah")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

listOf("sonar", "sonarqube").forEach { taskName ->
    tasks.named(taskName) {
        dependsOn(subprojects.map { "${it.path}:jacocoTestReport" })
    }
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
