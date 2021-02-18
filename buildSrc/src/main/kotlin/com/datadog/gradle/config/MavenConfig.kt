/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import com.datadog.gradle.utils.Version

object MavenConfig {

    val VERSION = Version(1, 0, 0, Version.Type.Alpha(3))
    const val GROUP_ID = "com.datadoghq"
}
