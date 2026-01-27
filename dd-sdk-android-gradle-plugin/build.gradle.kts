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

// Creating the source sets for different Kotlin versions for compiler plugin compatibility.
val common: SourceSet by sourceSets.creating
// Kotlin 2.0.x versions
val kotlin20: SourceSet by sourceSets.creating
val kotlin20Test: SourceSet by sourceSets.creating
// Kotlin 2.1.x versions
val kotlin21: SourceSet by sourceSets.creating
val kotlin21Test: SourceSet by sourceSets.creating
// Kotlin 2.2.x versions
val kotlin22: SourceSet by sourceSets.creating
val kotlin22Test: SourceSet by sourceSets.creating

configurations {
    listOf("kotlin20TestImplementation", "kotlin21TestImplementation", "kotlin22TestImplementation").forEach {
        named(it) {
            extendsFrom(configurations["testImplementation"], configurations["implementation"])
        }
    }
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

    // Test source set outputs
    testImplementation(kotlin20.output)
    testImplementation(kotlin21.output)
    testImplementation(kotlin22.output)
    testImplementation(common.output)

    // Compile-only dependencies
    compileOnly(libs.androidToolsPluginGradle) // for auto-wiring into Android projects
    compileOnly(libs.kotlinCompilerEmbeddable)
    compileOnly(libs.kotlinPluginGradle)
    compileOnly(kotlin20.output)
    compileOnly(kotlin21.output)
    compileOnly(kotlin22.output)
    compileOnly(common.output)
    compileOnly(libs.autoServiceAnnotation)
    kapt(libs.autoService)

    // Common source set
    common.compileOnlyConfigurationName(libs.kotlinCompilerEmbeddable)

    // Kotlin 2.2.x source set
    with(kotlin22) {
        compileOnlyConfigurationName(libs.kotlinReflect)
        compileOnlyConfigurationName(libs.kotlinCompilerEmbeddable22)
        compileOnlyConfigurationName(common.output)
    }

    // Kotlin 2.1.x source set
    with(kotlin21) {
        compileOnlyConfigurationName(libs.kotlinReflect)
        compileOnlyConfigurationName(libs.kotlinCompilerEmbeddable21)
        compileOnlyConfigurationName(common.output)
    }

    // Kotlin 2.0.x source set
    with(kotlin20) {
        compileOnlyConfigurationName(libs.kotlinReflect)
        compileOnlyConfigurationName(libs.kotlinCompilerEmbeddable20)
        compileOnlyConfigurationName(common.output)
    }
    val testSourceVersions = listOf("20", "21", "22")

    testSourceVersions.forEach { version ->
        val sourceSetName = "kotlin$version"
        val testImpl = "${sourceSetName}TestImplementation"

        val compilerEmbeddableLib = when (version) {
            "20" -> libs.kotlinCompilerEmbeddable20
            "21" -> libs.kotlinCompilerEmbeddable21
            "22" -> libs.kotlinCompilerEmbeddable22
            else -> error("No embeddable lib for version $version")
        }

        val compilerTestingLib = when (version) {
            "20" -> libs.kotlinCompilerTesting20
            "21" -> libs.kotlinCompilerTesting21
            "22" -> libs.kotlinCompilerTesting22
            else -> error("No testing lib for version $version")
        }

        dependencies {
            add(testImpl, libs.bundles.jUnit5)
            add(testImpl, compilerTestingLib)
            add(testImpl, compilerEmbeddableLib)
            add(testImpl, sourceSets["test"].output)
            add(testImpl, sourceSets["main"].output)
            add(testImpl, sourceSets[sourceSetName].output)
        }
    }
}

kotlinConfig()
junitConfig()
jacocoConfig()
javadocConfig()
dependencyUpdateConfig()
publishingConfig("Plugin to upload Proguard/R8 mapping files to Datadog.")

tasks.withType<Jar>().configureEach {
    from(kotlin20.output)
    from(kotlin21.output)
    from(kotlin22.output)
    from(common.output)
}

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

listOf(
    "kotlin20" to kotlin20Test,
    "kotlin21" to kotlin21Test,
    "kotlin22" to kotlin22Test
).forEach { (name, sourceSet) ->
    tasks.register<Test>("test${name.replaceFirstChar { it.uppercaseChar() }}") {
        group = "verification"
        description = "Runs tests for ${name.replace("kotlin", "Kotlin ")}.x source set."
        testClassesDirs = sourceSet.output.classesDirs
        classpath = sourceSet.runtimeClasspath
        useJUnitPlatform()
    }
}

tasks.register("allTests") {
    dependsOn("test", "testKotlin20", "testKotlin21", "testKotlin22")
}
