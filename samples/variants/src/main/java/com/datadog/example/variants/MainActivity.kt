/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.example.variants

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.datadog.example.variants.ext.toast

/**
 * Main Activity for the sample app.
 */
class MainActivity : AppCompatActivity() {

    private val varyingClass: VaryingInfo = VaryingClass()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        toast("Hello World, with ${varyingClass.getName()}")
    }
}
