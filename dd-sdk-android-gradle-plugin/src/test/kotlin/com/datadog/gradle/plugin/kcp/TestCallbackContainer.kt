/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

/**
 * A container for storing callbacks passed from the unit test environment.
 *
 * This object should be loaded by the same class loader that loads the test target class file.
 * To ensure the test target function is invoked correctly, the function under test should
 * call [TestCallbackContainer.invokeCallback], allowing external verification of the callback invocation.
 */
object TestCallbackContainer {

    private var callback: (Boolean) -> Unit = {}

    /**
     * Sets the callback function to be invoked during testing.
     *
     * @param callback The function to be stored and later invoked.
     */
    fun setCallback(callback: (Boolean) -> Unit) {
        this.callback = callback
    }

    /**
     * Invokes the stored callback function.
     */
    fun invokeCallback(isImageRole: Boolean = false) {
        callback.invoke(isImageRole)
    }
}
