// Декларативный слой UI поверх Scene2D.
plugins {
    id("dargon.java-conventions")
}

dependencies {
    api(libs.gdx.core)
}

dependencies {
    testImplementation(libs.gdx.backend.headless)
    testImplementation(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
}
