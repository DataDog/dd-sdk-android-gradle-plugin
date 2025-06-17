/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { setUrl(com.datadog.gradle.Dependencies.Repositories.Gradle) }
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
        maven { setUrl(com.datadog.gradle.Dependencies.Repositories.Jitpack) }
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}

// Empty task defined by one of our CI pipeline which does not apply here.
tasks.register("checkGeneratedFiles") {}

tasks.register("listAllPublishedArtifactIds") {
    doLast {
        val artifactIds = rootProject.subprojects.flatMap { subproject ->
            val publishing = subproject.extensions.findByType<PublishingExtension>()
            publishing?.publications?.mapNotNull { publication ->
                if (publication is MavenPublication) {
                    publication.artifactId
                } else {
                    null
                }
            }.orEmpty()
        }
        artifactIds.forEach {
            println(it)
        }
    }
}
