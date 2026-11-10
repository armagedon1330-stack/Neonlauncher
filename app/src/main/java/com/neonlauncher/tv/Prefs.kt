package com.neonlauncher.tv

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("neon", Context.MODE_PRIVATE)

    private fun int(key: String, def: Int): Int = sp.getInt(key, def)
    private fun put(key: String, v: Int) { sp.edit().putInt(key, v).apply() }

    var accent: Int
        get() = int("accent", 1).coerceIn(0, ACCENTS.size - 1)
        set(v) = put("accent", v)

    var glow: Int
        get() = int("glow", 6)
        set(v) = put("glow", v)

    var glass: Int
        get() = int("glass", 12)
        set(v) = put("glass", v)

    var radius: Int
        get() = int("radius", 12)
        set(v) = put("radius", v)

    var cols: Int
        get() = int("cols", 6)
        set(v) = put("cols", v)

    var dim: Int
        get() = int("dim", 35)
        set(v) = put("dim", v)

    var labels: Boolean
        get() = sp.getBoolean("labels", true)
        set(v) { sp.edit().putBoolean("labels", v).apply() }

    var hidden: Set<String>
        get() = HashSet<String>(sp.getStringSet("hidden", emptySet<String>()) ?: emptySet<String>())
        set(v) { sp.edit().putStringSet("hidden", HashSet<String>(v)).apply() }

    companion object {
        val ACCENTS: List<Pair<String, Int>> = listOf(
            "Мята" to 0xFF19F2B1.toInt(),
            "Голубой" to 0xFF2EA8FF.toInt(),
            "Фиолет" to 0xFFA855F7.toInt(),
            "Розовый" to 0xFFFF3D8B.toInt(),
            "Красный" to 0xFFFF4D4D.toInt(),
            "Оранжевый" to 0xFFFF9F1C.toInt(),
            "Белый" to 0xFFFFFFFF.toInt()
        )
    }
}
