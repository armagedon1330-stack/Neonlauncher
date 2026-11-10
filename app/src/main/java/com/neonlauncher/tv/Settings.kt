package com.neonlauncher.tv

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.Window
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingRow(
    ctx: Context,
    title: String,
    private val accent: () -> Int,
    private val value: () -> String,
    private val onChange: (Int) -> Unit
) : LinearLayout(ctx) {
    private val titleView = TextView(ctx)
    private val valueView = TextView(ctx)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        val ph = Ui.ui(12f)
        val pv = Ui.ui(9f)
        setPadding(ph, pv, ph, pv)
        titleView.text = title
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.u(12f))
        titleView.setTextColor(Color.WHITE)
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.u(12f))
        valueView.setTextColor(0xFFB8C4D6.toInt())
        addView(titleView, LayoutParams(0, WRAP, 1f))
        addView(valueView, LayoutParams(WRAP, WRAP))
        refresh()
        updateBg()
    }

    fun refresh() {
        valueView.text = value()
    }

    private fun updateBg() {
        val bg = GradientDrawable()
        bg.cornerRadius = Ui.u(8f)
        if (isFocused) {
            bg.setColor(withAlpha(60, accent()))
            bg.setStroke(Math.max(1, Ui.ui(1.5f)), accent())
        } else {
            bg.setColor(0x14FFFFFF)
        }
        background = bg
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                onChange(-1)
                refresh()
                updateBg()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                onChange(1)
                refresh()
                updateBg()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        updateBg()
    }
}

class SettingsDialog(
    activity: Activity,
    private val prefs: Prefs,
    private val wallpaperDir: String,
    private val onChanged: () -> Unit
) : Dialog(activity) {

    private val accentNow: () -> Int = { Prefs.ACCENTS[prefs.accent].second }

    private fun stepRow(
        title: String,
        read: () -> Int,
        write: (Int) -> Unit,
        min: Int,
        max: Int,
        step: Int,
        suffix: String
    ): SettingRow {
        return SettingRow(context, title, accentNow, { "<  " + read() + suffix + "  >" }) { d ->
            write((read() + d * step).coerceIn(min, max))
            onChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val list = LinearLayout(context)
        list.orientation = LinearLayout.VERTICAL

        fun add(row: SettingRow) {
            val lp = LinearLayout.LayoutParams(MATCH, WRAP)
            lp.bottomMargin = Ui.ui(6f)
            list.addView(row, lp)
        }

        val n = Prefs.ACCENTS.size
        add(SettingRow(context, "Цвет акцента", accentNow,
            { "<  " + Prefs.ACCENTS[prefs.accent].first + "  >" }) { d ->
            prefs.accent = (prefs.accent + d + n) % n
            onChanged()
        })
        add(stepRow("Свечение", { prefs.glow }, { prefs.glow = it }, 0, 10, 1, ""))
        add(stepRow("Прозрачность стекла", { prefs.glass }, { prefs.glass = it }, 0, 40, 2, "%"))
        add(stepRow("Скругление", { prefs.radius }, { prefs.radius = it }, 0, 30, 2, ""))
        add(stepRow("Колонки", { prefs.cols }, { prefs.cols = it }, 4, 8, 1, ""))
        add(stepRow("Затемнение обоев", { prefs.dim }, { prefs.dim = it }, 0, 90, 5, "%"))
        add(SettingRow(context, "Названия приложений", accentNow,
            { if (prefs.labels) "Вкл" else "Выкл" }) {
            prefs.labels = !prefs.labels
            onChanged()
        })
        add(SettingRow(context, "Вернуть скрытые приложения", accentNow, { "OK" }) {
            prefs.hidden = emptySet()
            onChanged()
        })

        val scroll = ScrollView(context)
        scroll.isVerticalScrollBarEnabled = false
        scroll.addView(list, LinearLayout.LayoutParams(MATCH, WRAP))

        val title = TextView(context)
        title.text = "Настройки"
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.u(18f))
        title.setTextColor(Color.WHITE)
        title.setPadding(Ui.ui(4f), 0, 0, Ui.ui(10f))

        val hint = TextView(context)
        hint.text = "Скрыть приложение: долгое нажатие OK на плитке.\n" +
            "Обои: положите wallpaper.mp4 или wallpaper.jpg в папку\n" + wallpaperDir
        hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.u(8.5f))
        hint.setTextColor(0xFF7F8CA0.toInt())
        hint.setPadding(Ui.ui(4f), Ui.ui(8f), 0, 0)

        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable()
        bg.setColor(0xF2101420.toInt())
        val r = Ui.u(14f)
        bg.cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
        root.background = bg
        val p = Ui.ui(18f)
        root.setPadding(p, p, p, p)
        root.addView(title, LinearLayout.LayoutParams(MATCH, WRAP))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(hint, LinearLayout.LayoutParams(MATCH, WRAP))

        setContentView(root)
        window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(Ui.ui(400f), MATCH)
            w.setGravity(Gravity.END)
            w.setDimAmount(0.45f)
        }
    }
}
