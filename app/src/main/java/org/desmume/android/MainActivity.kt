package org.desmume.android

import android.net.Uri
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var view: EmulatorView
    private lateinit var emu: EmulatorThread
    private var romName: String? = null

    private val pickRom = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importAndLoad(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view = EmulatorView(this)
        setContentView(view)
        hideSystemUi()

        emu = EmulatorThread(view, File(filesDir, "desmume").apply { mkdirs() }.absolutePath)
        view.emu = emu
        emu.start()

        val last = getPreferences(MODE_PRIVATE).getString("lastRom", null)
        if (last != null && File(last).exists()) loadRom(File(last)) else pickRom.launch(arrayOf("*/*"))
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    // ---- ROM handling --------------------------------------------------------------------

    private fun importAndLoad(uri: Uri) {
        Thread {
            try {
                val name = (contentResolver.query(uri, arrayOf("_display_name"), null, null, null)
                    ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "game.nds")
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                val dest = File(File(filesDir, "roms").apply { mkdirs() }, name)
                contentResolver.openInputStream(uri)!!.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
                runOnUiThread { loadRom(dest) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Could not read ROM: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun loadRom(file: File) {
        emu.active = false
        emu.post {
            val rc = NativeCore.loadRom(file.absolutePath)
            if (rc == 0) {
                emu.active = true
                romName = file.nameWithoutExtension
                getPreferences(MODE_PRIVATE).edit().putString("lastRom", file.absolutePath).apply()
            } else runOnUiThread {
                Toast.makeText(this, "Failed to load ${file.name} (not a valid .nds?)", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun stateFile(): File? {
        val n = romName ?: return null
        return File(File(filesDir, "states").apply { mkdirs() }, "$n.state1")
    }

    // ---- Menu (Back button) --------------------------------------------------------------

    private fun showMenu() {
        emu.paused = true
        val items = arrayOf("Resume", "Open ROM…", "Reset", "Save state", "Load state", "Exit")
        AlertDialog.Builder(this).setTitle("DeSmuME")
            .setItems(items) { _, which ->
                when (which) {
                    1 -> pickRom.launch(arrayOf("*/*"))
                    2 -> emu.post { NativeCore.reset() }
                    3 -> stateFile()?.let { f -> emu.post {
                        val ok = NativeCore.saveState(f.absolutePath) == 0
                        toast(if (ok) "State saved" else "Save failed") } }
                    4 -> stateFile()?.let { f -> emu.post {
                        val ok = f.exists() && NativeCore.loadState(f.absolutePath) == 0
                        toast(if (ok) "State loaded" else "No state / load failed") } }
                    5 -> finish()
                }
                emu.paused = false
            }
            .setOnCancelListener { emu.paused = false }
            .show()
    }

    private fun toast(msg: String) = runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

    override fun onBackPressed() { showMenu() }

    // ---- Lifecycle -----------------------------------------------------------------------

    override fun onPause() { super.onPause(); emu.paused = true; emu.post { NativeCore.flushSave() } }
    override fun onResume() { super.onResume(); hideSystemUi(); emu.paused = false }
    override fun onDestroy() {
        super.onDestroy()
        emu.running = false
        emu.join(2000)
    }

    // ---- Hardware gamepad / keyboard -----------------------------------------------------

    private fun mapKey(code: Int): Int = when (code) {
        // Nintendo layout: Android "B" (east) is the DS A button, etc.
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_X -> NativeCore.KEY_A
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_Z -> NativeCore.KEY_B
        KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_S -> NativeCore.KEY_X
        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_A -> NativeCore.KEY_Y
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q -> NativeCore.KEY_L
        KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_W -> NativeCore.KEY_R
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> NativeCore.KEY_START
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_DEL -> NativeCore.KEY_SELECT
        KeyEvent.KEYCODE_DPAD_UP -> NativeCore.KEY_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> NativeCore.KEY_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> NativeCore.KEY_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> NativeCore.KEY_RIGHT
        else -> 0
    }

    private var hatKeys = 0
    private var buttonKeys = 0

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val k = mapKey(e.keyCode)
        if (k != 0) {
            buttonKeys = if (e.action == KeyEvent.ACTION_DOWN) buttonKeys or k else buttonKeys and k.inv()
            emu.hwKeys = buttonKeys or hatKeys
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    override fun dispatchGenericMotionEvent(e: MotionEvent): Boolean {
        if (e.source and InputDevice.SOURCE_JOYSTICK != 0 && e.action == MotionEvent.ACTION_MOVE) {
            var k = 0
            val hx = e.getAxisValue(MotionEvent.AXIS_HAT_X); val hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y)
            val lx = e.getAxisValue(MotionEvent.AXIS_X); val ly = e.getAxisValue(MotionEvent.AXIS_Y)
            if (hx < -0.5f || lx < -0.5f) k = k or NativeCore.KEY_LEFT
            if (hx > 0.5f || lx > 0.5f) k = k or NativeCore.KEY_RIGHT
            if (hy < -0.5f || ly < -0.5f) k = k or NativeCore.KEY_UP
            if (hy > 0.5f || ly > 0.5f) k = k or NativeCore.KEY_DOWN
            hatKeys = k
            emu.hwKeys = buttonKeys or hatKeys
            return true
        }
        return super.dispatchGenericMotionEvent(e)
    }
}
