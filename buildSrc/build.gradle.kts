/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    @Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")
    alias(libs.plugins.versionsPluginGradle)
}

buildscript {
    repositories {
        mavenCentral()
    }
}

apply(plugin = "kotlin")

repositories {
    mavenCentral()
    google()
    maven { setUrl("https://plugins.gradle.org/m2/") }
    maven { setUrl("https://maven.google.com") }
    maven { setUrl("https://jitpack.io") }
}

dependencies {

    // Dependencies used to configure the gradle plugins
    implementation(embeddedKotlin("gradle-plugin"))
    implementation(libs.detektPluginGradle)
    implementation(libs.ktLintPluginGradle)
    implementation(libs.androidToolsPluginGradle)
    implementation(libs.versionsPluginGradle)
    implementation(libs.fuzzyWuzzy)
    implementation(libs.dokkaPluginGradle)

    // Tests
    testImplementation(libs.jUnit4)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.elmyr.core)
    testImplementation(libs.elmyr.inject)
    testImplementation(libs.elmyr.jUnit4)
    testImplementation(libs.elmyr.jvm)
}

gradlePlugin {
    plugins {
        register("thirdPartyLicences") {
            id = "thirdPartyLicences" // the alias
            implementationClass = "com.datadog.gradle.plugin.checklicenses.ThirdPartyLicensesPlugin"
        }
        register("transitiveDependencies") {
            id = "transitiveDependencies" // the alias
            implementationClass = "com.datadog.gradle.plugin.transdeps.TransitiveDependenciesPlugin"
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

tasks {
    register<Copy>("copyTestRes") {
        from("$projectDir/src/test/kotlin/com/example/model")
        into("$projectDir/src/test/resources/output")
    }

    register<Delete>("deleteTestRes") {
        delete("$projectDir/src/test/resources/output/")
    }
}

tasks.named("test") {
    dependsOn("copyTestRes")
    finalizedBy("deleteTestRes")
}
