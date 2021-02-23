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
