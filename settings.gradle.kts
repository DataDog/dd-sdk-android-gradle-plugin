/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

include(":dd-sdk-android-gradle-plugin")

// KCP modules as composite builds (allows different Kotlin versions per module)
includeBuild("dd-sdk-android-gradle-plugin/dd-kcp-common")
includeBuild("dd-sdk-android-gradle-plugin/dd-kcp-test-fixtures")
includeBuild("dd-sdk-android-gradle-plugin/dd-kcp-kotlin20")
includeBuild("dd-sdk-android-gradle-plugin/dd-kcp-kotlin21")
includeBuild("dd-sdk-android-gradle-plugin/dd-kcp-kotlin22")

include(":samples:basic")
include(":samples:ndk")
include(":samples:variants")
include(":samples:variants-kotlin")
include(":samples:lib-module")
include(":instrumented")
