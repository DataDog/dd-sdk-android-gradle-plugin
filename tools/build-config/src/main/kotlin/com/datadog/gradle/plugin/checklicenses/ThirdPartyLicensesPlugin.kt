/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.checklicenses

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import java.io.File

class ThirdPartyLicensesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.extensions
            .create<ThirdPartyLicensesExtension>(EXT_NAME)
        extension.csvFile = File(
            target.rootDir,
            ThirdPartyLicensesExtension.DEFAULT_TP_LICENCE_FILENAME
        )

        target.tasks
            .register<UpdateThirdPartyLicensesTask>(TASK_UPDATE_NAME) {
                this.extension = extension
            }

        val checkTask = target.tasks
            .register<CheckThirdPartyLicensesTask>(TASK_CHECK_NAME) {
                this.extension = extension
            }

        target.tasks.named("check").configure {
            dependsOn(checkTask)
        }
    }

    companion object {
        const val EXT_NAME = "thirdPartyLicences"

        const val TASK_UPDATE_NAME = "updateThirdPartyLicences"
        const val TASK_CHECK_NAME = "checkThirdPartyLicences"
    }
}
