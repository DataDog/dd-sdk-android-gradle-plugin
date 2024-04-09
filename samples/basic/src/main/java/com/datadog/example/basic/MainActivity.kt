@file:Suppress("UnusedImports")

package com.datadog.example.basic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.datadog.example.lib.Placeholder // ktlint-disable no-unused-imports unused import is on purpose

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
