/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

/**
 * Test utility container for managing callbacks during KCP compilation tests.
 *
 * Used to verify that compiler-injected instrumentation code correctly
 * invokes callbacks at runtime.
 */
object TestCallbackContainer {
    private var callback: ((Boolean) -> Unit)? = null

    /**
     * Registers a callback to be invoked during test execution.
     * @param callback the callback function that receives an isImageRole flag
     */
    fun setCallback(callback: (Boolean) -> Unit) {
        this.callback = callback
    }

    /**
     * Invokes the registered callback if present.
     * @param isImageRole flag indicating whether the element has an image role
     */
    fun invokeCallback(isImageRole: Boolean = false) {
        callback?.invoke(isImageRole)
    }
}
