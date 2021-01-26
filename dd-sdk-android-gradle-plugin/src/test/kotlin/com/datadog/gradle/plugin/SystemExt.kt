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
