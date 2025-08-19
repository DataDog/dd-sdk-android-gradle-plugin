/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.utils.forge

import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeConfigurator
import fr.xgouchet.elmyr.jvm.useJvmFactories

class Configurator : ForgeConfigurator {

    override fun configure(forge: Forge) {
        forge.addFactory(IdentifierForgeryFactory())
        forge.addFactory(DdExtensionForgeryFactory())
        forge.addFactory(DdExtensionConfigurationForgeryFactory())
        forge.addFactory(RepositoryInfoForgeryFactory())
        forge.useJvmFactories()
    }
}
