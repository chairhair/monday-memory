rootProject.name = "monday-backend"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
    }
}


dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral() // you can still keep this for everything else
    }
}

includeBuild("./libs/shared-monday-memory-be-library")