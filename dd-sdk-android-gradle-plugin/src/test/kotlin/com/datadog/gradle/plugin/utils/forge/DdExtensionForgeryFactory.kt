/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.utils.forge

import com.datadog.gradle.plugin.DatadogSite
import com.datadog.gradle.plugin.DdExtension
import com.datadog.gradle.plugin.SdkCheckLevel
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory
import org.gradle.api.model.ObjectFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class DdExtensionForgeryFactory : ForgeryFactory<DdExtension> {
    override fun getForgery(forge: Forge): DdExtension {
        val objectFactory: ObjectFactory = mock()

        whenever(objectFactory.domainObjectContainer<Any>(any())) doReturn mock()

        return DdExtension(objectFactory).apply {
            serviceName = forge.aStringMatching("[a-z]{3}(\\.[a-z]{5,10}){2,4}")
            versionName = forge.aStringMatching("\\d\\.\\d{1,2}\\.\\d{1,3}")
            site = forge.aValueFrom(DatadogSite::class.java).name
            checkProjectDependencies = forge.aValueFrom(SdkCheckLevel::class.java)
            remoteRepositoryUrl = forge.aStringMatching(
                "https://[a-z]{4,10}\\.[com|org]/[a-z]{4,10}/[a-z]{4,10}\\.git"
            )
            mappingFilePath = forge.aStringMatching(
                "([a-z]+)/([a-z]+)/([a-z]+)/mapping.txt"
            )
            mappingFilePackageAliases = forge.aMap {
                forge.aStringMatching("[a-z]{3}(\\.[a-z]{5,10}){2,4}") to
                    forge.anAlphabeticalString()
            }
            mappingFileTrimIndents = forge.aBool()
            ignoreDatadogCiFileConfig = forge.aBool()
        }
    }
}
