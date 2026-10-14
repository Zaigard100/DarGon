plugins {
    id("dargon.java-conventions")
    application
}

// Проверка данных игры: схемы, уровни, скрипты, локализация. Запускается в CI.
//   ./gradlew :tools:level-check:run                       — проверить assets
//   ./gradlew :tools:level-check:run --args="assets --write-schema" — перегенерировать level.schema.json
dependencies {
    implementation(project(":level"))
    implementation(libs.json.schema.validator)
    runtimeOnly(libs.slf4j.nop)
}

application {
    mainClass = "com.zaig100.dargon.tools.levelcheck.LevelCheck"
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
    args("assets") // --args заменяет этот список
}

// Проверка ассетов — часть `check`. Занимает около секунды, поэтому выполняется каждый раз.
val checkAssets = tasks.register<JavaExec>("checkAssets") {
    group = "verification"
    description = "Проверяет уровни, префабы, предметы и локализацию в assets/."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.zaig100.dargon.tools.levelcheck.LevelCheck"
    workingDir = rootDir
    args("assets")
}

// Пак-пример examples/workshop проверяется так же, как пак игрока.
val checkExamples = tasks.register<JavaExec>("checkExamples") {
    group = "verification"
    description = "Проверяет пак-пример examples/workshop."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.zaig100.dargon.tools.levelcheck.LevelCheck"
    workingDir = rootDir
    args("assets", "--pack", "examples/workshop")
}

tasks.named("check") { dependsOn(checkAssets, checkExamples) }
