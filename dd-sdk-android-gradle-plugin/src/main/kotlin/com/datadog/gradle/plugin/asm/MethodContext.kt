/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.asm

import com.android.build.api.instrumentation.ClassData
import com.android.build.gradle.internal.instrumentation.ClassContextImpl
import com.android.build.gradle.internal.instrumentation.ClassesDataCache
import com.android.build.gradle.internal.instrumentation.ClassesHierarchyResolver

data class MethodContext(
    val access: Int,
    val name: String?,
    val descriptor: String?,
    val signature: String?,
    val exceptions: List<String>?,
)

fun ClassData.toClassContext() =
    ClassContextImpl(this, ClassesHierarchyResolver.Builder(ClassesDataCache()).build())
