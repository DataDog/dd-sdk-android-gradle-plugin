/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.utils.assertj.BuildResultAssert.Companion.assertThat
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

@ExtendWith(ForgeExtension::class)
internal class ComposeInstrumentationFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `M instrument app and library W supported Kotlin integration`(forge: Forge) {
        // Given
        val (kotlinVersion, builtInKotlin) = forge.anElementFrom(
            listOf(
                "2.3.20" to true,
                "2.4.21" to true,
                "2.3.20" to false
            )
        )
        prepareProject(kotlinVersion, datadogFirst = forge.aBool(), builtInKotlin)
        val runner = GradleRunner.create()
            .withGradleVersion("9.6.1")
            .withProjectDir(projectDir)
            .withArguments(
                ":app:compileDebugKotlin",
                ":library:compileDebugKotlin",
                "--configuration-cache",
                "--stacktrace"
            )

        // When
        val firstRun = runner.build()

        // Then: inspect compiled code, not just successful compilation or plugin configuration.
        listOf("app", "library").forEach { module ->
            assertThat(firstRun).hasSuccessfulTaskOutcome(":$module:compileDebugKotlin")
            assertThat(disassemble(module))
                .contains("instrumentedDatadog", "InstrumentedNavigationViewTrackingEffect")
        }
        val cachedRun = runner.build()
        assertThat(cachedRun).containsInOutput("Configuration cache entry reused.")
        listOf("app", "library").forEach { module ->
            assertThat(cachedRun.task(":$module:compileDebugKotlin")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }

        // Disabling instrumentation must invalidate the compilation and remove both injected calls.
        val disabledRun = runner.withArguments(runner.arguments + "-PinstrumentationMode=DISABLE").build()
        listOf("app", "library").forEach { module ->
            assertThat(disabledRun).hasSuccessfulTaskOutcome(":$module:compileDebugKotlin")
            assertThat(disassemble(module))
                .doesNotContain("instrumentedDatadog", "InstrumentedNavigationViewTrackingEffect")
        }
    }

    private fun prepareProject(kotlinVersion: String, datadogFirst: Boolean, builtInKotlin: Boolean) {
        File(projectDir, "settings.gradle.kts").writeText("include(\":app\", \":library\")\n")
        File(projectDir, "local.properties").writeText("sdk.dir=${System.getenv("ANDROID_HOME")}\n")
        // Use a separate runner only to read TestKit metadata through its public API.
        // The runner executing the fixture must not inject this classpath (Gradle #22466).
        val pluginClasspath = GradleRunner.create().withPluginClasspath().pluginClasspath
            .joinToString(",") { it.absolutePath }
        File(projectDir, "gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx1280m
            org.gradle.daemon.idletimeout=10000
            android.useAndroidX=true
            android.builtInKotlin=$builtInKotlin
            android.newDsl=$builtInKotlin
            kotlinVersion=$kotlinVersion
            datadogFirst=$datadogFirst
            pluginClasspath=$pluginClasspath
            """.trimIndent()
        )
        copyBuildScript("root.gradle.kts", File(projectDir, "build.gradle.kts"))
        listOf("app", "library").forEach { module ->
            val moduleDir = File(projectDir, module).apply { mkdirs() }
            copyBuildScript("module.gradle.kts", File(moduleDir, "build.gradle.kts"))
            val sources = File(moduleDir, "src/main/kotlin").apply { mkdirs() }
            File(moduleDir, "src/main/AndroidManifest.xml").writeText("<manifest />\n")
            File(sources, "Screen.kt").writeText(
                """
                package com.datadog.test.$module

                import androidx.compose.material3.Text
                import androidx.compose.runtime.Composable
                import androidx.navigation.compose.NavHost
                import androidx.navigation.compose.composable
                import androidx.navigation.compose.rememberNavController

                @Composable fun Screen() { Text("Integration test") }
                @Composable fun Navigation() {
                    NavHost(rememberNavController(), startDestination = "home") {
                        composable("home") { Screen() }
                    }
                }
                """.trimIndent()
            )
        }
        prepareCompilerPluginRepository(kotlinVersion)
    }

    private fun copyBuildScript(resource: String, destination: File) {
        val content = checkNotNull(javaClass.classLoader.getResource("compose_gradle_files/$resource")).readText()
        destination.writeText(content)
    }

    private fun prepareCompilerPluginRepository(kotlinVersion: String) {
        val version = BuildConfig.DD_PLUGIN_VERSION
        val compilerArtifact = if (kotlinVersion.startsWith("2.3.")) "kotlin22" else "kotlin24"
        val artifacts = checkNotNull(System.getProperty("dd.kcp.artifacts"))
            .split(File.pathSeparator)
            .map { File(it) }
        listOf("common", compilerArtifact).forEach { suffix ->
            val artifactId = "dd-sdk-android-gradle-plugin-kcp-$suffix"
            val jar = artifacts.single { it.name == "$artifactId-$version.jar" }
            val directory = File(projectDir, "compiler-plugins/com/datadoghq/$artifactId/$version")
                .apply { mkdirs() }
            jar.copyTo(File(directory, jar.name))
            val dependencies = if (suffix == "common") {
                ""
            } else {
                """
                <dependencies><dependency>
                    <groupId>com.datadoghq</groupId>
                    <artifactId>dd-sdk-android-gradle-plugin-kcp-common</artifactId>
                    <version>$version</version>
                </dependency></dependencies>
                """.trimIndent()
            }
            File(directory, "$artifactId-$version.pom").writeText(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.datadoghq</groupId>
                    <artifactId>$artifactId</artifactId>
                    <version>$version</version>
                    $dependencies
                </project>
                """.trimIndent()
            )
        }
    }

    private fun disassemble(module: String): String {
        val classFile = File(projectDir, "$module/build").walkTopDown().single { it.name == "ScreenKt.class" }
        val output = File(projectDir, "$module-bytecode.txt")
        val process = ProcessBuilder(
            File(System.getProperty("java.home"), "bin/javap").absolutePath,
            "-c",
            "-p",
            classFile.absolutePath
        ).redirectErrorStream(true).redirectOutput(output).start()
        check(process.waitFor(1, TimeUnit.MINUTES)) { "javap did not finish for $classFile" }
        check(process.exitValue() == 0) { output.readText() }
        return output.readText()
    }
}
