package com.neonlauncher.tv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.content.ComponentName

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** Размеры считаются от ширины экрана: 1000 единиц = вся ширина. Так вид не зависит от dpi телевизора. */
object Ui {
    var w = 1920f
    fun u(v: Float): Float = v * w / 1000f
    fun ui(v: Float): Int = u(v).toInt()
}

fun withAlpha(a: Int, c: Int): Int = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

class AppEntry(
    val pkg: String,
    val label: String,
    val component: ComponentName,
    val icon: Drawable
)

data class Style(
    val accent: Int,
    val glow: Int,
    val glassPct: Int,
    val radius: Float,
    val showLabel: Boolean,
    val zoom: Float
)

class Tile(ctx: Context) : LinearLayout(ctx) {
    private val icon = ImageView(ctx)
    private val label = TextView(ctx)
    private var st = Style(0xFF2EA8FF.toInt(), 6, 12, 12f, true, 1.18f)
    var onFocusGained: ((View) -> Unit)? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val rect2 = RectF()
    private val pad: Float get() = Ui.u(6f)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isFocusable = true
        isClickable = true
        setWillNotDraw(false)
        clipChildren = false
        line.style = Paint.Style.STROKE
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.u(10.5f))
        label.setTextColor(0xE6FFFFFF.toInt())
        label.gravity = Gravity.CENTER
        label.maxLines = 1
        label.ellipsize = TextUtils.TruncateAt.END
        addView(icon, LayoutParams(Ui.ui(40f), Ui.ui(40f)))
        addView(label, LayoutParams(MATCH, WRAP))
    }

    fun bind(e: AppEntry, style: Style, cellW: Int) {
        st = style
        animate().cancel()
        val f = isFocused
        scaleX = if (f) style.zoom else 1f
        scaleY = if (f) style.zoom else 1f
        alpha = if (f) 1f else 0.78f
        val iconSize = (cellW * 0.44f).toInt()
        val lp = LayoutParams(iconSize, iconSize)
        lp.bottomMargin = if (style.showLabel) Ui.ui(5f) else 0
        icon.layoutParams = lp
        icon.setImageDrawable(e.icon)
        label.text = e.label
        label.visibility = VISIBLE
        label.animate().cancel()
        label.alpha = if (style.showLabel || f) 1f else 0f
        val p = (pad + Ui.u(4f)).toInt()
        setPadding(p, p, p, p)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        sheen.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        val p = pad
        rect.set(p, p, width - p, height - p)
        val r = Ui.u(st.radius)

        // стекло: полупрозрачная заливка + светлый блик сверху
        fill.style = Paint.Style.FILL
        fill.color = Color.argb(st.glassPct * 255 / 100, 255, 255, 255)
        c.drawRoundRect(rect, r, r, fill)
        c.drawRoundRect(rect, r, r, sheen)

        if (isFocused) {
            fill.color = withAlpha(46, st.accent)
            c.drawRoundRect(rect, r, r, fill)

            val k = st.glow / 10f
            if (k > 0f) {
                line.strokeWidth = Ui.u(2.2f)
                for (i in 5 downTo 1) {
                    val s = Ui.u(1f) * i
                    rect2.set(rect.left - s, rect.top - s, rect.right + s, rect.bottom + s)
                    line.color = withAlpha((k * 70f / i).toInt().coerceIn(0, 255), st.accent)
                    c.drawRoundRect(rect2, r + s, r + s, line)
                }
            }
            line.strokeWidth = Ui.u(1.6f)
            line.color = st.accent
            c.drawRoundRect(rect, r, r, line)
        } else {
            line.strokeWidth = Ui.u(1f)
            line.color = 0x40FFFFFF
            c.drawRoundRect(rect, r, r, line)
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        val z = if (gainFocus) st.zoom else 1f
        animate()
            .scaleX(z)
            .scaleY(z)
            .alpha(if (gainFocus) 1f else 0.78f)
            .translationZ(if (gainFocus) Ui.u(8f) else 0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()
        label.animate().alpha(if (gainFocus || st.showLabel) 1f else 0f).setDuration(200).start()
        if (gainFocus) onFocusGained?.invoke(this)
        invalidate()
    }
}

class StatusPill(ctx: Context) : TextView(ctx) {
    var accent: Int = 0xFF2EA8FF.toInt()
    var active: Boolean = false

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.u(11f))
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        val ph = Ui.ui(12f)
        val pv = Ui.ui(6f)
        setPadding(ph, pv, ph, pv)
        render()
    }

    fun render() {
        val bg = GradientDrawable()
        bg.cornerRadius = Ui.u(16f)
        val sw = Math.max(1, Ui.ui(1f))
        if (isFocused) {
            bg.setColor(withAlpha(70, accent))
            bg.setStroke(sw * 2, accent)
            setTextColor(Color.WHITE)
        } else if (active) {
            bg.setColor(withAlpha(36, accent))
            bg.setStroke(sw, accent)
            setTextColor(Color.WHITE)
        } else {
            bg.setColor(0x14FFFFFF)
            bg.setStroke(sw, 0x40FFFFFF)
            setTextColor(0xFF9AA5B5.toInt())
        }
        background = bg
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        render()
    }
}
