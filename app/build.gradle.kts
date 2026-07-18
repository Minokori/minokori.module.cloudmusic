plugins {
    alias(libs.plugins.android.application)

}
val appName = "minokori.module.cloudmusic"

android {
    namespace = appName
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = appName
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    //alibaba
    implementation("com.alibaba.fastjson2:fastjson2:2.0.61")
    implementation("com.github.Adonai:jaudiotagger:2.3.14")
    // LSposed API
    compileOnly("de.robv.android.xposed:api:82")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
