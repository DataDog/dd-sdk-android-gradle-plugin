plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.datadoghq.dd-sdk-android-gradle-plugin")
}

repositories {
    google()
    mavenCentral()
}

android {
    compileSdkVersion = 30
    buildToolsVersion = "31.0.0"

    defaultConfig {
        applicationId "com.example.variants"
        minSdkVersion 21
        targetSdkVersion 30
        compileSdkVersion 30
        versionCode 1
        versionName "1.0"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    flavorDimensions("version", "colour")
    productFlavors {
        demo {
            dimension "version"
            applicationIdSuffix ".demo"
            versionNameSuffix "-demo"
        }
        full {
            dimension "version"
            applicationIdSuffix ".full"
            versionNameSuffix "-full"
        }
        pro {
            dimension "version"
            applicationIdSuffix ".pro"
            versionNameSuffix "-pro"
        }

        red {
            dimension "colour"
        }
        green {
            dimension "colour"
        }
        blue {
            dimension "colour"
        }
    }
}

datadog {
    checkProjectDependencies = "warn"
}

