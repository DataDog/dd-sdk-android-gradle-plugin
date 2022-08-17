/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

/**
 * Defines the Datadog sites you can send tracked data to.
 */
enum class DatadogSite {
    /**
     *  The US1 site: [app.datadoghq.com](https://app.datadoghq.com) (legacy name).
     */
    US,

    /**
     *  The US1 site: [app.datadoghq.com](https://app.datadoghq.com).
     */
    US1,

    /**
     *  The US3 site: [us3.datadoghq.com](https://us3.datadoghq.com).
     */
    US3,

    /**
     *  The US5 site: [us5.datadoghq.com](https://us5.datadoghq.com).
     */
    US5,

    /**
     *  The US1_FED site (FedRAMP compatible): [app.ddog-gov.com](https://app.ddog-gov.com) (legacy name).
     */
    GOV,

    /**
     *  The US1_FED site (FedRAMP compatible): [app.ddog-gov.com](https://app.ddog-gov.com).
     */
    US1_FED,

    /**
     *  The EU1 site: [app.datadoghq.eu](https://app.datadoghq.eu) (legacy name).
     */
    EU,

    /**
     *  The EU1 site: [app.datadoghq.eu](https://app.datadoghq.eu).
     */
    EU1;

    /**
     * Returns the endpoint to use to upload sourcemap to this site.
     */
    internal fun uploadEndpoint(): String {
        return when (this) {
            US, US1 -> "https://sourcemap-intake.datadoghq.com/api/v2/srcmap"
            US3 -> "https://sourcemap-intake.us3.datadoghq.com/api/v2/srcmap"
            US5 -> "https://sourcemap-intake.us5.datadoghq.com/api/v2/srcmap"
            GOV, US1_FED -> "https://sourcemap-intake.ddog-gov.com/api/v2/srcmap"
            EU, EU1 -> "https://sourcemap-intake.datadoghq.eu/api/v2/srcmap"
        }
    }

    companion object {
        internal val validIds = DatadogSite.values().map { it.name }
    }
}
