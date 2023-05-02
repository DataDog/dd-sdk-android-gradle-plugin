/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
fun setEnv(key: String, value: String) {
    try {
        val env = System.getenv()
        val cl: Class<*> = env.javaClass
        val field: Field = cl.getDeclaredField("m")
        field.isAccessible = true
        val writableEnv = field.get(env) as MutableMap<String, String>
        writableEnv[key] = value
    } catch (e: Exception) {
        throw IllegalStateException("Failed to set environment variable", e)
    }
}

@Suppress("UNCHECKED_CAST")
fun removeEnv(key: String) {
    try {
        val env = System.getenv()
        val cl: Class<*> = env.javaClass
        val field: Field = cl.getDeclaredField("m")
        field.isAccessible = true
        val writableEnv = field.get(env) as MutableMap<String, String>
        writableEnv.remove(key)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to remove environment variable", e)
    }
}
