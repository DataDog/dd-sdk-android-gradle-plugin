import com.datadog.gradle.config.AndroidConfig
import com.datadog.gradle.plugin.DdExtensionConfiguration

plugins {
    id("com.android.application")
    id("kotlin-android")
//    id("com.datadoghq.dd-sdk-android-gradle-plugin")
}


android {
    compileSdk = AndroidConfig.TARGET_SDK
    buildToolsVersion = AndroidConfig.BUILD_TOOLS_VERSION

    namespace = "com.datadog.example.variants.kotlin"
    defaultConfig {
        applicationId = "com.datadog.example.variants.kotlin"

        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }


    sourceSets.named("main") {
        java.srcDir("src/main/kotlin")
    }

    flavorDimensions.add("version")
    flavorDimensions.add("colour")
    productFlavors {
        register("demo") {
            dimension = "version"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
        }
        register("full") {
            dimension = "version"
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"
        }
        register("pro") {
            dimension = "version"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
        }

        register("red") {
            dimension = "colour"
        }
        register("green") {
            dimension = "colour"
        }
        register("blue") {
            dimension = "colour"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":samples:lib-module"))
    implementation(libs.kotlin)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

}

/*
datadog {
    site = "US"
    checkProjectDependencies = com.datadog.gradle.plugin.SdkCheckLevel.FAIL
    mappingFilePath = "path/to/mapping.txt"

    variants {
        register("demo") {
            site = "US3"
            versionName = "demo"
        }
        register("full") {
            versionName = "full"
        }
        register("pro") {
            versionName = "pro"
        }
    }
}
*/
