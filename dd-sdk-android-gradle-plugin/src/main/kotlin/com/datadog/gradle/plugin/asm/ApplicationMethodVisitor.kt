/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.asm

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter

class ApplicationMethodVisitor(
    apiVersion: Int,
    originalVisitor: MethodVisitor,
    instrumentableContext: MethodContext,
) :
    AdviceAdapter(
        apiVersion,
        originalVisitor,
        instrumentableContext.access,
        instrumentableContext.name,
        instrumentableContext.descriptor,
    ) {

    override fun onMethodEnter() {
        super.onMethodEnter()

        loadThis()
        visitMethodInsn(
            INVOKESTATIC,
            "com/datadog/android/sample/AppStartMetrics",
            "onApplicationCreate",
            "(Landroid/app/Application;)V",
            false,
        )
    }

    override fun onMethodExit(opcode: Int) {
        super.onMethodExit(opcode)

        loadThis()
        visitMethodInsn(
            INVOKESTATIC,
            "com/datadog/android/sample/AppStartMetrics",
            "onApplicationPostCreate",
            "(Landroid/app/Application;)V",
            false,
        )
    }
}
