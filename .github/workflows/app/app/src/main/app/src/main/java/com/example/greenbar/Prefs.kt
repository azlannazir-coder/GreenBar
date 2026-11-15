package com.example.greenbar

import android.content.Context
import android.graphics.Color

class Prefs(c: Context) {
    private val sp = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) { sp.edit().putBoolean("enabled", v).apply() }
    var color: Int
        get() = sp.getInt("color", Color.rgb(0, 230, 0))
        set(v) { sp.edit().putInt("color", v).apply() }
    var autoHeight: Boolean
        get() = sp.getBoolean("auto", true)
        set(v) { sp.edit().putBoolean("auto", v).apply() }
    var heightDp: Int
        get() = sp.getInt("h", 24)
        set(v) { sp.edit().putInt("h", v).apply() }
    var opacity: Int
        get() = sp.getInt("op", 100)
        set(v) { sp.edit().putInt("op", v).apply() }
}
