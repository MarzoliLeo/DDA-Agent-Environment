plugins {
    id("com.gradle.enterprise") version "3.17"
}

dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "DDA-Agent-Environment"

//include("directoryname_module")
include("pacman")
//Add every module you want here!


gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishOnFailure()
    }
}
