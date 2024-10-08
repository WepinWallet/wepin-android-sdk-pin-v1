pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // <= Add JitPack Repository
    }
}

rootProject.name = "Wepin SDK V1 Sample"
include(":app")
include(":pinlib")
//include(":loginlib")
