rootProject.name = "languages-dialett-app"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// shared: the data, the search and every screen, in common Kotlin and Compose Multiplatform.
// androidApp: the Android application around it. An iOS app would go in iosApp/ (see iosApp/README.md).
include(":shared")
include(":androidApp")
