package com.example.greenbar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var p: Prefs
    private lateinit var preview: View
    private lateinit var sw: Switch
    private lateinit var sr: SeekBar
    private lateinit var sg: SeekBar
    private lateinit var sb: SeekBar
    private lateinit var permBtn: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        p = Prefs(this)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(20))
        }
        setContentView(ScrollView(this).apply { addView(root) })

        root.addView(TextView(this).apply { text = "Green Bar"; textSize = 26f })

        permBtn = Button(this).apply {
            text = "Step 1: Allow 'Display over other apps'"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        }
        root.addView(permBtn)

        sw = Switch(this).apply {
            text = "Step 2: Turn colour bar on"
            textSize = 18f
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(sw)

        root.addView(label("Preview"))
        preview = View(this)
        root.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)))

        root.addView(label("Quick colours"))
        val row = LinearLayout(this)
        val presets = listOf(
            Color.rgb(0, 230, 0), Color.rgb(0, 150, 255), Color.rgb(255, 60, 60),
            Color.rgb(255, 200, 0), Color.rgb(170, 70, 255), Color.BLACK
        )
        for (c in presets) {
            val btn = Button(this).apply {
                setBackgroundColor(c)
                setOnClickListener {
                    sr.progress = Color.red(c); sg.progress = Color.green(c); sb.progress = Color.blue(c)
                }
            }
            row.addView(btn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        root.addView(row)

        val c0 = p.color
        sr = slider(root, "Red", 255, Color.red(c0)) { colorChanged() }
        sg = slider(root, "Green", 255, Color.green(c0)) { colorChanged() }
        sb = slider(root, "Blue", 255, Color.blue(c0)) { colorChanged() }

        val auto = CheckBox(this).apply {
            text = "Match system status bar height (auto, follows rotation)"
            isChecked = p.autoHeight
        }
        root.addView(auto)
        val hs = slider(root, "Height (dp)", 100, p.heightDp) { v -> p.heightDp = v; push() }
        hs.isEnabled = !p.autoHeight
        auto.setOnCheckedChangeListener { _, on ->
            p.autoHeight = on; hs.isEnabled = !on; push()
        }
        slider(root, "Opacity %", 100, p.opacity) { v -> p.opacity = v; refreshPreview(); push() }

        root.addView(Button(this).apply {
            text = "Keep running: battery settings"
            setOnClickListener {
                val pm = getSystemService(PowerManager::class.java)
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")))
                } else {
                    Toast.makeText(this@MainActivity, "Already unrestricted", Toast.LENGTH_SHORT).show()
                }
            }
        })

        sw.setOnCheckedChangeListener { _, on ->
            if (on && !Settings.canDrawOverlays(this)) {
                sw.isChecked = false
                Toast.makeText(this, "Do Step 1 first", Toast.LENGTH_LONG).show()
            } else {
                p.enabled = on
                if (on) push() else stopService(Intent(this, OverlayService::class.java))
            }
        }
        refreshPreview()
    }

    override fun onResume() {
        super.onResume()
        val ok = Settings.canDrawOverlays(this)
        permBtn.text = if (ok) "Step 1: Permission granted ✓" else "Step 1: Allow 'Display over other apps'"
        sw.isChecked = p.enabled && ok
    }

    private fun label(t: String) = TextView(this).apply { text = t; setPadding(0, dp(14), 0, 0) }

    private fun slider(root: LinearLayout, name: String, max: Int, init: Int, on: (Int) -> Unit): SeekBar {
        val tv = label("$name: $init")
        val sb = SeekBar(this).apply { this.max = max; progress = init }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, v: Int, user: Boolean) { tv.text = "$name: $v"; on(v) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
        root.addView(tv); root.addView(sb)
        return sb
    }

    private fun colorChanged() {
        p.color = Color.rgb(sr.progress, sg.progress, sb.progress)
        refreshPreview(); push()
    }

    private fun refreshPreview() {
        val a = p.opacity * 255 / 100
        preview.setBackgroundColor((a shl 24) or (p.color and 0xFFFFFF))
    }

    private fun push() {
        if (p.enabled && Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, OverlayService::class.java))
        }
    }
}
