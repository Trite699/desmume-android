// Lock-protected ring buffer sound core. The SPU pushes samples in; the Kotlin
// audio thread pulls them out and writes to AudioTrack (whose blocking write
// provides frame pacing).
#include <mutex>
#include <string.h>
#include <vector>
#include "android_audio.h"

static std::mutex g_mtx;
static std::vector<short> g_ring;   // interleaved stereo
static size_t g_cap = 0;            // capacity in frames
static size_t g_rd = 0, g_wr = 0, g_fill = 0; // frames
static int g_volume = 100;

static int AInit(int buffersize)
{
	std::lock_guard<std::mutex> l(g_mtx);
	g_cap = DESMUME_SAMPLE_RATE / 4;           // 250 ms of headroom
	g_ring.assign(g_cap * 2, 0);
	g_rd = g_wr = g_fill = 0;
	return 0;
}
static void ADeInit() { std::lock_guard<std::mutex> l(g_mtx); g_ring.clear(); g_cap = g_rd = g_wr = g_fill = 0; }

static void AUpdate(s16 *buf, u32 n)
{
	std::lock_guard<std::mutex> l(g_mtx);
	for (u32 i = 0; i < n && g_fill < g_cap; i++) {
		g_ring[g_wr * 2]     = buf[i * 2];
		g_ring[g_wr * 2 + 1] = buf[i * 2 + 1];
		g_wr = (g_wr + 1) % g_cap;
		g_fill++;
	}
}
static u32 AGetSpace() { std::lock_guard<std::mutex> l(g_mtx); return (u32)(g_cap - g_fill); }
static void AMute() {}
static void AUnmute() {}
static void ASetVolume(int v) { g_volume = v; }
static void AClear() { std::lock_guard<std::mutex> l(g_mtx); g_rd = g_wr = g_fill = 0; }

SoundInterface_struct SNDAndroid = {
	SNDCORE_ANDROID, "Android Ring Buffer",
	AInit, ADeInit, AUpdate, AGetSpace, AMute, AUnmute, ASetVolume, AClear,
	NULL, NULL
};

int android_audio_read(short *dst, int maxFrames)
{
	std::lock_guard<std::mutex> l(g_mtx);
	int n = 0;
	while (n < maxFrames && g_fill > 0) {
		dst[n * 2]     = (short)((int)g_ring[g_rd * 2]     * g_volume / 100);
		dst[n * 2 + 1] = (short)((int)g_ring[g_rd * 2 + 1] * g_volume / 100);
		g_rd = (g_rd + 1) % g_cap;
		g_fill--;
		n++;
	}
	return n;
}
void android_audio_set_volume(int v) { g_volume = v < 0 ? 0 : (v > 100 ? 100 : v); }
