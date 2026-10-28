#pragma once
#include "SPU.h"
#define SNDCORE_ANDROID 2
extern SoundInterface_struct SNDAndroid;
// Called from the emulation thread's consumer side (JNI) to drain mixed samples.
// Returns number of stereo frames copied (1 frame = L+R s16).
int android_audio_read(short *dst, int maxFrames);
void android_audio_set_volume(int volume0to100);
