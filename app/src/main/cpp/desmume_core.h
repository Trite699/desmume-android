// Thin, JNI-free wrapper over the DeSmuME core so it can be tested on a host.
#pragma once
#include <stdint.h>

// Button bits (match the Kotlin side).
enum {
	DS_KEY_A = 1 << 0, DS_KEY_B = 1 << 1, DS_KEY_SELECT = 1 << 2, DS_KEY_START = 1 << 3,
	DS_KEY_RIGHT = 1 << 4, DS_KEY_LEFT = 1 << 5, DS_KEY_UP = 1 << 6, DS_KEY_DOWN = 1 << 7,
	DS_KEY_R = 1 << 8, DS_KEY_L = 1 << 9, DS_KEY_X = 1 << 10, DS_KEY_Y = 1 << 11,
};

#define DS_SCREEN_W 256
#define DS_SCREEN_H 192

int  ds_init(const char *dataDir);          // 0 = ok
void ds_shutdown();
int  ds_load_rom(const char *path);         // 0 = ok
void ds_reset();
void ds_run_frame();                        // emulate one frame (~1/60 s of DS time)
void ds_get_frame_abgr(uint32_t *dst);      // 256x384, top screen first; Android ARGB_8888 byte order
void ds_set_input(uint32_t keys, int touching, int tx, int ty);
int  ds_save_state(const char *path);       // 0 = ok
int  ds_load_state(const char *path);
void ds_set_language(int lang);
void ds_flush_save();                       // flush battery save (.dsv) to disk
