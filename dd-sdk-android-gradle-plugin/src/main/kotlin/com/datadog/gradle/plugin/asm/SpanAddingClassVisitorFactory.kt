/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor
import java.io.File

abstract class SpanAddingClassVisitorFactory :
    AsmClassVisitorFactory<SpanAddingClassVisitorFactory.SpanAddingParameters> {

    interface SpanAddingParameters : InstrumentationParameters {
        @get:Input
        val debug: Property<Boolean>

        @get:Internal
        val tmpDir: Property<File>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val intrumentable = Application()

        return intrumentable.getVisitor(
            classContext,
            instrumentationContext.apiVersion.get(),
            nextClassVisitor,
            parameters = parameters.get(),
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val intrumentable = Application()
        return intrumentable.isInstrumentable(classData.toClassContext())
    }
}
