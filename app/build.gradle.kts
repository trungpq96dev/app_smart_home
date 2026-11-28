import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}


// Khoa ky ban release doc tu keystore.properties o thu muc goc.
// File do va file .jks KHONG nam trong git (xem .gitignore).
// May nao khong co file do thi release van build duoc, ky bang khoa debug -
// de du an khong chet build tren may khac.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasReleaseKey = keystorePropsFile.exists()

android {
    namespace = "vn.smarthome"
    compileSdk = 35

    defaultConfig {
        applicationId = "vn.smarthome"
        minSdk = 26
        targetSdk = 35
        versionCode = 32
        versionName = "0.9.10"
        resourceConfigurations += listOf("vi", "en")
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)

    // Khai bao tuong minh vi code dung Dispatchers.Main cua Android. De no lot
    // vao qua duong phu thuoc bac hai thi de vo khi nang phien ban Compose.
    implementation(libs.kotlinx.coroutines.android)

    // MQTT. Dung ban Java thuan, KHONG dung Paho Android Service (da bo hoang).
    // Phan tu ket noi lai tu viet trong MqttDien.kt.
    implementation(libs.paho.mqtt)

    // WebSocket cho Gemini Live. Day la thu vien HTTP dau tien cua du an -
    // README truoc do ghi "khong OkHttp", cau do khong con dung tu 20/11/2026.
    // Vi sao phai them: Android KHONG co client WebSocket nao san. HttpURLConnection
    // (dang dung o OpenMeteo/Go2rtc/OtaManager) khong noi duoc WebSocket, va
    // java.net.http.WebSocket chi co tu API 34 - minSdk cua app la 26.
    implementation(libs.okhttp)

    // Kiem thu khoi dong chay tren JVM (Robolectric) - khong can may that.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
