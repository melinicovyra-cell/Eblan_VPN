pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack removed: returns 401 for libv2ray (requires Pro subscription).
        // libv2ray is provided as a prebuilt AAR downloaded by CI into app/libs/.
    }
}

rootProject.name = "EblanVPN"
include(":app")
