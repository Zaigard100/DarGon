// Игра: компоненты, системы, загрузка уровней, экраны.
plugins {
    id("dargon.java-conventions")
}

dependencies {
    api(project(":ecs"))
    api(project(":dgscript"))
    api(project(":level"))
    api(project(":ui"))
    api(libs.gdx.core)
    api(libs.gdx.freetype)
    api(libs.gdx.controllers.core)

    testImplementation(libs.gdx.backend.headless)
}

tasks.withType<Test>().configureEach {
    // Тесты-симуляции на настоящих уровнях игры.
    systemProperty("dargon.assets", rootProject.file("assets").absolutePath)
    inputs.dir(rootProject.file("assets/data"))
    inputs.dir(rootProject.file("assets/packs"))
}

// Версия игры в коде (на Android у jar нет манифеста с версией).
val gameVersion = providers.gradleProperty("dargon.version")
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val out = layout.buildDirectory.dir("generated/sources/buildinfo/java")
    val version = gameVersion
    inputs.property("version", version)
    outputs.dir(out)
    doLast {
        val file = out.get().file("com/zaig100/dargon/BuildInfo.java").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.zaig100.dargon;
            |
            |/** Сведения о сборке (генерируется Gradle). */
            |public final class BuildInfo {
            |    /** Версия игры. */
            |    public static final String VERSION = "${version.get()}";
            |
            |    private BuildInfo() {}
            |}
            |""".trimMargin(),
        )
    }
}
sourceSets.main {
    java.srcDir(generateBuildInfo)
}
