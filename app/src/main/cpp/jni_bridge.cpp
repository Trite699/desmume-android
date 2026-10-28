#include <jni.h>
#include <android/log.h>
#include "desmume_core.h"
#include "android_audio.h"

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "DeSmuME", __VA_ARGS__)
#define FN(name) Java_org_desmume_android_NativeCore_##name

struct Str {
	JNIEnv *e; jstring j; const char *c;
	Str(JNIEnv *env, jstring s) : e(env), j(s), c(env->GetStringUTFChars(s, nullptr)) {}
	~Str() { e->ReleaseStringUTFChars(j, c); }
};

extern "C" {

JNIEXPORT jint JNICALL FN(init)(JNIEnv *env, jclass, jstring dataDir) { Str s(env, dataDir); return ds_init(s.c); }
JNIEXPORT void JNICALL FN(shutdown)(JNIEnv *, jclass) { ds_shutdown(); }
JNIEXPORT jint JNICALL FN(loadRom)(JNIEnv *env, jclass, jstring path) { Str s(env, path); return ds_load_rom(s.c); }
JNIEXPORT void JNICALL FN(reset)(JNIEnv *, jclass) { ds_reset(); }
JNIEXPORT void JNICALL FN(runFrame)(JNIEnv *, jclass) { ds_run_frame(); }

// Fills an int[256*384] with raw R,G,B,A byte-order pixels, i.e. what Bitmap.copyPixelsFromBuffer expects for ARGB_8888.
JNIEXPORT void JNICALL FN(getFrame)(JNIEnv *env, jclass, jintArray out)
{
	jint *p = env->GetIntArrayElements(out, nullptr);
	ds_get_frame_abgr((uint32_t *)p);
	env->ReleaseIntArrayElements(out, p, 0);
}

JNIEXPORT void JNICALL FN(setInput)(JNIEnv *, jclass, jint keys, jboolean touching, jint x, jint y)
{ ds_set_input((uint32_t)keys, touching ? 1 : 0, x, y); }

// Returns number of stereo frames written into out (which holds 2 shorts per frame).
JNIEXPORT jint JNICALL FN(readAudio)(JNIEnv *env, jclass, jshortArray out, jint maxFrames)
{
	jshort *p = env->GetShortArrayElements(out, nullptr);
	int n = android_audio_read((short *)p, maxFrames);
	env->ReleaseShortArrayElements(out, p, 0);
	return n;
}

JNIEXPORT void JNICALL FN(setVolume)(JNIEnv *, jclass, jint v) { android_audio_set_volume(v); }
JNIEXPORT jint JNICALL FN(saveState)(JNIEnv *env, jclass, jstring path) { Str s(env, path); return ds_save_state(s.c); }
JNIEXPORT jint JNICALL FN(loadState)(JNIEnv *env, jclass, jstring path) { Str s(env, path); return ds_load_state(s.c); }
JNIEXPORT void JNICALL FN(flushSave)(JNIEnv *, jclass) { ds_flush_save(); }
JNIEXPORT void JNICALL FN(setLanguage)(JNIEnv *, jclass, jint l) { ds_set_language(l); }

}
