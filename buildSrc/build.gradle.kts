plugins {
    `kotlin-dsl`
}

dependencies {
    // Делает типобезопасный каталог версий `libs` доступным в convention-плагинах.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
