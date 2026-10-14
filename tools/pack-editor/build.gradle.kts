import io.github.fourlastor.construo.Target

plugins {
    id("dargon.java-conventions")
    application
    alias(libs.plugins.construo)
}

// Редактор паков для ПК (Linux, Windows): уровни, тексты, свойства пака, проверка, пробный запуск
// игры и экспорт в .dgpack. Рисует уровни тем же рендерером, что и игра.
//   ./gradlew :tools:pack-editor:run                         — открыть последний проект
//   ./gradlew :tools:pack-editor:run --args="<каталог пака>" — открыть пак
dependencies {
    // Игра целиком: ассеты, рендерер, бэкенд LWJGL3 и лаунчер для пробного запуска.
    implementation(project(":desktop"))
    implementation(project(":core"))
    implementation(libs.gdx.backend.lwjgl3)
    implementation(libs.visui)
}

application {
    mainClass = "com.zaig100.dargon.editor.EditorLauncher"
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

tasks.named<JavaExec>("run") {
    isIgnoreExitValue = true
}

tasks.withType<Test>().configureEach {
    systemProperty("dargon.assets", rootProject.file("assets").absolutePath)
}

val gameVersion = providers.gradleProperty("dargon.version").get()

// Один jar: редактор, игра (для пробного запуска), ассеты и зависимости.
val distJar = tasks.register<Jar>("distJar") {
    group = "distribution"
    description = "Собирает исполняемый jar редактора со всеми зависимостями."
    archiveBaseName = "dargon-editor"
    archiveVersion = gameVersion
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Title" to "DarGon pack editor",
            "Implementation-Version" to gameVersion,
        )
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/INDEX.LIST", "module-info.class")
}

// Дистрибутивы редактора для Linux и Windows — с той же урезанной Java, что у игры.
construo {
    name = "dargon-editor"
    humanName = "DarGon Editor"
    jarTask = distJar.name
    outputDir = rootProject.layout.buildDirectory.dir("dist")
    zipFolder = "DarGonEditor-$gameVersion"
    jlink {
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
