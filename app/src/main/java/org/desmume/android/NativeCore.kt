package org.desmume.android

/** JNI surface of the DeSmuME core. Function names must match jni_bridge.cpp. */
object NativeCore {
    init { System.loadLibrary("desmume_jni") }

    const val SCREEN_W = 256
    const val SCREEN_H = 192
    const val SAMPLE_RATE = 44100

    // Button bits, identical to DS_KEY_* in desmume_core.h
    const val KEY_A = 1 shl 0
    const val KEY_B = 1 shl 1
    const val KEY_SELECT = 1 shl 2
    const val KEY_START = 1 shl 3
    const val KEY_RIGHT = 1 shl 4
    const val KEY_LEFT = 1 shl 5
    const val KEY_UP = 1 shl 6
    const val KEY_DOWN = 1 shl 7
    const val KEY_R = 1 shl 8
    const val KEY_L = 1 shl 9
    const val KEY_X = 1 shl 10
    const val KEY_Y = 1 shl 11

    @JvmStatic external fun init(dataDir: String): Int
    @JvmStatic external fun shutdown()
    @JvmStatic external fun loadRom(path: String): Int
    @JvmStatic external fun reset()
    @JvmStatic external fun runFrame()
    @JvmStatic external fun getFrame(out: IntArray)
    @JvmStatic external fun setInput(keys: Int, touching: Boolean, x: Int, y: Int)
    @JvmStatic external fun readAudio(out: ShortArray, maxFrames: Int): Int
    @JvmStatic external fun setVolume(volume: Int)
    @JvmStatic external fun saveState(path: String): Int
    @JvmStatic external fun loadState(path: String): Int
    @JvmStatic external fun flushSave()
    @JvmStatic external fun setLanguage(lang: Int)
}
