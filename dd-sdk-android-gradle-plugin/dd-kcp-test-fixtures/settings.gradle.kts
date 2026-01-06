/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

rootProject.name = "dd-kcp-test-fixtures"

// Read pluginVersion from root gradle.properties
val rootProperties = java.util.Properties().apply {
    file("../../gradle.properties").inputStream().use { load(it) }
}

gradle.beforeProject {
    extra["pluginVersion"] = rootProperties.getProperty("pluginVersion")
}
