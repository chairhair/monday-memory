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

include(":shared-monday-memory-be-library")
project(":shared-monday-memory-be-library").projectDir = file("libs/shared-monday-memory-be-library")
