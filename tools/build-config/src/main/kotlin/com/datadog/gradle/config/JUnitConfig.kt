/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider

internal fun Project.junitConfig() {
    // Use the same Mockito version as the tests, and attach it at startup for JDK 21+.
    val mockitoAgent = configurations.named("testRuntimeClasspath").flatMap { configuration ->
        configuration.incoming.artifacts.resolvedArtifacts.map { artifacts ->
            artifacts.filter { artifact ->
                val component = artifact.id.componentIdentifier
                component is ModuleComponentIdentifier &&
                    component.group == "org.mockito" && component.module == "mockito-core"
            }.map { it.file }
        }
    }
    val mockitoAgentFiles = files(mockitoAgent)
    tasks.withType<Test> {
        jvmArgs(
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }
    }
    // Kover adds its agent after evaluation. Mockito must follow it so inline mocks can retransform classes.
    afterEvaluate {
        tasks.withType<Test> {
            jvmArgumentProviders.add(MockitoAgentArgumentProvider(mockitoAgentFiles))
        }
    }
}

internal class MockitoAgentArgumentProvider(
    @get:Classpath val agentJar: FileCollection
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-javaagent:${agentJar.singleFile.absolutePath}")
}
