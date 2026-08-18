import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
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
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }


}

tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(tasks.named("generateAppVersion"))
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(layout.buildDirectory.dir("generated/version"))

android {
    namespace = "com.budgeyet"
    compileSdk = 35

    defaultConfig {
            applicationId = "com.imhx.budgeyet"
            minSdk = 24
            targetSdk = 35
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
        }
    }
}

compose {
    resources {
        packageOfResClass = "com.budgeyet.generated.resources"
    }
}
