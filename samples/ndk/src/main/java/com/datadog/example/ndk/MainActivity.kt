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

    companion object {
        init {
            System.loadLibrary("ndk")
        }
    }
}
