/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import com.datadog.gradle.utils.Version
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.findByType
import org.gradle.plugins.signing.SigningExtension

object MavenConfig {

    val VERSION = Version(1, 23, 0, Version.Type.Snapshot)
    const val GROUP_ID = "com.datadoghq"
    const val PLUGIN_PUBLICATION = "pluginMaven"

    // should be aligned with com.vanniktech.maven.publish.Platform.PUBLICATION_NAME
    const val KCP_PUBLICATION = "maven"
}

fun Project.publishingConfig(projectDescription: String, publishAsGradlePlugin: Boolean = true) {
    val projectName = name
    val signingExtension = extensions.findByType(SigningExtension::class)
    project.group = MavenConfig.GROUP_ID
    project.version = MavenConfig.VERSION.name

    if (signingExtension == null) {
        logger.error("Missing signing extension for $projectName")
        return
    }
    signingExtension.apply {
        val privateKey = System.getenv("GPG_PRIVATE_KEY")
        val password = System.getenv("GPG_PASSWORD")
        isRequired = System.getenv("CI").toBoolean() && !hasProperty("dd-skip-signing")
        useInMemoryPgpKeys(privateKey, password)
        // com.gradle.plugin-publish plugin will automatically add signing task "signPluginMavenPublication"
        // for KCP modules we need to do it manually
        if (!publishAsGradlePlugin) {
            afterEvaluate {
                extensions.findByType<PublishingExtension>()?.apply {
                    sign(publications.getByName(MavenConfig.KCP_PUBLICATION))
                }
            }
        }
    }

    val mavenPublishing = extensions.findByType<MavenPublishBaseExtension>()
    if (mavenPublishing == null) {
        logger.error("Missing Maven publishing extension for $projectName")
        return
    }

    mavenPublishing.apply {
        configureBasedOnAppliedPlugins()
    }

    afterEvaluate {
        if (publishAsGradlePlugin) {
            tasks.named("javadocJar", Jar::class.java).configure {
                group = "publishing"
                dependsOn("dokkaGenerate")
                archiveClassifier.convention("javadoc")
                from("${layout.buildDirectory.dir("/reports/javadoc")}")
            }
        }

        val publishingExtension = extensions.findByType<PublishingExtension>()
        if (publishingExtension == null) {
            logger.error("Missing publishing extension for $projectName")
            return@afterEvaluate
        }

        publishingExtension.apply {
            val publicationName = if (publishAsGradlePlugin) {
                MavenConfig.PLUGIN_PUBLICATION
            } else {
                MavenConfig.KCP_PUBLICATION
            }
            val mavenPublication = publications.getByName(publicationName) as MavenPublication

            mavenPublication.apply {
                groupId = MavenConfig.GROUP_ID
                artifactId = projectName
                version = MavenConfig.VERSION.name

                pom {
                    name.set(projectName)
                    description.set(projectDescription)
                    url.set("https://github.com/DataDog/dd-sdk-android-gradle-plugin/")

                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    organization {
                        name.set("Datadog")
                        url.set("https://www.datadoghq.com/")
                    }
                    developers {
                        developer {
                            name.set("Datadog")
                            email.set("info@datadoghq.com")
                            organization.set("Datadog")
                            organizationUrl.set("https://www.datadoghq.com/")
                        }
                    }

                    scm {
                        url.set("https://github.com/DataDog/dd-sdk-android-gradle-plugin/")
                        connection.set(
                            "scm:git:git@github.com:Datadog/dd-sdk-android-gradle-plugin.git"
                        )
                        developerConnection.set(
                            "scm:git:git@github.com:Datadog/dd-sdk-android-gradle-plugin.git"
                        )
                    }
                }
            }
        }

        // should be in afterEvaluate
        mavenPublishing.publishToMavenCentral(automaticRelease = false)
    }
}
