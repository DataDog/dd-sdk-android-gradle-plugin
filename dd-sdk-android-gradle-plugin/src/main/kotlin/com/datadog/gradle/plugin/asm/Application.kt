/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.asm

import com.android.build.api.instrumentation.ClassContext
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class Application : ClassInstrumentable {

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
    ): ClassVisitor =
        CommonClassVisitor(
            apiVersion = apiVersion,
            classVisitor = originalVisitor,
            methodInstrumentables = listOf(ApplicationMethodInstrumentable()),
            parameters = parameters,
        )

    override fun isInstrumentable(data: ClassContext): Boolean =
        data.currentClassData.superClasses.contains("android.app.Application")
}

class ApplicationMethodInstrumentable : MethodInstrumentable {

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters,
    ): MethodVisitor =
        ApplicationMethodVisitor(
            apiVersion = apiVersion,
            originalVisitor = originalVisitor,
            instrumentableContext = instrumentableContext,
        )

    override fun isInstrumentable(data: MethodContext): Boolean {
        // TODO: think about constructors as well
        // <init>, ()V
        // <clinit>, ()V

        // public void onCreate()
        // onCreate, ()V
        return data.name == "onCreate" && data.descriptor == "()V"
    }
}
