/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.example.ndk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

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
        toaster.toast(stringFromJNI())
    }

    private external fun stringFromJNI(): String

    private companion object {
        init {
            System.loadLibrary("ndk")
        }
    }
}
