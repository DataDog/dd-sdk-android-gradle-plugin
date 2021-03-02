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
     * Whether the plugin should be enabled or not.
     */
    var enabled: Boolean = true

    // introduced because cannot use lateinit with `variants` because of name
    // ambiguity (method vs property). Consider switching to constructor injection
    // via https://docs.gradle.org/current/javadoc/org/gradle/api/model/ObjectFactory.html#domainObjectContainer-java.lang.Class-
    // once it is stable
    private lateinit var _variants: NamedDomainObjectContainer<DdExtensionConfiguration>

    /**
     * Container for the variant's configurations.
     */
    var variants: NamedDomainObjectContainer<DdExtensionConfiguration>
        get() = _variants
        set(value) {
            _variants = value
        }

    /**
     * Closure method to create a DSL for variant configurations.
     */
    fun variants(configureClosure: Closure<DdExtensionConfiguration>) {
        if (::_variants.isInitialized) {
            _variants.configure(configureClosure)
        }
    }
}
