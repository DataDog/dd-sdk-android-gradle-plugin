/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.utils.assertj.BuildResultAssert.Companion.assertThat
import com.datadog.gradle.plugin.utils.headHash
import com.datadog.gradle.plugin.utils.initializeGit
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.io.path.Path

@Extensions(
    ExtendWith(ForgeExtension::class)
)
@ForgeConfiguration(Configurator::class)
internal class DdAndroidGradlePluginFunctionalTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var appRootDir: File
    private lateinit var libModuleRootDir: File
    private lateinit var appMainSrcDir: File
    private lateinit var appKotlinSourcesDir: File
    private lateinit var appJavaSourcesDir: File
    private lateinit var libModuleMainSrcDir: File
    private lateinit var libModuleKotlinSourcesDir: File
    private lateinit var settingsFile: File
    private lateinit var rootBuildFile: File
    private lateinit var localPropertiesFile: File
    private lateinit var appBuildGradleFile: File
    private lateinit var libModuleBuildGradleFile: File
    private lateinit var appManifestFile: File
    private lateinit var libModuleManifestFile: File
    private lateinit var gradlePropertiesFile: File
    private lateinit var sampleApplicationClassFile: File
    private lateinit var javaPlaceholderClassFile: File
    private lateinit var libModulePlaceholderFile: File

    // Native files, can be null
    private var appMainCppSourcesDir: File? = null
    private var cmakeFile: File? = null
    private var cppPlaceholderFile: File? = null


    @StringForgery(regex = "http[s]?://github\\.com:[1-9]{2}/[a-z]+/repository\\.git")
    lateinit var fakeRemoteUrl: String
    private val colors = listOf("Blue", "Green")
    private val versions = listOf("Demo", "Full")
    private val variants: List<String> by lazy {
        versions.flatMap { version ->
            colors.map {
                "${version.lowercase()}$it"
            }
        }
    }

    private lateinit var datadogCiFile: File
    private lateinit var buildVersionConfig: BuildVersionConfig

    @BeforeEach
    fun `set up`(forge: Forge) {
        appRootDir = File(testProjectDir, "samples/app").apply { mkdirs() }
        libModuleRootDir = File(testProjectDir, "samples/lib-module").apply { mkdirs() }
        rootBuildFile = File(testProjectDir, "build.gradle")
        settingsFile = File(testProjectDir, "settings.gradle")
        localPropertiesFile = File(testProjectDir, "local.properties")
        gradlePropertiesFile = File(testProjectDir, "gradle.properties")
        appMainSrcDir = File(appRootDir, "src/main").apply { mkdirs() }
        appKotlinSourcesDir = File(appMainSrcDir, "kotlin").apply { mkdirs() }
        appJavaSourcesDir = File(appMainSrcDir, "java").apply { mkdirs() }
        libModuleMainSrcDir = File(libModuleRootDir, "src/main").apply { mkdirs() }
        libModuleKotlinSourcesDir = File(libModuleMainSrcDir, "kotlin").apply { mkdirs() }
        appBuildGradleFile = File(appRootDir, "build.gradle")
        libModuleBuildGradleFile = File(libModuleRootDir, "build.gradle")
        appManifestFile = File(appMainSrcDir, "AndroidManifest.xml")
        libModuleManifestFile = File(libModuleMainSrcDir, "AndroidManifest.xml")
        sampleApplicationClassFile = File(appKotlinSourcesDir, "SampleApplication.kt")
        javaPlaceholderClassFile = File(appJavaSourcesDir, "Placeholder.java")
        libModulePlaceholderFile = File(libModuleKotlinSourcesDir, "Placeholder.kt")
        datadogCiFile = File(testProjectDir.parent, "datadog-ci.json")

        // we need to check that our plugin supports different AGP versions (backward and forward
        // compatible)
        buildVersionConfig = forge.anElementFrom(TESTED_CONFIGURATIONS)
        stubFile(rootBuildFile, ROOT_BUILD_FILE_CONTENT)
        stubFile(settingsFile, SETTINGS_FILE_CONTENT)
        stubFile(localPropertiesFile, "sdk.dir=${System.getenv("ANDROID_HOME")}")
        stubFile(sampleApplicationClassFile, APPLICATION_CLASS_CONTENT)
        stubFile(javaPlaceholderClassFile, JAVA_CLASS_CONTENT)
        stubFile(appManifestFile, APP_MANIFEST_FILE_CONTENT)
        stubFile(
            gradlePropertiesFile,
            GRADLE_PROPERTIES_FILE_CONTENT.format(
                Locale.US,
                buildVersionConfig.agpVersion,
                buildVersionConfig.buildToolsVersion,
                buildVersionConfig.targetSdkVersion,
                buildVersionConfig.kotlinVersion,
                PluginUnderTestMetadataReading.readImplementationClasspath()
                    .joinToString(",") { it.absolutePath },
                buildVersionConfig.jvmTarget
            )
        )
        stubFile(libModulePlaceholderFile, LIB_MODULE_PLACEHOLDER_CLASS_CONTENT)
        stubFile(libModuleManifestFile, LIB_MODULE_MANIFEST_FILE_CONTENT)
        stubGradleBuildFromResourceFile(
            "lib_module_build.gradle",
            libModuleBuildGradleFile
        )
        initializeGit(fakeRemoteUrl, appRootDir)
    }

    @AfterEach
    fun `tear down`() {
        if (datadogCiFile.exists()) {
            datadogCiFile.delete()
        }
    }

    // region Assemble

    @Test
    fun `M success W assembleRelease`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )
        // When
        val result = gradleRunner { withArguments("--stacktrace", ":samples:app:assembleRelease") }
            .build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleRelease")
    }

    @Test
    fun `M success W assembleRelease { project with library module }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_lib_module_attached.gradle",
            appBuildGradleFile
        )
        // When
        val result =
            gradleRunner { withArguments("--stacktrace", ":samples:app:assembleRelease") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleRelease")
    }

    @Test
    fun `M success W assembleRelease { build cache }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )

        // When
        val result =
            gradleRunner { withArguments("--build-cache", ":samples:app:assembleRelease") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleRelease")
    }

    @Test
    fun `M success W assembleDebug`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )
        // When
        val result = gradleRunner { withArguments("--stacktrace", ":samples:app:assembleDebug") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
    }

    @Test
    fun `M success W assembleDebug { project with library module }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_lib_module_attached.gradle",
            appBuildGradleFile
        )
        // When
        val result = gradleRunner { withArguments("--stacktrace", ":samples:app:assembleDebug") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
    }

    @Test
    fun `M success W assembleDebug { non default obfuscation }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_non_default_obfuscation.gradle",
            appBuildGradleFile
        )
        // When
        val result = gradleRunner { withArguments("--stacktrace", ":samples:app:assembleDebug") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
    }

    @Test
    fun `M success W assembleDebug { plugin disabled }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_plugin_disabled.gradle",
            appBuildGradleFile
        )
        // When
        val result = gradleRunner { withArguments("--info", ":samples:app:assembleDebug") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
        assertThat(result).containsInOutput("Datadog extension disabled, no upload task created")
        assertThat(result).hasNoUploadTasks()
    }

    @Test
    fun `M success W assembleDebug { build cache }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )

        // When
        val result =
            gradleRunner { withArguments("--build-cache", ":samples:app:assembleDebug") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
    }

    @Disabled(
        "This test is ignored for now because of these tasks: " +
            "collect[Flavour1][Flavour2]ReleaseDependencies. This is caused under the hood" +
            "by this task: PerModuleReportDependenciesTask which accesses the project object" +
            "inside the action method. " +
            "There is already an opened issue here: https://github.com/gradle/gradle/issues/12871"
    )
    @Test
    fun `M success W assembleRelease { configuration cache, checkProjectDependencies enabled }`() {
        // TODO: https://datadoghq.atlassian.net/browse/RUMM-1894

        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )

        // When
        val result = gradleRunner {
            withArguments(
                "--configuration-cache",
                "--stacktrace",
                ":samples:app:assembleRelease"
            )
        }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleRelease")
    }

    @Disabled(
        "This test is ignored for now as we are using the Configuration object at the " +
            "task action level in our DdCheckSdkDepsTask and this is breaking " +
            "the --configuration-cache. There is no workaround this yet and this is " +
            "also present in some internal build.gradle tasks (see the test comment above)"
    )
    @Test
    fun `M success W assembleDebug { configuration cache, checkProjectDependencies enabled }`() {
        // TODO: https://datadoghq.atlassian.net/browse/RUMM-1893

        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )

        // When
        val result = gradleRunner {
            withArguments(
                "--configuration-cache",
                "--stacktrace",
                ":samples:app:assembleDebug"
            )
        }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
    }

    @Test
    fun `M success W assembleDebug { configuration cache, checkDependencies disabled }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_check_deps_disabled.gradle",
            appBuildGradleFile
        )

        // When
        val result = gradleRunner {
            withArguments(
                "--configuration-cache",
                "--stacktrace",
                ":samples:app:assembleDebug"
            )
        }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
    }

    @Disabled(
        "This test is ignored for now because of these tasks: " +
            "collect[Flavour1][Flavour2]ReleaseDependencies. This is caused under the hood" +
            "by this task: PerModuleReportDependenciesTask which accesses the project object" +
            "inside the action method. " +
            "There is already an opened issue here: https://github.com/gradle/gradle/issues/12871"
    )
    @Test
    fun `M success W assembleRelease { configuration cache, checkDependencies disabled  }`() {
        // TODO: https://datadoghq.atlassian.net/browse/RUMM-1894

        // Given
        stubGradleBuildFromResourceFile(
            "build_with_check_deps_disabled.gradle",
            appBuildGradleFile
        )

        // When
        val result = gradleRunner {
            withArguments(
                "--configuration-cache",
                "--stacktrace",
                ":samples:app:assembleRelease"
            )
        }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleRelease")
    }

    @Test
    fun `M success W assembleRelease { Datadog SDK not in deps, checkDependencies to warn }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_check_deps_set_to_warn.gradle",
            appBuildGradleFile
        )

        // When
        val result =
            gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleRelease")
        for (it in variants) {
            assertThat(result).containsInOutput(
                "Following application variant doesn't have " +
                    "Datadog SDK included: ${it}Release"
            )
        }
    }

    @Test
    fun `M success W assembleDebug { Datadog SDK not in deps, checkDependencies to warn }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_check_deps_set_to_warn.gradle",
            appBuildGradleFile
        )

        // When
        val result = gradleRunner { withArguments("--info", ":samples:app:assembleDebug") }.build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
        for (it in variants) {
            assertThat(result).containsInOutput(
                "Following application variant doesn't have " +
                    "Datadog SDK included: ${it}Debug"
            )
        }
    }

    @Disabled("RUMM-2344")
    @Test
    fun `M fail W assembleRelease { Datadog SDK not in deps }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_without_datadog_dep.gradle",
            appBuildGradleFile
        )

        // When
        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .buildAndFail()
    }

    @Disabled("RUMM-2344")
    @Test
    fun `M fail W assembleDebug { Datadog SDK not in deps }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_without_datadog_dep.gradle",
            appBuildGradleFile
        )

        // When
        gradleRunner { withArguments("--info", ":samples:app:assembleDebug") }
            .buildAndFail()
    }

    // TODO remove once RUMM-2344 is done
    @Test
    fun `M success W assembleRelease { Datadog SDK not in deps }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_without_datadog_dep.gradle",
            appBuildGradleFile
        )

        // When
        val result = gradleRunner { withArguments("--stacktrace", ":samples:app:assembleRelease") }
            .build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleRelease")
    }

    // TODO remove once RUMM-2344 is done
    @Test
    fun `M success W assembleDebug { Datadog SDK not in deps }`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_without_datadog_dep.gradle",
            appBuildGradleFile
        )

        // When
        val result = gradleRunner { withArguments("--stacktrace", ":samples:app:assembleDebug") }
            .build()

        // Then
        assertThat(result).hasSuccessfulTaskOutcome(":samples:app:assembleDebug")
    }

    // endregion

    // region BuildId

    @Test
    fun `M inject build ID W assembleRelease`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )
        val runArguments = mutableListOf(
            "--stacktrace",
            ":samples:app:assembleRelease"
        ).apply {
            // https://issuetracker.google.com/issues/231997838
            if (buildVersionConfig.isAgpAboveOrEqual730()) {
                add("--configuration-cache")
            }
        }

        // When
        val result = gradleRunner { withArguments(runArguments) }
            .build()

        // Then
        assertThat(result.task(":samples:app:assembleRelease")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val apks = testProjectDir.walk()
            .filter { it.isFile && it.extension == "apk" }
            .map { ZipFile(it) }
            .toList()

        assertThat(apks).isNotEmpty

        val buildIdFiles = apks.mapNotNull { it.getEntry(BUILD_ID_FILE_PATH_APK) }

        // each apk should contain one build ID file
        assertThat(apks.size).isEqualTo(buildIdFiles.size)

        val buildIds = apks.map {
            it.readBuildId(BUILD_ID_FILE_PATH_APK)
                .let { UUID.fromString(it) }
        }

        // all build IDs should be unique
        assertThat(buildIds.toSet()).hasSize(apks.size)
    }

    @Test
    fun `M inject buildId W bundleRelease`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )
        val runArguments = mutableListOf(
            "--stacktrace",
            ":samples:app:bundleRelease"
        ).apply {
            // https://issuetracker.google.com/issues/231997838
            if (buildVersionConfig.isAgpAboveOrEqual730()) {
                add("--configuration-cache")
            }
        }

        // When
        val result = gradleRunner { withArguments(runArguments) }
            .build()

        // Then
        assertThat(result.task(":samples:app:bundleRelease")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val bundles = testProjectDir.walk()
            .filter {
                it.isFile &&
                    !it.name.contains("intermediary") &&
                    it.extension == "aab"
            }
            .map { ZipFile(it) }
            .toList()

        assertThat(bundles).isNotEmpty

        val buildIdFiles = bundles.mapNotNull { it.getEntry(BUILD_ID_FILE_PATH_AAB) }

        // each bundle should contain one build ID file
        assertThat(bundles.size).isEqualTo(buildIdFiles.size)

        val buildIds = bundles.map {
            it.readBuildId(BUILD_ID_FILE_PATH_AAB)
                .let { UUID.fromString(it) }
        }

        // all build IDs should be unique
        assertThat(buildIds.toSet()).hasSize(bundles.size)
    }

    // region Mapping Upload

    @Test
    fun `M try to upload the mapping file W upload { using a fake API_KEY }`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )
        val color = forge.anElementFrom(colors)
        val version = forge.anElementFrom(versions)
        val variantVersionName = version.lowercase()
        val variant = "${version.lowercase()}$color"
        val taskName = resolveMappingUploadTask(variant)

        // When
        // since there is no explicit dependency between assemble and upload tasks, Gradle may
        // optimize the execution and run them in parallel, ignoring the order in the command
        // line, so we do the explicit split
        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        val result = gradleRunner {
            withArguments(
                taskName,
                "--info",
                "--stacktrace",
                "-PDD_API_KEY=fakekey"
            )
        }
            .buildAndFail()

        // Then
        assertThat(result).containsInOutput("Creating request with GZIP encoding.")

        val buildIdInOriginFile = testProjectDir.findBuildIdInOriginFile(variant)
        val buildIdInApk = testProjectDir.findBuildIdInApk(variant)
        assertThat(buildIdInApk).isEqualTo(buildIdInOriginFile)

        assertThat(result).containsInOutput(
            "Uploading file jvm_mapping with tags " +
                "`service:com.example.variants.$variantVersionName`, " +
                "`version:1.0-$variantVersionName`, " +
                "`version_code:1`, " +
                "`variant:$variant`, " +
                "`build_id:$buildIdInOriginFile` (site=datadoghq.com):"
        )
        assertThat(result).containsInOutput(
            """
                Detected repository:
                {
                    "files": [
                        "src/main/java/Placeholder.java",
                        "src/main/kotlin/SampleApplication.kt"
                    ],
                    "repository_url": "$fakeRemoteUrl",
                    "hash": "${headHash(appRootDir)}"
                }
            """.trimIndent()
        )
    }

    @Test
    fun `M try to upload the mapping file W upload { using a fake API_KEY, gzip disabled }`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )
        val color = forge.anElementFrom(colors)
        val version = forge.anElementFrom(versions)
        val variantVersionName = version.lowercase()
        val variant = "${version.lowercase()}$color"
        val taskName = resolveMappingUploadTask(variant)

        // When
        // since there is no explicit dependency between assemble and upload tasks, Gradle may
        // optimize the execution and run them in parallel, ignoring the order in the command
        // line, so we do the explicit split
        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        val result = gradleRunner {
            withArguments(
                taskName,
                "--info",
                "--stacktrace",
                "-PDD_API_KEY=fakekey",
                "-Pdd-disable-gzip"
            )
        }
            .buildAndFail()

        // Then
        assertThat(result).containsInOutput("Creating request without GZIP encoding.")

        val buildIdInOriginFile = testProjectDir.findBuildIdInOriginFile(variant)
        val buildIdInApk = testProjectDir.findBuildIdInApk(variant)
        assertThat(buildIdInApk).isEqualTo(buildIdInOriginFile)

        assertThat(result).containsInOutput(
            "Uploading file jvm_mapping with tags " +
                "`service:com.example.variants.$variantVersionName`, " +
                "`version:1.0-$variantVersionName`, " +
                "`version_code:1`, " +
                "`variant:$variant`, " +
                "`build_id:$buildIdInOriginFile` (site=datadoghq.com):"
        )
        assertThat(result).containsInOutput(
            """
                Detected repository:
                {
                    "files": [
                        "src/main/java/Placeholder.java",
                        "src/main/kotlin/SampleApplication.kt"
                    ],
                    "repository_url": "$fakeRemoteUrl",
                    "hash": "${headHash(appRootDir)}"
                }
            """.trimIndent()
        )
    }

    @Test
    fun `M try to upload the mapping file W upload { datadog-ci file, parent dir }`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )
        val color = forge.anElementFrom(colors)
        val version = forge.anElementFrom(versions)
        val variantVersionName = version.lowercase()
        val variant = "${version.lowercase()}$color"
        val taskName = resolveMappingUploadTask(variant)

        datadogCiFile.createNewFile()

        datadogCiFile.writeText(
            JSONObject().apply {
                put("apiKey", "someKey")
                put("datadogSite", "datadoghq.eu")
            }.toString()
        )

        // When
        // since there is no explicit dependency between assemble and upload tasks, Gradle may
        // optimize the execution and run them in parallel, ignoring the order in the command
        // line, so we do the explicit split
        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        val result = gradleRunner { withArguments(taskName, "--info") }
            .buildAndFail()

        // Then
        val buildIdInOriginFile = testProjectDir.findBuildIdInOriginFile(variant)
        val buildIdInApk = testProjectDir.findBuildIdInApk(variant)
        assertThat(buildIdInApk).isEqualTo(buildIdInOriginFile)

        assertThat(result).containsInOutput(
            "Uploading file jvm_mapping with tags " +
                "`service:com.example.variants.$variantVersionName`, " +
                "`version:1.0-$variantVersionName`, " +
                "`version_code:1`, " +
                "`variant:$variant`, " +
                "`build_id:$buildIdInOriginFile` (site=datadoghq.eu):"
        )
        assertThat(result).containsInOutput("API key found in Datadog CI config file, using it.")
        assertThat(result)
            .containsInOutput("Site property found in Datadog CI config file, using it.")
        assertThat(result).containsInOutput(
            """
                Detected repository:
                {
                    "files": [
                        "src/main/java/Placeholder.java",
                        "src/main/kotlin/SampleApplication.kt"
                    ],
                    "repository_url": "$fakeRemoteUrl",
                    "hash": "${headHash(appRootDir)}"
                }
            """.trimIndent()
        )
    }

    @Test
    fun `M try to upload the mapping file W upload { custom remote repos url }`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_custom_remote_repos_url.gradle",
            appBuildGradleFile
        )
        val color = forge.anElementFrom(colors)
        val version = forge.anElementFrom(versions)
        val variantVersionName = version.lowercase()
        val variant = "${version.lowercase()}$color"
        val taskName = resolveMappingUploadTask(variant)

        // When
        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        val result = gradleRunner {
            withArguments(
                taskName,
                "--info",
                "--stacktrace",
                "-PDD_API_KEY=fakekey"
            )
        }
            .buildAndFail()

        // Then
        val buildIdInOriginFile = testProjectDir.findBuildIdInOriginFile(variant)
        val buildIdInApk = testProjectDir.findBuildIdInApk(variant)
        assertThat(buildIdInApk).isEqualTo(buildIdInOriginFile)

        assertThat(result).containsInOutput(
            "Uploading file jvm_mapping with tags " +
                "`service:com.example.variants.$variantVersionName`, " +
                "`version:1.0-$variantVersionName`, " +
                "`version_code:1`, " +
                "`variant:$variant`, " +
                "`build_id:$buildIdInOriginFile` (site=datadoghq.com):"
        )
        assertThat(result).containsInOutput(
            """
                Detected repository:
                {
                    "files": [
                        "src/main/java/Placeholder.java",
                        "src/main/kotlin/SampleApplication.kt"
                    ],
                    "repository_url": "http://github.com:fakeapp/repository.git",
                    "hash": "${headHash(appRootDir)}"
                }
            """.trimIndent()
        )
    }

    @Test
    fun `M try to upload the mapping file W upload { optimized mapping }`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_optimized_mapping.gradle",
            appBuildGradleFile
        )
        val color = forge.anElementFrom(colors)
        val version = forge.anElementFrom(versions)
        val variantVersionName = version.lowercase()
        val variant = "${version.lowercase()}$color"
        val taskName = resolveMappingUploadTask(variant)

        // When
        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        val result = gradleRunner {
            withArguments(
                taskName,
                "--info",
                "--stacktrace",
                "-PDD_API_KEY=fakekey"
            )
        }
            .buildAndFail()

        // Then
        val buildIdInOriginFile = testProjectDir.findBuildIdInOriginFile(variant)
        val buildIdInApk = testProjectDir.findBuildIdInApk(variant)
        assertThat(buildIdInApk).isEqualTo(buildIdInOriginFile)

        assertThat(result).containsInOutput(
            "Uploading file jvm_mapping with tags " +
                "`service:com.example.variants.$variantVersionName`, " +
                "`version:1.0-$variantVersionName`, " +
                "`version_code:1`, " +
                "`variant:$variant`, " +
                "`build_id:$buildIdInOriginFile` (site=datadoghq.com):"
        )
        assertThat(result).containsInOutput(
            """
                Detected repository:
                {
                    "files": [
                        "src/main/java/Placeholder.java",
                        "src/main/kotlin/SampleApplication.kt"
                    ],
                    "repository_url": "$fakeRemoteUrl",
                    "hash": "${headHash(appRootDir)}"
                }
            """.trimIndent()
        )
        val optimizedFile = Path(
            appRootDir.path,
            "build",
            "outputs",
            "mapping",
            "${variant}Release",
            DdMappingFileUploadTask.MAPPING_OPTIMIZED_FILE_NAME
        ).toFile()
        assertThat(result).containsInOutput(
            "Size of optimized file is ${optimizedFile.length()} bytes"
        )
    }

    @Test
    fun `M try to upload the mapping file W upload {variant config override}`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_variant_override.gradle",
            appBuildGradleFile
        )
        val color = forge.anElementFrom(colors)
        val variant = "pro$color"
        val taskName = resolveMappingUploadTask(variant)

        // When
        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        val result = gradleRunner {
            withArguments(
                taskName,
                "--info",
                "--stacktrace",
                "-PDD_API_KEY=fakekey"
            )
        }
            .buildAndFail()

        // Then
        assertThat(result).containsInOutput(
            """
                Detected repository:
                {
                    "files": [
                        "src/main/java/Placeholder.java",
                        "src/main/kotlin/SampleApplication.kt"
                    ],
                    "repository_url": "http://github.com:fakeapp-another/repository.git",
                    "hash": "${headHash(appRootDir)}"
                }
            """.trimIndent()
        )
    }

    @Test
    fun `M not contain any uploadTasks W minifyNotEnabled`() {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_minify_not_enabled.gradle",
            appBuildGradleFile
        )

        // When
        val result = gradleRunner { withArguments("--info", ":samples:app:tasks") }
            .build()

        // Then
        val uploadTask = result.output
            .split("\n")
            .firstOrNull { it.startsWith("upload") }
        assertThat(uploadTask).isNull()
    }

    // endregion

    // region Symbol Upload

    @Test
    fun `M not try to upload the symbol file W no cmake dependencies { using a fake API_KEY }`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "build_with_datadog_dep.gradle",
            appBuildGradleFile
        )
        stubNativeFiles()

        val result = gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        // Then
        assertThat(result).hasNoNdkSymbolUploadTasks()
    }

    @Test
    fun `M try to upload the symbol file W upload { using a fake API_KEY }`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "native_gradle_files/build_with_native_and_datadog_dep.gradle",
            appBuildGradleFile
        )
        stubNativeFiles()

        val color = forge.anElementFrom(colors)
        val version = forge.anElementFrom(versions)
        val variantVersionName = version.lowercase()
        val variant = "${version.lowercase()}$color"
        val taskName = resolveSymbolUploadTask(variant)

        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        val result = gradleRunner {
            withArguments(
                taskName,
                "--info",
                "--stacktrace",
                "-PDD_API_KEY=fakekey"
            )
        }
            .buildAndFail()

        // Then
        val buildIdInOriginFile = testProjectDir.findBuildIdInOriginFile(variant)

        assertThat(result).containsInOutput("Creating request with GZIP encoding.")

        assertThat(result).containsInOutput("Uploading ndk_symbol_file file: ");

        assertThat(result).containsInOutput(
            "Uploading file libplaceholder.so with tags " +
                    "`service:com.example.variants.$variantVersionName`, " +
                    "`version:1.0-$variantVersionName`, " +
                    "`version_code:1`, " +
                    "`variant:$variant`, " +
                    "`build_id:$buildIdInOriginFile` (site=datadoghq.com):"
        )

        for (arch in SUPPORTED_ABIS) {
            assertThat(result).containsInOutput("extra attributes: {arch=$arch}")
        }
    }

    fun `M try to upload the symbol file W upload { using a fake API_KEY, gzip disabled }`(forge: Forge) {
        // Given
        stubGradleBuildFromResourceFile(
            "native_gradle_files/build_with_native_and_datadog_dep.gradle",
            appBuildGradleFile
        )
        stubNativeFiles()

        val color = forge.anElementFrom(colors)
        val version = forge.anElementFrom(versions)
        val variantVersionName = version.lowercase()
        val variant = "${version.lowercase()}$color"
        val taskName = resolveSymbolUploadTask(variant)

        gradleRunner { withArguments("--info", ":samples:app:assembleRelease") }
            .build()

        val result = gradleRunner {
            withArguments(
                taskName,
                "--info",
                "--stacktrace",
                "-PDD_API_KEY=fakekey"
            )
        }
            .buildAndFail()

        // Then
        val buildIdInOriginFile = testProjectDir.findBuildIdInOriginFile(variant)

        assertThat(result).containsInOutput("Creating request without GZIP encoding.")

        assertThat(result).containsInOutput("Uploading ndk_symbol_file file: ");

        assertThat(result).containsInOutput(
            "Uploading file libplaceholder.so with tags " +
                    "`service:com.example.variants.$variantVersionName`, " +
                    "`version:1.0-$variantVersionName`, " +
                    "`version_code:1`, " +
                    "`variant:$variant`, " +
                    "`build_id:$buildIdInOriginFile` (site=datadoghq.com):"
        )

        for (arch in SUPPORTED_ABIS) {
            assertThat(result).containsInOutput("extra attributes: {arch=$arch}")
        }
    }

    // endregion

    // region Internal

    private fun resolveMappingUploadTask(variantName: String) = "uploadMapping${variantName}Release"
    private fun resolveSymbolUploadTask(variantName: String) = "ddUploadNdkSymbolFiles${variantName}Release"

    private fun stubFile(destination: File, content: String) {
        with(destination.outputStream()) {
            write(content.toByteArray())
        }
    }

    private fun stubGradleBuildFromResourceFile(resourceFilePath: String, gradleFile: File) {
        javaClass.classLoader.getResource(resourceFilePath)?.file?.let {
            File(it).copyTo(gradleFile)
        }
    }

    private fun stubNativeFiles() {
        appMainCppSourcesDir = File(appMainSrcDir, "cpp").apply { mkdirs() }
        cmakeFile = File(appMainCppSourcesDir, "CMakeLists.txt")
        cppPlaceholderFile = File(appMainCppSourcesDir, "placeholder.cpp")

        stubFile(cmakeFile!!, CMAKE_FILE_CONTENT)
        stubFile(cppPlaceholderFile!!, CPP_FILE_CONTENT)
    }

    private fun gradleRunner(configure: GradleRunner.() -> Unit): GradleRunner {
        return GradleRunner.create()
            .withGradleVersion(buildVersionConfig.gradleVersion)
            .withProjectDir(testProjectDir)
            // https://github.com/gradle/gradle/issues/22466
            // for now the workaround will be to manually inject necessary files into plugin classpath
            // see ROOT_BUILD_FILE_CONTENT
            // .withPluginClasspath()
            .apply {
                configure(this)
            }
    }

    private fun File.findBuildIdInOriginFile(variantName: String): String {
        return walk()
            .filter {
                it.name == GenerateBuildIdTask.BUILD_ID_FILE_NAME &&
                    it.path.contains(variantName)
            }
            .map {
                it.readText()
            }
            .first()
    }

    private fun File.findBuildIdInApk(variantName: String): String {
        return walk()
            .filter {
                it.extension == "apk" && it.path.contains(variantName)
            }
            .map {
                ZipFile(it).readBuildId(BUILD_ID_FILE_PATH_APK)
            }
            .first()
    }

    private fun ZipFile.readBuildId(path: String): String {
        return getInputStream(getEntry(path))
            .bufferedReader()
            .readText()
            .trim()
    }

    @Suppress("ReturnCount")
    private fun BuildVersionConfig.isAgpAboveOrEqual730(): Boolean {
        val groups = agpVersion.split(".")
        if (groups.size < 3) return false
        val major = groups[0].toIntOrNull()
        val minor = groups[1].toIntOrNull()
        val patch = groups[2].substringBefore("-").toIntOrNull()
        if (major == null || minor == null || patch == null) return false
        return major >= 7 && minor >= 3 && patch >= 0
    }

    // endregion

    companion object {

        data class BuildVersionConfig(
            val agpVersion: String,
            val gradleVersion: String,
            val buildToolsVersion: String,
            val targetSdkVersion: String,
            val kotlinVersion: String,
            val jvmTarget: String
        )

        val SUPPORTED_ABIS = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

        val APPLICATION_CLASS_CONTENT = """
            package com.datadog.android.sample

            import android.app.Application
            import android.util.Log

            class SampleApplication : Application() {

                override fun onCreate() {
                    super.onCreate()
                    Log.v("Application","Hello World")
                }
            }
        """.trimIndent()
        val JAVA_CLASS_CONTENT = """
            package com.datadog.android.sample;

            public class Placeholder {}
        """.trimIndent()
        val LIB_MODULE_PLACEHOLDER_CLASS_CONTENT = """
            package com.example.lib
            
            class Placeholder {
            }
        """.trimIndent()
        val APP_MANIFEST_FILE_CONTENT = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                      package="com.example.variants">

                <application
                    android:allowBackup="true"
                    android:supportsRtl="true">
                    <activity android:name=".MainActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN"/>

                            <category android:name="android.intent.category.LAUNCHER"/>
                        </intent-filter>
                    </activity>
                </application>

            </manifest>
        """.trimIndent()
        val LIB_MODULE_MANIFEST_FILE_CONTENT = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.example.lib">

            </manifest>
        """.trimIndent()
        const val ROOT_BUILD_FILE_CONTENT = """
            buildscript {
                ext {
                    targetSdkVersion = targetSdkVersion as Integer
                    buildToolsVersion = buildToolsVersion
                    // some AndroidX dependencies in recent SDK versions require compileSdk >= 33, so downgrading
                    datadogSdkDependency = targetSdkVersion >= 33 ?
                       "com.datadoghq:dd-sdk-android-rum:2.5.0" : "com.datadoghq:dd-sdk-android:1.15.0"
                    jvmTarget = jvmTarget
                }
                repositories {
                    gradlePluginPortal()
                    google()
                }

                dependencies {
                    classpath files(*pluginClasspath.split(","))
                    classpath dependencies.create("com.android.tools.build:gradle:${"$"}agpVersion")
                    classpath dependencies.create("org.jetbrains.kotlin:kotlin-gradle-plugin:${"$"}kotlinVersion")
                }
            }
            allprojects {
                repositories {
                    gradlePluginPortal()
                    google()
                }
            }
        """
        const val SETTINGS_FILE_CONTENT = """
            include(":samples:app")
            include(":samples:lib-module")
        """
        val GRADLE_PROPERTIES_FILE_CONTENT = """
           org.gradle.jvmargs=-Xmx2560m
           android.useAndroidX=true
           agpVersion=%s
           buildToolsVersion=%s
           targetSdkVersion=%s
           kotlinVersion=%s
           pluginClasspath=%s
           jvmTarget=%s
           // use less memory, we are not interested in result, just in the build process
           android.enableR8.fullMode=false
        """.trimIndent()

        val CMAKE_FILE_CONTENT = """
            cmake_minimum_required(VERSION 3.4.1)
            project("placeholder")
            add_library( ${"\${CMAKE_PROJECT_NAME}"} SHARED
                         placeholder.cpp )
                         
            target_link_libraries( ${"\${CMAKE_PROJECT_NAME}"}
                android
                log)
        """

        val CPP_FILE_CONTENT = """
            #include <jni.h>

            extern "C" JNIEXPORT jstring JNICALL
            Java_com_datadog_example_ndk_MainActivity_stringFromJNI( JNIEnv* env, jobject object ) {
                return env->NewStringUTF("Hello from JNI!");
            }
        """.trimIndent()

        const val LATEST_GRADLE_VERSION = "8.5"
        const val LATEST_AGP_VERSION = "8.2.0"

        // NB: starting from AGP 7.x, Gradle should have the same major version.
        // While work with Gradle with higher major version is possible, it is not guaranteed.
        val TESTED_CONFIGURATIONS = listOf(
            BuildVersionConfig(
                agpVersion = "7.0.4",
                gradleVersion = "7.4",
                buildToolsVersion = "31.0.0",
                targetSdkVersion = "31",
                kotlinVersion = "1.6.10",
                jvmTarget = JavaVersion.VERSION_11.toString()
            ),
            BuildVersionConfig(
                agpVersion = LATEST_AGP_VERSION,
                gradleVersion = LATEST_GRADLE_VERSION,
                buildToolsVersion = "34.0.0",
                targetSdkVersion = "34",
                kotlinVersion = "1.9.21",
                jvmTarget = JavaVersion.VERSION_17.toString()
            )
        )

        const val BUILD_ID_FILE_PATH_APK = "assets/datadog.buildId"
        const val BUILD_ID_FILE_PATH_AAB = "base/assets/datadog.buildId"
    }
}
