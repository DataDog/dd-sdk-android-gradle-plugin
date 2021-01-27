/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer

/**
 * Extension used to configure the `dd-android-gradle-plugin`.
 */
open class DdExtension : DdExtensionConfiguration() {

    /**
     * Container for the variant's configurations.
     */
    var variants: NamedDomainObjectContainer<DdExtensionConfiguration>? = null

    /**
     * Closure method to create a DSL for variant configurations.
     */
    fun variants(configureClosure: Closure<DdExtensionConfiguration>) {
        variants?.configure(configureClosure)
    }
}
