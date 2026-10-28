// Platform glue DeSmuME expects every frontend to provide.
#include <string>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include "pcap.h"
#include "types.h"
#include "SPU.h"
#include "render3D.h"
#include "rasterize.h"
#include "android_audio.h"

std::string g_android_config_dir;

// libretro-common's rthreads.c calls pthread_setname_np() unconditionally, but bionic only
// provides it from API 26. With minSdk 24 the symbol is missing at link time (the NDK links
// shared libs with --no-undefined), so supply a no-op. Thread names are cosmetic.
#if defined(__ANDROID__) && __ANDROID_API__ < 26
extern "C" int pthread_setname_np(pthread_t, const char *) { return 0; }
#endif

// Global run flag the core reads (declared in NDSSystem.h); frontends own the definition.
volatile bool execute = false;

// ---- pcap stubs (bridged wifi unavailable on Android) ----
extern "C" {
int pcap_findalldevs(pcap_if_t **, char *err) { strncpy(err, "pcap unavailable", PCAP_ERRBUF_SIZE); return -1; }
void pcap_freealldevs(pcap_if_t *) {}
struct pcap *pcap_open_live(const char *, int, int, int, char *err) { strncpy(err, "pcap unavailable", PCAP_ERRBUF_SIZE); return NULL; }
void pcap_close(struct pcap *) {}
int pcap_setnonblock(struct pcap *, int, char *err) { strncpy(err, "pcap unavailable", PCAP_ERRBUF_SIZE); return -1; }
int pcap_sendpacket(struct pcap *, const u_char *, int) { return -1; }
int pcap_dispatch(struct pcap *, int, pcap_handler, u_char *) { return -1; }
void pcap_breakloop(struct pcap *) {}
}

// ---- core lists required by the emulator core ----
SoundInterface_struct *SNDCoreList[] = {
	&SNDDummy,
	&SNDDummy,
	&SNDAndroid,
	NULL
};

GPU3DInterface *core3DList[] = {
	&gpu3DNull,
	&gpu3DRasterize,   // software rasterizer; GLES renderer is a future step
	NULL
};
