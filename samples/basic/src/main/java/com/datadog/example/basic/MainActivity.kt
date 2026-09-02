/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

@file:Suppress("UnusedImports", "ktlint:standard:no-unused-imports")

package com.datadog.example.basic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.datadog.example.lib.Placeholder // unused import is on purpose

/**
 * Main Activity for the sample app.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var toaster: Toaster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toaster = Toaster(this)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        toaster.toast("Hello world !")
    }
}
