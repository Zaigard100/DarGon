plugins {
    id("dargon.java-conventions")
    application
}

// Одноразовый конвертер уровней DarGon3 (legacy/levels) в новый формат (assets/packs/dargon/levels).
// Запуск: ./gradlew :tools:level-migrator:run
dependencies {
    implementation(project(":level"))
}

application {
    mainClass = "com.zaig100.dargon.tools.migrator.LevelMigrator"
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
    args("legacy/levels", "assets", "build/level-migration.md")
}
