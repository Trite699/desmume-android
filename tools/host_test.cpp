// Host smoke test: init the core, try to load a ROM given on argv, run frames.
#include <stdio.h>
#include <stdlib.h>
#include <vector>
#include "desmume_core.h"
#include "android_audio.h"
int main(int argc, char **argv)
{
	if (ds_init("/tmp/ds_cfg") != 0) { puts("init failed"); return 1; }
	puts("init ok");
	if (argc < 2) { puts("no rom given; init-only test passed"); ds_shutdown(); return 0; }
	if (ds_load_rom(argv[1]) != 0) { puts("load failed"); return 2; }
	std::vector<uint32_t> fb(DS_SCREEN_W * DS_SCREEN_H * 2);
	std::vector<short> au(4096 * 2);
	long samples = 0;
	for (int i = 0; i < 600; i++) {
		ds_run_frame();
		samples += android_audio_read(au.data(), 4096);
	}
	ds_get_frame_abgr(fb.data());
	unsigned nz = 0; for (auto p : fb) if ((p & 0xFFFFFF) != 0) nz++;
	printf("ran 600 frames, audio frames=%ld, nonblack px=%u\n", samples, nz);
	ds_shutdown();
	return 0;
}
