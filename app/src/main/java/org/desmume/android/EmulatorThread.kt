package org.desmume.android

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock

/**
 * Runs the core on a dedicated thread: input -> one frame -> present -> audio.
 * All NativeCore emulation calls happen on this thread (the core is not thread-safe);
 * other threads post work with [post].
 */
class EmulatorThread(private val view: EmulatorView, private val dataDir: String) : Thread("DeSmuME-emu") {
    /** True once a ROM has been loaded successfully; frames only run while set. */
    @Volatile var active = false
    @Volatile var running = true
    @Volatile var paused = false
    @Volatile var touchKeys = 0   // from the on-screen pad
    @Volatile var hwKeys = 0      // from a hardware gamepad / keyboard
    @Volatile var touching = false
    @Volatile var touchX = 0
    @Volatile var touchY = 0

    private val tasks = java.util.concurrent.ConcurrentLinkedQueue<() -> Unit>()
    fun post(task: () -> Unit) { tasks.add(task) }

    private val frame = IntArray(NativeCore.SCREEN_W * NativeCore.SCREEN_H * 2)
    private val audioBuf = ShortArray(2048 * 2)

    private fun newAudioTrack(): AudioTrack? = try {
        val minBytes = AudioTrack.getMinBufferSize(
            NativeCore.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(NativeCore.SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(maxOf(minBytes, 4096 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build().also { it.play() }
    } catch (e: Exception) { null }

    override fun run() {
        if (NativeCore.init(dataDir) != 0) { running = false; return }
        NativeCore.setLanguage(1) // English
        val audio = newAudioTrack()
        val frameNanos = 1_000_000_000L / 59.8261   // DS refresh rate
        var next = SystemClock.elapsedRealtimeNanos()

        while (running) {
            while (true) { (tasks.poll() ?: break).invoke() }

            if (paused || !active) {
                audio?.pause(); sleep(20); next = SystemClock.elapsedRealtimeNanos(); continue
            }
            if (audio != null && audio.playState != AudioTrack.PLAYSTATE_PLAYING) audio.play()

            NativeCore.setInput(touchKeys or hwKeys, touching, touchX, touchY)
            NativeCore.runFrame()
            NativeCore.getFrame(frame)
            view.present(frame)

            if (audio != null) {
                while (true) {
                    val n = NativeCore.readAudio(audioBuf, audioBuf.size / 2)
                    if (n <= 0) break
                    audio.write(audioBuf, 0, n * 2)   // blocking; also helps pacing
                }
            }

            next += frameNanos.toLong()
            val wait = next - SystemClock.elapsedRealtimeNanos()
            if (wait > 0) sleep(wait / 1_000_000, (wait % 1_000_000).toInt())
            else if (wait < -100_000_000L) next = SystemClock.elapsedRealtimeNanos() // fell behind; resync
        }
        audio?.stop(); audio?.release()
        NativeCore.flushSave()
        NativeCore.shutdown()
    }
}
