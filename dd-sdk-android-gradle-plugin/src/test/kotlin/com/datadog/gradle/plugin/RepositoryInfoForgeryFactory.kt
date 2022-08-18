/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

class RepositoryInfoForgeryFactory : ForgeryFactory<RepositoryInfo> {
    override fun getForgery(forge: Forge): RepositoryInfo {
        return RepositoryInfo(
            url = forge.aStringMatching("git@github\\.com:[a-z]+/[a-z][a-z0-9_-]+\\.git"),
            hash = forge.anHexadecimalString(Case.LOWER),
            sourceFiles = forge.aList { aStringMatching("\\w+(/\\w+)/\\w+\\.[a-z]{3}") }
        )
    }
}
