// Лаунчер для Android.
import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}

val natives: Configuration = configurations.create("natives")

android {
    namespace = "com.zaig100.dargon.android"
    compileSdk = 37

    defaultConfig {
        // Тот же applicationId, что у DarGon3: новая версия ставится поверх старой.
        applicationId = "com.zaig100.dg"
        minSdk = 21
        targetSdk = 37
        versionCode = 400
        versionName = providers.gradleProperty("dargon.version").get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.util.function, java.time и т.п. есть в Android только с API 24–26; minSdk — 21.
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets {
        getByName("main") {
            assets.directories.add(rootProject.file("assets").path)
            jniLibs.directories.add(layout.buildDirectory.dir("gdx-natives/lib").get().asFile.path)
        }
    }

    // Ключ подписи — вне репозитория: ~/.config/dargon/keys/signing.properties (или -Pdargon.signing=путь).
    val signingFile =
        file(
            providers.gradleProperty("dargon.signing").orNull
                ?: "${System.getProperty("user.home")}/.config/dargon/keys/signing.properties",
        )
    val signing = Properties()
    if (signingFile.exists()) {
        signingFile.inputStream().use { signing.load(it) }
        signingConfigs.create("release") {
            storeFile = file(signing.getProperty("storeFile"))
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            if (signingFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":core"))
    implementation(libs.gdx.backend.android)
    implementation(libs.gdx.controllers.android)

    for (abi in listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")) {
        natives(variantOf(libs.gdx.platform) { classifier("natives-$abi") })
        natives(variantOf(libs.gdx.freetype.platform) { classifier("natives-$abi") })
    }
}

// Распаковывает нативные .so libGDX в build/gdx-natives/lib/<abi>.
val copyAndroidNatives = tasks.register<ExtractGdxNatives>("copyAndroidNatives") {
    nativeJars.from(natives)
    outputDir = layout.buildDirectory.dir("gdx-natives/lib")
}

tasks.named("preBuild") {
    dependsOn(copyAndroidNatives, rootProject.tasks.named("packTextures"))
}
