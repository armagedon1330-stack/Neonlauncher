package com.neonlauncher.tv

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onLong: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    var items: List<AppEntry> = emptyList()
    var style = Style(0xFF2EA8FF.toInt(), 6, 12, 12f, true)
    var cellW = 100
    var cellH = 100

    class VH(val tile: Tile) : RecyclerView.ViewHolder(tile)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(Tile(parent.context))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.tile.bind(e, style, cellW)
        holder.tile.layoutParams = RecyclerView.LayoutParams(MATCH, cellH)
        holder.tile.setOnClickListener { onClick(e) }
        holder.tile.setOnLongClickListener {
            onLong(e)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var wallpaper: WallpaperView
    private lateinit var dim: View
    private lateinit var rv: RecyclerView
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var netPill: StatusPill
    private lateinit var vpnPill: StatusPill
    private lateinit var settingsPill: StatusPill

    private val handler = Handler(Looper.getMainLooper())
    private var allApps: List<AppEntry> = emptyList()
    private var screenW = 1920
    private var firstFill = true

    private val adapter = AppAdapter(
        onClick = { launch(it) },
        onLong = { hide(it) }
    )

    private val ticker = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dm = resources.displayMetrics
        screenW = Math.max(dm.widthPixels, dm.heightPixels)
        Ui.w = screenW.toFloat()
        prefs = Prefs(this)
        buildUi()
        applyStyle()
    }

    private fun buildUi() {
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        wallpaper = WallpaperView(this)
        root.addView(wallpaper, FrameLayout.LayoutParams(MATCH, MATCH))

        dim = View(this)
        root.addView(dim, FrameLayout.LayoutParams(MATCH, MATCH))

        val side = Ui.ui(40f)
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(side, Ui.ui(22f), side, 0)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = android.view.Gravity.CENTER_VERTICAL

        val timeBox = LinearLayout(this)
        timeBox.orientation = LinearLayout.VERTICAL
        clock = TextView(this)
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.u(34f))
        clock.setTextColor(Color.WHITE)
        clock.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        date = TextView(this)
        date.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.u(11f))
        date.setTextColor(0xB3FFFFFF.toInt())
        timeBox.addView(clock)
        timeBox.addView(date)
        bar.addView(timeBox, LinearLayout.LayoutParams(0, WRAP, 1f))

        netPill = StatusPill(this)
        vpnPill = StatusPill(this)
        settingsPill = StatusPill(this)
        settingsPill.isFocusable = true
        settingsPill.isClickable = true
        settingsPill.text = "НАСТРОЙКИ"
        settingsPill.setOnClickListener { openSettings() }
        for (p in listOf(netPill, vpnPill, settingsPill)) {
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.leftMargin = Ui.ui(8f)
            bar.addView(p, lp)
        }
        content.addView(bar, LinearLayout.LayoutParams(MATCH, WRAP))

        rv = RecyclerView(this)
        rv.layoutManager = GridLayoutManager(this, 6)
        rv.adapter = adapter
        rv.clipToPadding = false
        rv.clipChildren = false
        rv.itemAnimator = null
        rv.setHasFixedSize(true)
        rv.overScrollMode = View.OVER_SCROLL_NEVER
        rv.setPadding(0, Ui.ui(6f), 0, Ui.ui(40f))
        val lp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        lp.topMargin = Ui.ui(14f)
        content.addView(rv, lp)

        root.addView(content, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
    }

    private fun applyStyle() {
        val accent = Prefs.ACCENTS[prefs.accent].second
        val style = Style(accent, prefs.glow, prefs.glass, prefs.radius.toFloat(), prefs.labels)
        dim.setBackgroundColor(Color.argb(prefs.dim * 255 / 100, 0, 0, 0))
        val cols = prefs.cols
        (rv.layoutManager as GridLayoutManager).spanCount = cols
        val gridW = screenW - 2 * Ui.ui(40f)
        adapter.style = style
        adapter.cellW = gridW / cols
        adapter.cellH = (adapter.cellW * 0.86f).toInt()
        adapter.notifyDataSetChanged()
        for (p in listOf(netPill, vpnPill, settingsPill)) {
            p.accent = accent
            p.render()
        }
        wallpaper.setAccent(accent)
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val seen = HashSet<String>()
            val out = ArrayList<AppEntry>()
            for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
                val i = Intent(Intent.ACTION_MAIN).addCategory(cat)
                for (ri in pm.queryIntentActivities(i, 0)) {
                    val pkg = ri.activityInfo.packageName
                    if (pkg == packageName || !seen.add(pkg)) continue
                    out.add(
                        AppEntry(
                            pkg,
                            ri.loadLabel(pm).toString(),
                            ComponentName(pkg, ri.activityInfo.name),
                            ri.loadIcon(pm)
                        )
                    )
                }
            }
            out.sortBy { it.label.lowercase() }
            runOnUiThread {
                if (!isDestroyed) {
                    allApps = out
                    rebuildList()
                }
            }
        }.start()
    }

    private fun rebuildList() {
        val h = prefs.hidden
        adapter.items = allApps.filter { it.pkg !in h }
        adapter.notifyDataSetChanged()
        if (firstFill && adapter.items.isNotEmpty()) {
            firstFill = false
            rv.post { rv.getChildAt(0)?.requestFocus() }
        }
    }

    private fun launch(e: AppEntry) {
        try {
            val i = Intent(Intent.ACTION_MAIN)
            i.component = e.component
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(i)
        } catch (ex: Exception) {
            Toast.makeText(this, "Не удалось открыть: " + e.label, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hide(e: AppEntry) {
        prefs.hidden = prefs.hidden + e.pkg
        Toast.makeText(this, "Скрыто: " + e.label + " (вернуть: Настройки)", Toast.LENGTH_SHORT).show()
        rebuildList()
    }

    private fun openSettings() {
        val dir = getExternalFilesDir(null)?.absolutePath ?: ""
        SettingsDialog(this, prefs, dir) {
            applyStyle()
            rebuildList()
        }.show()
    }

    private fun updateStatus() {
        val now = Date()
        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)

        var vpn = false
        var net = "Нет сети"
        var connected = false
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            for (n in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(n) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    vpn = true
                    continue
                }
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    net = "Wi-Fi"
                    connected = true
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    net = "Ethernet"
                    connected = true
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    net = "Мобильная сеть"
                    connected = true
                }
            }
        } catch (e: Exception) {
        }
        netPill.text = if (connected) "● $net" else net
        netPill.active = connected
        netPill.render()
        vpnPill.text = if (vpn) "● VPN ВКЛ" else "VPN ВЫКЛ"
        vpnPill.active = vpn
        vpnPill.render()
    }

    override fun onStart() {
        super.onStart()
        wallpaper.reload(getExternalFilesDir(null))
        handler.post(ticker)
        loadApps()
    }

    override fun onStop() {
        super.onStop()
        wallpaper.pause()
        handler.removeCallbacks(ticker)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        rv.scrollToPosition(0)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            openSettings()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        rv.scrollToPosition(0)
    }
}
