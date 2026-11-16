import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
}

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop")

    // Web (Kotlin/Wasm): ./gradlew :composeApp:wasmJsBrowserDevelopmentRun  |  …:wasmJsBrowserDistribution
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.compose.material.icons.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.multiplatform.settings)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

// ./gradlew :composeApp:desktopTest --tests "cz.pfspells.PreviewRender" -Ppreviews=true → PNG v build/previews
tasks.withType<Test>().configureEach {
    val previews = project.findProperty("previews")?.toString() ?: "false"
    systemProperty("previews", previews)
    if (previews == "true") outputs.upToDateWhen { false }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "cz.pfspells.resources"
    generateResClass = always
}

// Verze appky: -PappVersion=1.2.3 (CI ji bere z tagu v1.2.3), jinak 1.0.0. Android versionCode: -PversionCode=N.
val appVersion: String = (findProperty("appVersion") as String?)?.removePrefix("v")
    ?.takeIf { Regex("""[1-9]\d*\.\d+\.\d+""").matches(it) } ?: "1.0.0"
val appVersionCode: Int = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

// Podpis release APK: keystore z proměnných prostředí (CI secrets). Bez nich se release podepíše debug klíčem.
val releaseKeystore: File? = System.getenv("ANDROID_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "cz.pfspells"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cz.pfspells"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersion
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "cz.pfspells.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "PF Spellbook"
            packageVersion = appVersion
            description = "Pathfinder 1e spellbook & inventory"
            vendor = "PF Spellbook"
            windows {
                menu = true
                shortcut = true
                dirChooser = true
                upgradeUuid = "4f6b2c1e-8d3a-4b7e-9a51-2c0d6e9f1a73" // neměnit – umožňuje aktualizaci přes nový .msi
            }
            macOS { bundleID = "cz.pfspells" }
            linux { packageName = "pf-spellbook" }
        }
    }
}
