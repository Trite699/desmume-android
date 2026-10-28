// Minimal glib shim for Android. DeSmuME's path.cpp only uses three glib calls;
// this avoids pulling in the whole library. The config directory is supplied by
// the app via android_set_config_dir() before a ROM is loaded.
#pragma once
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <sys/stat.h>
#include <string>

extern std::string g_android_config_dir;

static inline const char *g_get_user_config_dir(void) { return g_android_config_dir.c_str(); }

static inline char *g_build_filename(const char *a, const char *b, ...)
{
	std::string s = std::string(a) + "/" + b;
	return strdup(s.c_str());
}

static inline int g_mkdir_with_parents(const char *path, int mode)
{
	std::string p(path);
	for (size_t i = 1; i < p.size(); i++) {
		if (p[i] == '/') {
			p[i] = 0;
			mkdir(p.c_str(), mode);
			p[i] = '/';
		}
	}
	return mkdir(p.c_str(), mode);
}

static inline void g_free(void *p) { free(p); }
