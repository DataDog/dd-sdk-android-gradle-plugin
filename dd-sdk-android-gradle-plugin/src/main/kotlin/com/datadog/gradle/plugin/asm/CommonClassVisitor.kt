/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class CommonClassVisitor(
    apiVersion: Int,
    classVisitor: ClassVisitor,
    private val methodInstrumentables: List<MethodInstrumentable>,
    private val parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
) : ClassVisitor(apiVersion, classVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        val methodContext = MethodContext(access, name, descriptor, signature, exceptions?.toList())
        val instrumentable = methodInstrumentables.find { it.isInstrumentable(methodContext) }
        return instrumentable?.getVisitor(methodContext, api, mv, parameters) ?: mv
    }
}
