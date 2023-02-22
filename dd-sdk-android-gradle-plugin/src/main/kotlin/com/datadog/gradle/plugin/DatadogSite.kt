/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

/**
 * Defines the Datadog sites you can send tracked data to.
 */
enum class DatadogSite(internal val domain: String) {
    /**
     *  The US1 site: [app.datadoghq.com](https://app.datadoghq.com) (legacy name).
     */
    US("datadoghq.com"),

    /**
     *  The US1 site: [app.datadoghq.com](https://app.datadoghq.com).
     */
    US1("datadoghq.com"),

    /**
     *  The US3 site: [us3.datadoghq.com](https://us3.datadoghq.com).
     */
    US3("us3.datadoghq.com"),

    /**
     *  The US5 site: [us5.datadoghq.com](https://us5.datadoghq.com).
     */
    US5("us5.datadoghq.com"),

    /**
     *  The US1_FED site (FedRAMP compatible): [app.ddog-gov.com](https://app.ddog-gov.com) (legacy name).
     */
    GOV("ddog-gov.com"),

    /**
     *  The US1_FED site (FedRAMP compatible): [app.ddog-gov.com](https://app.ddog-gov.com).
     */
    US1_FED("ddog-gov.com"),

    /**
     *  The EU1 site: [app.datadoghq.eu](https://app.datadoghq.eu) (legacy name).
     */
    EU("datadoghq.eu"),

    /**
     *  The EU1 site: [app.datadoghq.eu](https://app.datadoghq.eu).
     */
    EU1("datadoghq.eu"),

    /**
     *  The AP1 site: [ap1.datadoghq.com](https://ap1.datadoghq.com).
     */
    AP1("ap1.datadoghq.com");

    /**
     * Returns the endpoint to use to upload sourcemap to this site.
     */
    internal fun uploadEndpoint(): String {
        return "https://sourcemap-intake.$domain/api/v2/srcmap"
    }

    internal fun apiKeyVerificationEndpoint(): String {
        return "https://api.$domain/api/v1/validate"
    }

    companion object {
        internal val validIds = DatadogSite.values().map { it.name }

        internal fun fromDomain(domain: String): DatadogSite? {
            return DatadogSite.values().firstOrNull { it.domain == domain }
        }
    }
}
