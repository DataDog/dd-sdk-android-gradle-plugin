package com.example.variants

import android.content.Context
import android.widget.Toast

class Toaster(context: Context) {

    val appContext = context.applicationContext

    fun toast(msg: String) {
        Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
    }
}
