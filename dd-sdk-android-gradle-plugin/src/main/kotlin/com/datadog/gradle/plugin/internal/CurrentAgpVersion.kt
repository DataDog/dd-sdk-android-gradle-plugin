/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.TaskUtils

@Suppress("MagicNumber")
internal object CurrentAgpVersion {

    // can work probably even with lower versions, but legacy Variant API is working fine there as well
    val CAN_ENABLE_NEW_VARIANT_API: Boolean
        get() = TaskUtils.isAgpEqualOrAbove(major = 8, minor = 4, patch = 0)

    val SUPPORTS_KOTLIN_DIRECTORIES_SOURCE_PROVIDER: Boolean
        get() = TaskUtils.isAgpEqualOrAbove(major = 7, minor = 0, patch = 0)

    val EXTERNAL_NATIVE_BUILD_SOFOLDER_IS_PUBLIC: Boolean
        get() = TaskUtils.isAgpEqualOrAbove(major = 8, minor = 0, patch = 0)

    val CAN_QUERY_MAPPING_FILE_PROVIDER: Boolean
        get() = TaskUtils.isAgpEqualOrAbove(major = 7, minor = 0, patch = 0)

    val IMPLEMENTS_BUILT_IN_KOTLIN: Boolean
        get() = TaskUtils.isAgpEqualOrAbove(major = 9, minor = 0, patch = 0)
}
