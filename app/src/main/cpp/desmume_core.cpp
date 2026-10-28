#include "desmume_core.h"
#include <string.h>
#include <string>
#include "NDSSystem.h"
#include "GPU.h"
#include "SPU.h"
#include "driver.h"
#include "saves.h"
#include "slot2.h"
#include "MMU.h"
#include "mc.h"
#include "render3D.h"
#include "android_audio.h"

extern std::string g_android_config_dir;

static bool g_inited = false;
static bool g_romLoaded = false;

int ds_init(const char *dataDir)
{
	if (g_inited) return 0;
	g_android_config_dir = dataDir ? dataDir : ".";

	driver = new BaseDriver();
	if (NDS_Init() != 0) return -1;

	CommonSettings.use_jit = false;           // interpreter only for now (see README)
	CommonSettings.cheatsDisable = true;

	slot2_Init();
	slot2_Change(NDS_SLOT2_NONE);

	SPU_ChangeSoundCore(SNDCORE_ANDROID, 735 * 4);
	SPU_SetSynchMode(0, 0);
	SPU_SetVolume(100);

	if (!GPU->Change3DRendererByID(RENDERID_SOFTRASTERIZER)) return -2;

	g_inited = true;
	return 0;
}

void ds_shutdown()
{
	if (!g_inited) return;
	execute = false;
	NDS_DeInit();
	g_inited = false;
	g_romLoaded = false;
}

int ds_load_rom(const char *path)
{
	if (!g_inited) return -1;
	if (NDS_LoadROM(path) < 0) return -2;
	g_romLoaded = true;
	execute = true;
	return 0;
}

void ds_reset() { if (g_romLoaded) NDS_Reset(); }

void ds_run_frame()
{
	if (!g_romLoaded) return;
	NDS_beginProcessingInput();
	NDS_endProcessingInput();
	NDS_exec<false>();
	SPU_Emulate_user();
}

void ds_get_frame_abgr(uint32_t *dst)
{
	const NDSDisplayInfo &info = GPU->GetDisplayInfo();
	const uint16_t *src = info.masterNativeBuffer16; // BGR555: R=bits0-4 G=5-9 B=10-14
	const int n = DS_SCREEN_W * DS_SCREEN_H * 2;
	for (int i = 0; i < n; i++) {
		uint16_t p = src[i];
		uint32_t r = (p & 0x1F), g = (p >> 5) & 0x1F, b = (p >> 10) & 0x1F;
		r = (r << 3) | (r >> 2); g = (g << 3) | (g >> 2); b = (b << 3) | (b >> 2);
		dst[i] = 0xFF000000u | (b << 16) | (g << 8) | r;   // memory order R,G,B,A on little-endian
	}
}

void ds_set_input(uint32_t k, int touching, int tx, int ty)
{
	NDS_setPad(k & DS_KEY_RIGHT, k & DS_KEY_LEFT, k & DS_KEY_DOWN, k & DS_KEY_UP,
	           k & DS_KEY_SELECT, k & DS_KEY_START, k & DS_KEY_B, k & DS_KEY_A,
	           k & DS_KEY_Y, k & DS_KEY_X, k & DS_KEY_L, k & DS_KEY_R,
	           false, false);
	if (touching) {
		if (tx < 0) tx = 0; if (tx > 255) tx = 255;
		if (ty < 0) ty = 0; if (ty > 191) ty = 191;
		NDS_setTouchPos((u16)tx, (u16)ty);
	} else {
		NDS_releaseTouch();
	}
}

int ds_save_state(const char *path) { return savestate_save(path) ? 0 : -1; }
int ds_load_state(const char *path) { return savestate_load(path) ? 0 : -1; }
void ds_set_language(int lang) { CommonSettings.fwConfig.language = (u8)lang; }

void ds_flush_save() { if (g_romLoaded) MMU_new.backupDevice.flushBackup(); }
