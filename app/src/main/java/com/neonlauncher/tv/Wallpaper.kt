package com.neonlauncher.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import java.io.File

/** Фон: картинка или зацикленное видео без звука. Если файла нет, рисуется тёмный градиент. */
class WallpaperView(ctx: Context) : FrameLayout(ctx), TextureView.SurfaceTextureListener {
    private val img = ImageView(ctx)
    private val tex = TextureView(ctx)
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var videoFile: File? = null
    private var vw = 0
    private var vh = 0

    init {
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(img, LayoutParams(MATCH, MATCH))
        addView(tex, LayoutParams(MATCH, MATCH))
        tex.visibility = View.GONE
        tex.surfaceTextureListener = this
        setAccent(0xFF2EA8FF.toInt())
    }

    fun setAccent(accent: Int) {
        val g = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF0A0F1E.toInt(), 0xFF05070D.toInt(), withAlpha(255, blend(accent)))
        )
        background = g
    }

    private fun blend(c: Int): Int {
        // сильно затемнённый акцент для угла градиента
        val r = (android.graphics.Color.red(c) * 0.12f).toInt()
        val gr = (android.graphics.Color.green(c) * 0.12f).toInt()
        val b = (android.graphics.Color.blue(c) * 0.12f).toInt()
        return android.graphics.Color.rgb(r, gr, b)
    }

    fun reload(dir: File?) {
        stopVideo()
        img.setImageDrawable(null)
        tex.visibility = View.GONE
        videoFile = null
        if (dir == null) return

        val video = listOf("mp4", "webm", "mkv", "mov").map { File(dir, "wallpaper.$it") }.firstOrNull { it.exists() }
        if (video != null) {
            videoFile = video
            tex.visibility = View.VISIBLE
            if (tex.surfaceTexture != null) startVideo()
            return
        }
        val image = listOf("jpg", "jpeg", "png", "webp").map { File(dir, "wallpaper.$it") }.firstOrNull { it.exists() }
        if (image != null) {
            val bmp = decode(image)
            if (bmp != null) img.setImageBitmap(bmp)
        }
    }

    fun pause() {
        stopVideo()
    }

    private fun decode(f: File): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            var s = 1
            while (bounds.outWidth / (s * 2) >= 1920) s *= 2
            val o = BitmapFactory.Options()
            o.inSampleSize = s
            o.inPreferredConfig = Bitmap.Config.RGB_565
            BitmapFactory.decodeFile(f.absolutePath, o)
        } catch (e: Throwable) {
            null
        }
    }

    private fun startVideo() {
        val f = videoFile ?: return
        val st = tex.surfaceTexture ?: return
        stopVideo()
        try {
            val s = Surface(st)
            surface = s
            val mp = MediaPlayer()
            mp.setDataSource(f.absolutePath)
            mp.setSurface(s)
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            mp.setOnVideoSizeChangedListener { _, w, h ->
                vw = w
                vh = h
                fitCrop()
            }
            mp.setOnPreparedListener { it.start() }
            mp.setOnErrorListener { _, _, _ ->
                stopVideo()
                true
            }
            player = mp
            mp.prepareAsync()
        } catch (e: Exception) {
            stopVideo()
        }
    }

    private fun stopVideo() {
        val mp = player
        player = null
        if (mp != null) {
            try {
                mp.stop()
            } catch (e: Exception) {
            }
            mp.release()
        }
        surface?.release()
        surface = null
    }

    private fun fitCrop() {
        val w = tex.width.toFloat()
        val h = tex.height.toFloat()
        if (vw == 0 || vh == 0 || w == 0f || h == 0f) return
        val scale = Math.max(w / vw, h / vh)
        val m = Matrix()
        m.setScale(vw * scale / w, vh * scale / h, w / 2f, h / 2f)
        tex.setTransform(m)
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
        startVideo()
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
        fitCrop()
    }

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        stopVideo()
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
}
