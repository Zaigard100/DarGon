plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.spotless)
}

spotless {
    java {
        target("*/src/**/*.java", "tools/*/src/**/*.java")
        googleJavaFormat(libs.versions.googleJavaFormat.get()).aosp()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts", "tools/*/*.gradle.kts", "buildSrc/src/**/*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ---------------------------------------------------------------------------
// Упаковка текстур: assets-raw/textures/*.png → assets/atlas/game.atlas (+ png).
// Атлас генерируется, в git не хранится.
// ---------------------------------------------------------------------------
val texturePacker: Configuration = configurations.create("texturePacker")

dependencies {
    texturePacker(libs.gdx.tools)
}

tasks.register<JavaExec>("packTextures") {
    group = "assets"
    description = "Упаковывает assets-raw/textures в атлас assets/atlas/game.atlas."
    val input = layout.projectDirectory.dir("assets-raw/textures")
    val output = layout.projectDirectory.dir("assets/atlas")
    inputs.dir(input)
    outputs.dir(output)
    classpath = texturePacker
    mainClass = "com.badlogic.gdx.tools.texturepacker.TexturePacker"
    args(input.asFile.absolutePath, output.asFile.absolutePath, "game")
    jvmArgs("-Djava.awt.headless=true")
}
