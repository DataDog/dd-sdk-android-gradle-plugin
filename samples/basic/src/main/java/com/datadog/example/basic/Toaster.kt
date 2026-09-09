/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.example.basic

import android.content.Context
import android.widget.Toast

internal class Toaster(context: Context) {

    val appContext = context.applicationContext

    fun toast(msg: String) {
        Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
    }
}
