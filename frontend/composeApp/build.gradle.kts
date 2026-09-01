import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sentryKmp)
    alias(libs.plugins.kotlinCocoapods)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

val versionProps = Properties()
val versionPropsFile = rootProject.file("version.properties")
if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
} else {
    versionProps["VERSION_NAME"] = "0.0.0"
    versionProps["VERSION_CODE"] = "0"
}

val generateAppVersion by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/version")
    val versionName = versionProps["VERSION_NAME"] as String
    val versionCode = (versionProps["VERSION_CODE"] as String).toInt()
    outputs.dir(outputDir)
    doLast {
        val src = outputDir.get().file("AppVersion.kt").asFile
        src.parentFile.mkdirs()
        src.writeText(
            """
object AppVersion {
    const val VERSION_NAME = "$versionName"
    const val VERSION_CODE = $versionCode
}
""".trimIndent()
        )
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    // 1. Android Target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // 2. iOS Targets (Simulator, Device, Mac)
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // 3. Web Target (JS) — uses Kotlin/JS with the IR compiler backend, targeting browsers.
    //    The ktor-client-js engine (jsMain dependency below) handles HTTP on the JS target.
    //    Output is a static webpack bundle (index.html + composeApp.js) deployable to any
    //    static host (Cloudflare Pages, etc.).
    js(IR) {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        // Shared by the Android and iOS targets only. The Sentry KMP SDK has no JS variant, so it
        // must not live in commonMain (the JS target couldn't resolve it) — SentryMonitoring and
        // its dependency live here, inherited by androidMain and iosMain.
        val androidIosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.sentry.kmp)
            }
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        val androidMain by getting {
            dependsOn(androidIosMain)
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
            }
        }

        val iosMain by getting {
            dependsOn(androidIosMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(npm("@sentry/browser", "8.36.0"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
    }

    // CocoaPods integration so the iOS framework links Sentry's native Cocoa SDK. The Sentry KMP
    // Gradle plugin injects pod("Sentry") into this config automatically. Building the iOS framework
    // (embedAndSignAppleFrameworkForXcode) will trigger `pod install`.
    cocoapods {
        version = "1.0.0"
        summary = "budge-yet shared KMP module"
        homepage = "https://github.com/imhx/budge-yet"
        ios.deploymentTarget = "17.0"
    }
}

// Sentry KMP Gradle plugin. The JS target is not supported by the Sentry KMP SDK, so auto-install
// into commonMain would fail — the SDK dependency is added manually to the androidIosMain source set
// (libs.sentry.kmp) and the JS target gets @sentry/browser instead. CocoaPods auto-install is
// disabled because the iOS app links Sentry's native Cocoa SDK via Swift Package Manager in the
// Xcode project (see iosApp/iosApp.xcodeproj), not via a Podfile workspace.
sentryKmp {
    autoInstall {
        commonMain {
            enabled = false
        }
        cocoapods {
            enabled = false
        }
    }
}

tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(tasks.named("generateAppVersion"))
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(layout.buildDirectory.dir("generated/version"))

android {
    namespace = "com.budgeyet"
    compileSdk = 36

    defaultConfig {
            applicationId = "com.imhx.budgeyet"
            minSdk = 24
            targetSdk = 36
            versionCode = (versionProps["VERSION_CODE"] as String).toInt()
            versionName = versionProps["VERSION_NAME"] as String
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

compose {
    resources {
        packageOfResClass = "com.budgeyet.generated.resources"
    }
}
