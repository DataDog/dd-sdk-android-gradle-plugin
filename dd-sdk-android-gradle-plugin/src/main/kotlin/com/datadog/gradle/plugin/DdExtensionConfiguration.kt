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
     * The Datadog site to upload your data to (one of "US", "EU", "GOV").
     */
    var site: String? = null

    /**
     * The url of the remote repository where the source code was deployed. If not provided this
     * value will be resolved from your current GIT configuration during the task execution time.
     */
    var remoteRepositoryUrl: String? = null

    /**
     * This property controls if plugin should check if Datadog SDK is included in the dependencies
     * and if it is not: "none" - ignore, "warn" - log a warning, "fail" - fail the build
     * with an error (default).
     */
    var checkProjectDependencies: SdkCheckLevel? = null

    /**
     * The absolute path to the mapping file to be used for deobfuscation. If not provided the
     * default one will be used: [buildDir]/output/mapping/[variant]/mapping.txt
     */
    var mappingFilePath: String? = null

    /**
     * DO NOT SET THIS UNLESS YOU KNOW WHAT IT IS.
     */
    var mappingFilePackageAliases: Map<String, String> = emptyMap()

    internal fun updateWith(config: DdExtensionConfiguration) {
        config.versionName?.let { versionName = it }
        config.serviceName?.let { serviceName = it }
        config.site?.let { site = it }
        config.remoteRepositoryUrl?.let { remoteRepositoryUrl = it }
        config.checkProjectDependencies?.let { checkProjectDependencies = it }
        config.mappingFilePath?.let { mappingFilePath = it }
        mappingFilePackageAliases = config.mappingFilePackageAliases
    }
}
