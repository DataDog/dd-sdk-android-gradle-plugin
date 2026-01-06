/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

object TestCallbackContainer {
    private var callback: ((Boolean) -> Unit)? = null

    fun setCallback(callback: (Boolean) -> Unit) {
        this.callback = callback
    }

    fun invokeCallback(isImageRole: Boolean = false) {
        callback?.invoke(isImageRole)
    }
}
