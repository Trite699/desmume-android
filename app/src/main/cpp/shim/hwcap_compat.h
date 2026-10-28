// Force-included (see CMakeLists.txt) when compiling libretro-common's features_cpu.c.
// That file uses glibc's HWCAP_ARM_NEON / HWCAP_ARM_VFPv3 / HWCAP_ARM_VFPv4 names, but bionic and the
// kernel UAPI headers spell them HWCAP_NEON / HWCAP_VFPv3 / HWCAP_VFPv4 (and bionic's
// <sys/auxv.h> may not pull in <asm/hwcap.h> at all). Define whatever is missing; values are
// the stable Linux ARM ABI bits.
#pragma once
#include <sys/auxv.h>
#if defined(__linux__)
# ifndef HWCAP_ARM_NEON
#  define HWCAP_ARM_NEON  (1 << 12)
# endif
# ifndef HWCAP_ARM_VFPv3
#  define HWCAP_ARM_VFPv3 (1 << 13)
# endif
# ifndef HWCAP_ARM_VFPv4
#  define HWCAP_ARM_VFPv4 (1 << 16)
# endif
# ifndef HWCAP_ASIMD
#  define HWCAP_ASIMD     (1 << 1)
# endif
#endif
