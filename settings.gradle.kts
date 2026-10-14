pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "dargon"

include(
    ":ecs",
    ":dgscript",
    ":level",
    ":ui",
    ":core",
    ":desktop",
    ":android",
    ":tools:level-check",
    ":tools:level-migrator",
    ":tools:pack-editor",
)
