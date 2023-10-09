/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

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
    internal val variants = objectFactory.domainObjectContainer(DdExtensionConfiguration::class.java)

    /**
     * Closure method to create a groovy DSL for variant configurations.
     */
    fun variants(configureClosure: Closure<DdExtensionConfiguration>) {
        variants.configure(configureClosure)
    }

    /**
     * Method compatible with Kotlin Script to create a DSL for variant configurations.
     */
    fun variants(configure: VariantScope.() -> Unit) {
        configure(VariantScope())
    }

    /**
     * Inner class used for Kotlin DSL.
     */
    inner class VariantScope {

        /**
         * Defines a new named object, which will be created and configured when it is required.
         * @param name the name of the variant to configure
         * @param configuration the action to run to configure the variant
         */
        fun register(name: String, configuration: DdExtensionConfiguration.() -> Unit) {
            variants.register(name) {
                configuration(it)
            }
        }
    }
}
