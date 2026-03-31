/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(com.datadog.gradle.Dependencies.Repositories.Gradle)
        mavenLocal()
    }

    dependencies {
        classpath(libs.androidToolsPluginGradle)
        classpath(libs.kotlinPluginGradle)
        classpath(libs.dokkaPluginGradle)
        // Uncomment to use the samples
        // classpath(libs.datadogPluginGradle)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(com.datadog.gradle.Dependencies.Repositories.Jitpack)
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// Empty task defined by one of our CI pipeline which does not apply here.
tasks.register("checkGeneratedFiles") {
    childProjects.forEach { (name, _) ->
        if (name.startsWith("dd-sdk-android")) {
            dependsOn(":$name:checkTransitiveDependenciesList")
            dependsOn(":$name:checkCompilerMetadataChanges")
        }
    }
}
