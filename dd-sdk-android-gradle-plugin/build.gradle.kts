/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.MavenConfig
import com.datadog.gradle.config.dependencyUpdateConfig
import com.datadog.gradle.config.jacocoConfig
import com.datadog.gradle.config.javadocConfig
import com.datadog.gradle.config.junitConfig
import com.datadog.gradle.config.kotlinConfig
import com.datadog.gradle.config.publishingConfig

plugins {
    // Build
    id("java-gradle-plugin")
    kotlin("jvm")
    kotlin("kapt")

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka-javadoc")
    alias(libs.plugins.gradlePluginPublish)
    id("com.vanniktech.maven.publish.base")

    // Analysis tools
    id("com.github.ben-manes.versions")

    // Tests
    jacoco

    // Internal Generation
    id("thirdPartyLicences")
    id("transitiveDependencies")
}

dependencies {
    // Main implementation dependencies
    implementation(libs.kotlin)
    implementation(libs.okHttp)
    implementation(libs.json)

    // Test dependencies
    testImplementation(libs.bundles.jUnit5)
    testImplementation(libs.bundles.testTools)
    testImplementation(libs.okHttpMock)
    testImplementation(libs.androidToolsPluginGradle)
    testImplementation(libs.kotlinCompilerEmbeddable)
    testImplementation(libs.kotlinCompilerTesting20)
    testImplementation(libs.kotlinPluginGradle)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui)

    // KCP module dependencies for tests (from composite builds)
    testImplementation("com.datadoghq:dd-kcp-common:${property("pluginVersion")}")
    testImplementation("com.datadoghq:dd-kcp-kotlin20:${property("pluginVersion")}")
    testImplementation("com.datadoghq:dd-kcp-kotlin21:${property("pluginVersion")}")
    testImplementation("com.datadoghq:dd-kcp-kotlin22:${property("pluginVersion")}")

    // Compile-only dependencies
    compileOnly(libs.androidToolsPluginGradle) // for auto-wiring into Android projects
    compileOnly(libs.kotlinCompilerEmbeddable)
    compileOnly(libs.kotlinPluginGradle)
    compileOnly("com.datadoghq:dd-kcp-common:${property("pluginVersion")}")
    compileOnly("com.datadoghq:dd-kcp-kotlin20:${property("pluginVersion")}")
    compileOnly("com.datadoghq:dd-kcp-kotlin21:${property("pluginVersion")}")
    compileOnly("com.datadoghq:dd-kcp-kotlin22:${property("pluginVersion")}")
    compileOnly(libs.autoServiceAnnotation)
    kapt(libs.autoService)
}

kotlinConfig()
junitConfig()
jacocoConfig()
javadocConfig()
dependencyUpdateConfig()
publishingConfig("Plugin to upload Proguard/R8 mapping files to Datadog.")

gradlePlugin {

    website.set("https://docs.datadoghq.com/real_user_monitoring/error_tracking/android/")
    vcsUrl.set("https://github.com/DataDog/dd-sdk-android-gradle-plugin")

    plugins {
        register("dd-sdk-android-gradle-plugin") {
            description = "This plugin is used to upload your Proguard/R8 mapping files to Datadog."
            id = "com.datadoghq.dd-sdk-android-gradle-plugin" // the alias
            implementationClass = "com.datadog.gradle.plugin.DdAndroidGradlePlugin"
            displayName = "Gradle Plugin for Datadog Android SDK"
            version = MavenConfig.VERSION.name
            tags.set(listOf("android", "datadog", "mapping", "crash-report", "proguard", "R8"))
        }
    }
}

// TODO RUMM-3257 Target Java 17 bytecode at some point
java {
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<Test> {
    dependsOn("pluginUnderTestMetadata")
}

// Bundle KCP module classes into the main plugin JAR
// This ensures all version-specific classes are available at runtime
val pluginVersion: String by project
tasks.withType<Jar>().configureEach {
    dependsOn(
        gradle.includedBuild("dd-kcp-common").task(":jar"),
        gradle.includedBuild("dd-kcp-kotlin20").task(":jar"),
        gradle.includedBuild("dd-kcp-kotlin21").task(":jar"),
        gradle.includedBuild("dd-kcp-kotlin22").task(":jar")
    )
    from(zipTree("dd-kcp-common/build/libs/dd-kcp-common-$pluginVersion.jar"))
    from(zipTree("dd-kcp-kotlin20/build/libs/dd-kcp-kotlin20-$pluginVersion.jar"))
    from(zipTree("dd-kcp-kotlin21/build/libs/dd-kcp-kotlin21-$pluginVersion.jar"))
    from(zipTree("dd-kcp-kotlin22/build/libs/dd-kcp-kotlin22-$pluginVersion.jar"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Run all tests including KCP module tests from composite builds
tasks.register("allTests") {
    description = "Runs all tests including KCP module tests."
    group = "verification"
    val testTask = ":test"
    dependsOn("test")
    dependsOn(gradle.includedBuild("dd-kcp-kotlin20").task(testTask))
    dependsOn(gradle.includedBuild("dd-kcp-kotlin21").task(testTask))
    dependsOn(gradle.includedBuild("dd-kcp-kotlin22").task(testTask))
}
