package org.desmume.android

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/** Draws both DS screens and an on-screen gamepad, and turns touches into DS input. */
class EmulatorView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    var emu: EmulatorThread? = null

    private val bmp = Bitmap.createBitmap(NativeCore.SCREEN_W, NativeCore.SCREEN_H * 2, Bitmap.Config.ARGB_8888)
    private val bmpPaint = Paint().apply { isFilterBitmap = false }
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF; style = Paint.Style.FILL }
    private val btnPressed = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAFFFFFF.toInt(); style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textAlign = Paint.Align.CENTER }

    private class Btn(val label: String, val key: Int, val rect: RectF)
    // Rebuilt on the UI thread, read on the emulator thread: build a fresh list, then swap the reference.
    @Volatile private var buttons: List<Btn> = emptyList()
    private var building = ArrayList<Btn>()
    private val topSrc = Rect(0, 0, NativeCore.SCREEN_W, NativeCore.SCREEN_H)
    private val botSrc = Rect(0, NativeCore.SCREEN_H, NativeCore.SCREEN_W, NativeCore.SCREEN_H * 2)
    private val topDst = RectF()
    private val botDst = RectF()
    @Volatile private var pressedKeys = 0
    private var surfaceReady = false

    init { holder.addCallback(this); isFocusable = true }

    override fun surfaceCreated(h: SurfaceHolder) { surfaceReady = true }
    override fun surfaceDestroyed(h: SurfaceHolder) { surfaceReady = false }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) { layoutFor(w, hh) }

    private fun layoutFor(w: Int, h: Int) {
        building = ArrayList()
        val sw = NativeCore.SCREEN_W.toFloat(); val sh = NativeCore.SCREEN_H.toFloat()
        val landscape = w > h
        val scale: Float
        val left: Float
        if (landscape) { scale = minOf(w / sw, h / (sh * 2)); left = (w - sw * scale) / 2 }
        else { scale = w / sw; left = 0f }
        topDst.set(left, 0f, left + sw * scale, sh * scale)
        botDst.set(left, sh * scale, left + sw * scale, sh * scale * 2)

        val u = minOf(w, h) / 9f           // base button size
        if (landscape) {
            val cy = h * 0.62f
            dpad(u * 2.2f, cy, u)
            face(w - u * 2.2f, cy, u)
            building.add(Btn("L", NativeCore.KEY_L, RectF(u * 0.3f, u * 0.3f, u * 2.5f, u * 1.3f)))
            building.add(Btn("R", NativeCore.KEY_R, RectF(w - u * 2.5f, u * 0.3f, w - u * 0.3f, u * 1.3f)))
            building.add(Btn("SEL", NativeCore.KEY_SELECT, RectF(u * 0.3f, h - u * 1.4f, u * 2.0f, h - u * 0.5f)))
            building.add(Btn("START", NativeCore.KEY_START, RectF(w - u * 2.0f, h - u * 1.4f, w - u * 0.3f, h - u * 0.5f)))
        } else {
            val top = botDst.bottom
            val avail = h - top
            val cy = top + avail * 0.5f
            val uu = minOf(u, avail / 6f)
            dpad(uu * 2.4f, cy, uu)
            face(w - uu * 2.4f, cy, uu)
            building.add(Btn("L", NativeCore.KEY_L, RectF(uu * 0.3f, top + uu * 0.2f, uu * 2.6f, top + uu * 1.1f)))
            building.add(Btn("R", NativeCore.KEY_R, RectF(w - uu * 2.6f, top + uu * 0.2f, w - uu * 0.3f, top + uu * 1.1f)))
            val mx = w / 2f
            building.add(Btn("SEL", NativeCore.KEY_SELECT, RectF(mx - uu * 1.9f, h - uu * 1.3f, mx - uu * 0.1f, h - uu * 0.4f)))
            building.add(Btn("START", NativeCore.KEY_START, RectF(mx + uu * 0.1f, h - uu * 1.3f, mx + uu * 1.9f, h - uu * 0.4f)))
        }
        textPaint.textSize = u * 0.5f
        buttons = building
    }

    private fun dpad(cx: Float, cy: Float, u: Float) {
        building.add(Btn("▲", NativeCore.KEY_UP, RectF(cx - u / 2, cy - u * 1.5f, cx + u / 2, cy - u / 2)))
        building.add(Btn("▼", NativeCore.KEY_DOWN, RectF(cx - u / 2, cy + u / 2, cx + u / 2, cy + u * 1.5f)))
        building.add(Btn("◀", NativeCore.KEY_LEFT, RectF(cx - u * 1.5f, cy - u / 2, cx - u / 2, cy + u / 2)))
        building.add(Btn("▶", NativeCore.KEY_RIGHT, RectF(cx + u / 2, cy - u / 2, cx + u * 1.5f, cy + u / 2)))
    }

    private fun face(cx: Float, cy: Float, u: Float) {
        building.add(Btn("X", NativeCore.KEY_X, RectF(cx - u / 2, cy - u * 1.5f, cx + u / 2, cy - u / 2)))
        building.add(Btn("B", NativeCore.KEY_B, RectF(cx - u / 2, cy + u / 2, cx + u / 2, cy + u * 1.5f)))
        building.add(Btn("Y", NativeCore.KEY_Y, RectF(cx - u * 1.5f, cy - u / 2, cx - u / 2, cy + u / 2)))
        building.add(Btn("A", NativeCore.KEY_A, RectF(cx + u / 2, cy - u / 2, cx + u * 1.5f, cy + u / 2)))
    }

    /** Called from the emulator thread with a 256x384 ARGB frame. */
    fun present(pixels: IntArray) {
        if (!surfaceReady) return
        // Native side emits raw R,G,B,A byte order, which is exactly ARGB_8888's in-memory layout.
        bmp.copyPixelsFromBuffer(java.nio.IntBuffer.wrap(pixels))
        val c = try { holder.lockCanvas() } catch (e: Exception) { null } ?: return
        try {
            c.drawColor(Color.BLACK)
            c.drawBitmap(bmp, topSrc, topDst, bmpPaint)
            c.drawBitmap(bmp, botSrc, botDst, bmpPaint)
            val pk = pressedKeys
            for (b in buttons) {
                c.drawRoundRect(b.rect, 18f, 18f, if (pk and b.key != 0) btnPressed else btnPaint)
                c.drawText(b.label, b.rect.centerX(), b.rect.centerY() + textPaint.textSize / 3, textPaint)
            }
        } finally { holder.unlockCanvasAndPost(c) }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val t = emu ?: return true
        var keys = 0
        var touching = false
        val up = e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL
        if (!up) {
            for (i in 0 until e.pointerCount) {
                if (e.actionMasked == MotionEvent.ACTION_POINTER_UP && i == e.actionIndex) continue
                val x = e.getX(i); val y = e.getY(i)
                if (botDst.contains(x, y)) {
                    if (!touching) {
                        touching = true
                        t.touchX = ((x - botDst.left) / botDst.width() * NativeCore.SCREEN_W).toInt()
                        t.touchY = ((y - botDst.top) / botDst.height() * NativeCore.SCREEN_H).toInt()
                    }
                } else {
                    for (b in buttons) if (b.rect.contains(x, y)) keys = keys or b.key
                }
            }
        }
        pressedKeys = keys
        t.touching = touching
        t.touchKeys = keys
        return true
    }
}
