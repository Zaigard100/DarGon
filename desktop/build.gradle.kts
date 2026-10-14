// Лаунчер для Windows / Linux / macOS и сборка дистрибутивов (Construo: игра + урезанная Java).
import io.github.fourlastor.construo.Target

plugins {
    id("dargon.java-conventions")
    application
    alias(libs.plugins.construo)
}

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.backend.lwjgl3)
    implementation(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    implementation(variantOf(libs.gdx.freetype.platform) { classifier("natives-desktop") })
    implementation(libs.gdx.controllers.desktop)
}

application {
    mainClass = "com.zaig100.dargon.desktop.DesktopLauncher"
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

sourceSets.main {
    resources.srcDir(rootProject.file("assets"))
}

tasks.named("processResources") {
    dependsOn(rootProject.tasks.named("packTextures"))
}

tasks.named<JavaExec>("run") {
    isIgnoreExitValue = true
}

val gameVersion = providers.gradleProperty("dargon.version").get()

// Один jar с игрой, ассетами и всеми зависимостями — основа дистрибутивов.
val distJar = tasks.register<Jar>("distJar") {
    group = "distribution"
    description = "Собирает исполняемый jar со всеми зависимостями и ассетами."
    archiveBaseName = "dargon"
    archiveVersion = gameVersion
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Title" to "DarGon",
            "Implementation-Version" to gameVersion,
        )
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/INDEX.LIST", "module-info.class")
}

construo {
    name = "dargon"
    humanName = "DarGon"
    jarTask = distJar.name
    outputDir = rootProject.layout.buildDirectory.dir("dist")
    zipFolder = "DarGon-$gameVersion"
    jlink {
        // LWJGL пользуется sun.misc.Unsafe.
        modules.addAll("jdk.unsupported")
    }
    targets {
        create<Target.Linux>("linuxX64") {
            architecture = Target.Architecture.X86_64
            jdkUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz"
            jdkSha256 = "ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94"
        }
        create<Target.Windows>("winX64") {
            architecture = Target.Architecture.X86_64
            jdkUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip"
            jdkSha256 = "f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e"
            icon = rootProject.file("assets/icons/icon.png")
        }
    }
}
