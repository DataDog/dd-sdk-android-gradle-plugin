package com.datadog.gradle.plugin

/**
 * Base extension used to configure the `dd-android-gradle-plugin`.
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
     * This property controls if plugin should check if Datadog SDK is included in the dependencies
     * and if it is not: "none" - ignore, "warn" - log a warning, "fail" - fail the build
     * with an error (default).
     */
    var checkProjectDependencies: SdkCheckLevel? = null

    internal fun updateWith(config: DdExtensionConfiguration) {
        config.versionName?.let { versionName = it }
        config.serviceName?.let { serviceName = it }
        config.site?.let { site = it }
        config.checkProjectDependencies?.let { checkProjectDependencies = it }
    }
}
