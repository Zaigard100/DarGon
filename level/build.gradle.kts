// Формат уровней: модель, разбор JSON, префабы, предметы, автотайлинг, проверки.
// Не зависит от рендера и ECS — используется игрой и CLI-утилитами.
plugins {
    id("dargon.java-conventions")
}

dependencies {
    api(libs.gdx.core)
    api(project(":dgscript"))
}

tasks.withType<Test>().configureEach {
    // Тесты на настоящих данных игры.
    systemProperty("dargon.assets", rootProject.file("assets").absolutePath)
    inputs.dir(rootProject.file("assets/data"))
    inputs.dir(rootProject.file("assets/packs"))
}
