/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import groovy.lang.Closure
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory

/**
 * Extension used to configure the `dd-android-gradle-plugin`.
 */
open class DdExtension(
    @Inject private val objectFactory: ObjectFactory
) : DdExtensionConfiguration() {

    /**
     * Whether the plugin should be enabled or not.
     */
    var enabled: Boolean = true

    /**
     * Container for the variant's configurations.
     */
    internal val variants =
        objectFactory.domainObjectContainer(DdExtensionConfiguration::class.java)

    /**
     * Closure method to create a DSL for variant configurations.
     */
    fun variants(configureClosure: Closure<DdExtensionConfiguration>) {
        variants.configure(configureClosure)
    }
}
