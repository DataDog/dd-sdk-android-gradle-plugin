/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.DdConfiguration
import java.io.Serializable

/**
 * Extension used to configure the `dd-android-gradle-plugin`.
 */
open class DdExtension : Serializable {

    /**
     * The environment name for the application.
     */
    var environmentName: String = ""

    /**
     * The environment name for the application.
     * By default (null) it will read the version name of your application from your gradle
     * configuration.
     */
    var versionName: String? = null

    /**
     * The service name for the application.
     * By default (null) it will read the package name of your application from your gradle
     * configuration.
     */
    var serviceName: String? = null

    /**
     * The Datadog site to upload your data to (one of "US", "EU", "GOV").
     */
    var site: String = DdConfiguration.Site.US.name
}
