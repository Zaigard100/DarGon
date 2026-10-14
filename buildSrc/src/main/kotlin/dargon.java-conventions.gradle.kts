// Общие настройки для всех чистых Java-модулей проекта.
plugins {
    `java-library`
    checkstyle
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

version = providers.gradleProperty("dargon.version").get()

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Собираем любым JDK 17+, байткод и API — строго Java 17.
    options.release = 17
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-processing"))
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    // Модуль без тестов не должен ронять сборку.
    failOnNoDiscoveredTests = false
}

tasks.named<Test>("test") {
    // Бенчмарки — отдельной задачей `benchmark`, в check не входят.
    useJUnitPlatform { excludeTags("benchmark") }
}

val benchmark = tasks.register<Test>("benchmark") {
    group = "verification"
    description = "Запускает бенчмарки (тесты с тегом benchmark)."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("benchmark") }
    testLogging { showStandardStreams = true }
    outputs.upToDateWhen { false }
}

checkstyle {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}
