import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 서명 정보: 로컬에서는 keystore.properties(gitignore 대상) 파일을,
// GitHub Actions에서는 환경 변수(RELEASE_STORE_FILE 등)를 사용한다.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun signingProp(propertyKey: String, envName: String): String? =
    keystoreProperties.getProperty(propertyKey) ?: System.getenv(envName)

val releaseStoreFile = signingProp("storeFile", "RELEASE_STORE_FILE")

android {
    namespace = "com.movealarm.app"
    // TODO(AGP 10): compileSdkVersion(String)은 AGP 10에서 제거 예정.
    // 그때 새 compileSdk { } 블록 문법으로 교체할 것.
    @Suppress("DEPRECATION")
    compileSdkVersion("android-37.2")
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.movealarm.app"
        minSdk = 26
        targetSdk = 37
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingProp("storePassword", "RELEASE_STORE_PASSWORD")
                keyAlias = signingProp("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingProp("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
