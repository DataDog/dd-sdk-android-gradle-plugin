/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

/**
 * Base extension used to configure the `dd-android-gradle-plugin`.
 * @param name Name of the given configuration.
 */
open class DdExtensionConfiguration(
    val name: String = ""
) {

    /**
     * The version name of the application.
     * By default (null) it will read the version name of your application from your gradle
     * configuration.
     */
    var versionName: String? = null

    /**
     * The service name of the application.
     * By default (null) it will read the package name of your application from your gradle
     * configuration.
     */
    var serviceName: String? = null

    /**
     * The Datadog site to upload your data to (one of names in [DatadogSite], e.g. "US1", "EU1", etc.).
     */
    var site: String? = null

    /**
     * The url of the remote repository where the source code was deployed. If not provided this
     * value will be resolved from your current GIT configuration during the task execution time.
     */
    var remoteRepositoryUrl: String? = null

    /**
     * This property controls if plugin should check if Datadog SDK is included in the dependencies
     * and if it is not: "none" - ignore (default), "warn" - log a warning, "fail" - fail the build
     * with an error.
     */
    // TODO RUMM-2344
    var checkProjectDependencies: SdkCheckLevel? = null

    /**
     * The absolute path to the mapping file to be used for deobfuscation. If not provided the
     * default one will be used: [buildDir]/output/mapping/[variant]/mapping.txt
     */
    var mappingFilePath: String? = null

    // TODO RUMM-0000 Deprecate and then remove this API once all DCs support large file upload
    /**
     * Short aliases to use for package prefixes. Allows to replace, for example,
     * androidx.appcompat with something shorter, reducing the size of the mapping file. Key is
     * the prefix to replace, value is the replacement. Note, that these short aliases will be also
     * in the deobfuscated stacktrace instead of the original prefixes. Sample usage:
     *
     * ```
     *  "androidx.appcompat" to "axapp",
     *  "androidx.work" to "axw",
     *  "java.lang" to "jl",
     *  "kotlin.collections" to "kc"
     *  ...
     * ```
     *
     * This property is not inherited, meaning in the particular variant configuration it should
     * be declared explicitly, even if it exists in the root configuration.
     *
     * Warning: DO NOT SET THIS UNLESS YOU ARE OVER THE LIMIT OF THE MAPPING FILE SIZE.
     */
    var mappingFilePackageAliases: Map<String, String> = emptyMap()

    // TODO RUMM-0000 Deprecate and then remove this API once all DCs support large file upload
    /**
     * This property removes indents from each line of the mapping file, reducing its size.
     *
     * Warning: DO NOT SET THIS UNLESS YOU ARE OVER THE LIMIT OF THE MAPPING FILE SIZE.
     */
    var mappingFileTrimIndents: Boolean = false

    /**
     * This property declares that the obfuscation technology used is not the default
     * R8/Proguard included in the Android toolchain (e.g.: Dexguard, …).
     * Doing so will create an upload task for all variants and all build types
     */
    var nonDefaultObfuscation: Boolean = false

    /**
     * Ignore the config declared in `datadog-ci.json` file if found.
     */
    var ignoreDatadogCiFileConfig: Boolean = false

    /**
     * Additional locations that Gradle plugin will check for `.so` files during `uploadNdkSymbolFiles` task.
     * Directory structure should follow pattern: `/path/to/location/obj/{arch}/libname.so`.
     *
     * In case of above `additionalSymbolFilesLocations` should be equal to:
     *
     * ```
     * datadog {
     *      additionalSymbolFilesLocations = listOf("/path/to/location/obj")
     * }
     * ```
     *
     * Note that each `.so` file should be placed inside the directory with architecture name (e.g. x86).
     */
    var additionalSymbolFilesLocations: List<String>? = null

    /**
     * This function allow to register the instrumentation mode for Jetpack Compose RUM views tracking, actions tracking
     * and images recording. If the `composeInstrumentation` is not set, the instrumentation mode will be
     * [InstrumentationMode.DISABLE] by default.
     */
    var composeInstrumentation: InstrumentationMode = InstrumentationMode.DISABLE

    internal fun updateWith(config: DdExtensionConfiguration) {
        config.versionName?.let { versionName = it }
        config.serviceName?.let { serviceName = it }
        config.site?.let { site = it }
        config.remoteRepositoryUrl?.let { remoteRepositoryUrl = it }
        config.checkProjectDependencies?.let { checkProjectDependencies = it }
        config.mappingFilePath?.let { mappingFilePath = it }
        config.additionalSymbolFilesLocations?.let { additionalSymbolFilesLocations = it }
        mappingFilePackageAliases = config.mappingFilePackageAliases
        mappingFileTrimIndents = config.mappingFileTrimIndents
        ignoreDatadogCiFileConfig = config.ignoreDatadogCiFileConfig
        nonDefaultObfuscation = config.nonDefaultObfuscation
        composeInstrumentation = config.composeInstrumentation
    }
}
