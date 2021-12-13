package com.example.variants

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
// unused import is on purpose
import com.example.lib.Placeholder
import com.example.variants.ext.toast

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
