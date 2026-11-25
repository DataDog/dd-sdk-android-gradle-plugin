/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.MavenConfig
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    // Build
    id("java-gradle-plugin")
    kotlin("jvm")

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka-javadoc")
    alias(libs.plugins.gradlePluginPublish)
    id("com.vanniktech.maven.publish.base")

    // Analysis tools
    id("ktlint")
    alias(libs.plugins.detektGradlePlugin)

    // Tests
    id("org.jetbrains.kotlinx.kover")

    // Internal Generation
    id("transitiveDependencies")
    id("compilerMetadata")
    id("datadogBuildConfig")
    alias(libs.plugins.buildConfig)
}

buildConfig {
    packageName("com.datadog.gradle.plugin")
    buildConfigField("DD_PLUGIN_VERSION", MavenConfig.VERSION.name)
}

// Resolve the locally built compiler plugins as test inputs, without adding them to the test JVM's classpath.
val compilerPluginTestArtifacts = configurations.create("compilerPluginTestArtifacts") {
    isCanBeConsumed = false
    isTransitive = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

// Check for SDK releases before deciding whether functional test results can be reused.
val functionalTestSdk = configurations.create("functionalTestSdk") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    // CI checks every run; local builds keep Gradle's default 24-hour cache.
    if (providers.environmentVariable("CI").orNull == "true") {
        resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    }
}

dependencies {
    // Main implementation dependencies
    implementation(kotlin("stdlib"))
    implementation(libs.okHttp)
    implementation(libs.json)
    // Compile-only dependencies
    compileOnly(libs.androidToolsGradlePlugin) // for auto-wiring into Android projects
    compileOnly(libs.androidToolsCommon) { isTransitive = false }
    compileOnly(libs.kotlinGradlePluginApi21)

    // Test dependencies
    testImplementation(libs.bundles.jUnit)
    testImplementation(libs.bundles.testTools)
    testImplementation(libs.okHttpMock)
    testImplementation(libs.androidToolsGradlePlugin)
    testImplementation(libs.kotlinGradlePlugin21)
    add(functionalTestSdk.name, "com.datadoghq:dd-sdk-android-rum:latest.release")

    compilerPluginTestArtifacts(project(":dd-sdk-android-gradle-plugin-kcp-common"))
    compilerPluginTestArtifacts(project(":dd-sdk-android-gradle-plugin-kcp-kotlin22"))
    compilerPluginTestArtifacts(project(":dd-sdk-android-gradle-plugin-kcp-kotlin24"))
}

datadogBuildConfig {
    applyKotlinConfig(
        languageVersion = KotlinVersion.KOTLIN_2_1,
        apiVersion = KotlinVersion.KOTLIN_2_1,
        coreLibrariesVersion = libs.versions.kotlin21.get()
    )
    applyJunitConfig()
    applyJavadocConfig()
    applyPublishingConfig("Plugin to upload Proguard/R8 mapping files to Datadog.")
    applyDetektConfig()
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

tasks.withType<Test> {
    dependsOn("pluginUnderTestMetadata")
    val sdkVersion = functionalTestSdk.incoming.resolutionResult.rootComponent.map { root ->
        val dependency = root.dependencies.single()
        if (dependency is UnresolvedDependencyResult) throw dependency.failure
        requireNotNull((dependency as ResolvedDependencyResult).selected.moduleVersion).version
    }
    inputs.property("datadogSdkVersion", sdkVersion)

    // Some Gradle versions used in functional tests (e.g. 8.0) cannot run on the toolchain JDK
    @Suppress("MagicNumber")
    val jdk17Launcher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            val jdk17Home = jdk17Launcher.get().metadata.installationPath.asFile.invariantSeparatorsPath
            listOf("-Ddd.jdk17.home=$jdk17Home", "-Ddd.sdk.version=${sdkVersion.get()}")
        }
    )
}

tasks.test {
    filter {
        excludeTestsMatching("com.datadog.gradle.plugin.ComposeInstrumentationFunctionalTest")
    }
}

val composeIntegrationTest = tasks.register<Test>("composeIntegrationTest") {
    group = "verification"
    description = "Verifies Compose instrumentation through the Datadog Gradle plugin."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter {
        includeTestsMatching("com.datadog.gradle.plugin.ComposeInstrumentationFunctionalTest")
    }
    shouldRunAfter(tasks.test)

    val compilerArtifacts = files(compilerPluginTestArtifacts)
    inputs.files(compilerArtifacts)
        .withPropertyName("compilerPluginTestArtifacts")
        .withPathSensitivity(PathSensitivity.NONE)
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            listOf("-Ddd.kcp.artifacts=${compilerArtifacts.asPath}")
        }
    )
}

tasks.check {
    dependsOn(composeIntegrationTest)
}
