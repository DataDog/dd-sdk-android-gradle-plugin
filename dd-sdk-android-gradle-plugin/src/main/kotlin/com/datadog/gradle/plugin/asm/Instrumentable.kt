/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.asm

import com.android.build.api.instrumentation.ClassContext
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import java.io.Serializable

interface Instrumentable<Visitor, InstrumentableContext> : Serializable {

    val fqName: String
        get() = ""

    fun getVisitor(
        instrumentableContext: InstrumentableContext,
        apiVersion: Int,
        originalVisitor: Visitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
    ): Visitor

    fun isInstrumentable(data: InstrumentableContext): Boolean
}

interface ClassInstrumentable : Instrumentable<ClassVisitor, ClassContext> {

    override fun isInstrumentable(data: ClassContext): Boolean =
        fqName == data.currentClassData.className
}

interface MethodInstrumentable : Instrumentable<MethodVisitor, MethodContext> {

    override fun isInstrumentable(data: MethodContext): Boolean = fqName == data.name
}
