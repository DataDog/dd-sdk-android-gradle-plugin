/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.nhaarman.mockitokotlin2.mock
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

internal class DdExtensionForgeryFactory : ForgeryFactory<DdExtension> {
    override fun getForgery(forge: Forge): DdExtension {
        return DdExtension().apply {
            serviceName = forge.aStringMatching("[a-z]{3}(\\.[a-z]{5,10}){2,4}")
            versionName = forge.aStringMatching("\\d\\.\\d{1,2}\\.\\d{1,3}")
            site = forge.aValueFrom(DatadogSite::class.java).name
            variants = mock()
            checkProjectDependencies = forge.aValueFrom(SdkCheckLevel::class.java)
        }
    }
}
