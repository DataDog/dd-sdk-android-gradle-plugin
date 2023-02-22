package com.datadog.example.variants

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.datadog.example.variants.ext.toast
import com.datadog.example.lib.Placeholder // ktlint-disable no-unused-imports unused import is on purpose

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
