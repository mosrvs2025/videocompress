import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val needleModelUrl = "https://huggingface.co/Cactus-Compute/needle3/resolve/main/needle3.cact"
val generatedAssets = layout.buildDirectory.dir("generated/needleAssets")

// The 35 MB Apache-2.0 model is fetched at build time instead of being committed.
val prepareNeedleAssets by tasks.registering {
    val outDir = generatedAssets
    val tools = rootProject.file("needle/tools.json")
    inputs.file(tools)
    outputs.dir(outDir)
    doLast {
        val dir = outDir.get().asFile.apply { mkdirs() }
        tools.copyTo(File(dir, "tools.json"), overwrite = true)
        val model = File(dir, "needle3.cact")
        if (!model.exists() || model.length() < 1_000_000) {
            val local = System.getenv("NEEDLE_MODEL_FILE")?.let(::File)
            if (local != null && local.exists()) local.copyTo(model, overwrite = true)
            else URI(needleModelUrl).toURL().openStream().use { i -> model.outputStream().use { i.copyTo(it) } }
        }
        check(model.length() > 1_000_000) { "needle3.cact download incomplete" }
    }
}

android {
    namespace = "com.mosrvs.pocketpilot"
    compileSdk = 35
    ndkVersion = "27.2.12479018"
    defaultConfig {
        applicationId = "com.mosrvs.pocketpilot"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        // The upstream Android engine we link is arm64 only.
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild { cmake { arguments += listOf("-DANDROID_STL=c++_static") } }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    sourceSets["main"].assets.srcDir(generatedAssets)
    androidResources { noCompress += "cact" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") || it.name.startsWith("lintVitalAnalyze") }
    .configureEach { dependsOn(prepareNeedleAssets) }

dependencies {
    implementation("androidx.media3:media3-transformer:1.6.1")
    implementation("androidx.media3:media3-effect:1.6.1")
    implementation("androidx.media3:media3-common:1.6.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
