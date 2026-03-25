pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "ssh-shell-spring-boot"

include("ssh-shell-spring-boot-starter")
project(":ssh-shell-spring-boot-starter").projectDir = file("starter")

include("ssh-shell-spring-boot-basic-sample")
project(":ssh-shell-spring-boot-basic-sample").projectDir = file("samples/basic")

include("ssh-shell-spring-boot-complete-sample")
project(":ssh-shell-spring-boot-complete-sample").projectDir = file("samples/complete")
