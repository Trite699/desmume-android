cmake_minimum_required(VERSION 3.22)
project(desmume_android C CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_C_STANDARD 11)

# Repo layout: <repo>/desmume/src is the emulator core, <repo>/android is this project.
# Override with -DDESMUME_SRC=... or the DESMUME_SRC environment variable (used by CI).
get_filename_component(REPO_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../.." ABSOLUTE)
if(NOT DESMUME_SRC AND DEFINED ENV{DESMUME_SRC})
  set(DESMUME_SRC "$ENV{DESMUME_SRC}")
endif()
if(NOT DESMUME_SRC)
  set(DESMUME_SRC "${REPO_ROOT}/desmume/src")
endif()
if(NOT EXISTS "${DESMUME_SRC}/NDSSystem.cpp")
  message(FATAL_ERROR "DeSmuME core sources not found at ${DESMUME_SRC}. "
                      "Set DESMUME_SRC to the upstream desmume/src directory.")
endif()

if(CMAKE_SIZEOF_VOID_P EQUAL 8)
  add_compile_definitions(HOST_64)
else()
  add_compile_definitions(HOST_32)
endif()

# Interpreter only: HAVE_JIT intentionally NOT defined (no JIT on Android yet).
# No SIMD defines either: matches upstream's meson build, uses portable C++ paths.
add_compile_options(-O3 -fno-strict-aliasing -fexceptions -frtti
                    -Wno-narrowing -Wno-deprecated-declarations -Wno-unused-value)

set(CORE_SOURCES
    ${DESMUME_SRC}/armcpu.cpp
    ${DESMUME_SRC}/arm_instructions.cpp
    ${DESMUME_SRC}/bios.cpp
    ${DESMUME_SRC}/cp15.cpp
    ${DESMUME_SRC}/commandline.cpp
    ${DESMUME_SRC}/common.cpp
    ${DESMUME_SRC}/debug.cpp
    ${DESMUME_SRC}/driver.cpp
    ${DESMUME_SRC}/Database.cpp
    ${DESMUME_SRC}/emufile.cpp
    ${DESMUME_SRC}/encrypt.cpp
    ${DESMUME_SRC}/FIFO.cpp
    ${DESMUME_SRC}/firmware.cpp
    ${DESMUME_SRC}/GPU.cpp
    ${DESMUME_SRC}/mc.cpp
    ${DESMUME_SRC}/path.cpp
    ${DESMUME_SRC}/readwrite.cpp
    ${DESMUME_SRC}/wifi.cpp
    ${DESMUME_SRC}/MMU.cpp
    ${DESMUME_SRC}/NDSSystem.cpp
    ${DESMUME_SRC}/ROMReader.cpp
    ${DESMUME_SRC}/render3D.cpp
    ${DESMUME_SRC}/rtc.cpp
    ${DESMUME_SRC}/saves.cpp
    ${DESMUME_SRC}/slot1.cpp
    ${DESMUME_SRC}/slot2.cpp
    ${DESMUME_SRC}/SPU.cpp
    ${DESMUME_SRC}/matrix.cpp
    ${DESMUME_SRC}/gfx3d.cpp
    ${DESMUME_SRC}/thumb_instructions.cpp
    ${DESMUME_SRC}/movie.cpp
    ${DESMUME_SRC}/frontend/modules/Disassembler.cpp
    ${DESMUME_SRC}/utils/advanscene.cpp
    ${DESMUME_SRC}/utils/datetime.cpp
    ${DESMUME_SRC}/utils/guid.cpp
    ${DESMUME_SRC}/utils/emufat.cpp
    ${DESMUME_SRC}/utils/fsnitro.cpp
    ${DESMUME_SRC}/utils/xstring.cpp
    ${DESMUME_SRC}/utils/decrypt/crc.cpp
    ${DESMUME_SRC}/utils/decrypt/decrypt.cpp
    ${DESMUME_SRC}/utils/decrypt/header.cpp
    ${DESMUME_SRC}/utils/task.cpp
    ${DESMUME_SRC}/utils/vfat.cpp
    ${DESMUME_SRC}/utils/dlditool.cpp
    ${DESMUME_SRC}/utils/libfat/cache.cpp
    ${DESMUME_SRC}/utils/libfat/directory.cpp
    ${DESMUME_SRC}/utils/libfat/disc.cpp
    ${DESMUME_SRC}/utils/libfat/fatdir.cpp
    ${DESMUME_SRC}/utils/libfat/fatfile.cpp
    ${DESMUME_SRC}/utils/libfat/filetime.cpp
    ${DESMUME_SRC}/utils/libfat/file_allocation_table.cpp
    ${DESMUME_SRC}/utils/libfat/libfat.cpp
    ${DESMUME_SRC}/utils/libfat/libfat_public_api.cpp
    ${DESMUME_SRC}/utils/libfat/lock.cpp
    ${DESMUME_SRC}/utils/libfat/partition.cpp
    ${DESMUME_SRC}/utils/tinyxml/tinystr.cpp
    ${DESMUME_SRC}/utils/tinyxml/tinyxml.cpp
    ${DESMUME_SRC}/utils/tinyxml/tinyxmlerror.cpp
    ${DESMUME_SRC}/utils/tinyxml/tinyxmlparser.cpp
    ${DESMUME_SRC}/utils/colorspacehandler/colorspacehandler.cpp
    ${DESMUME_SRC}/addons/slot2_auto.cpp
    ${DESMUME_SRC}/addons/slot2_mpcf.cpp
    ${DESMUME_SRC}/addons/slot2_paddle.cpp
    ${DESMUME_SRC}/addons/slot2_gbagame.cpp
    ${DESMUME_SRC}/addons/slot2_none.cpp
    ${DESMUME_SRC}/addons/slot2_rumblepak.cpp
    ${DESMUME_SRC}/addons/slot2_guitarGrip.cpp
    ${DESMUME_SRC}/addons/slot2_hcv1000.cpp
    ${DESMUME_SRC}/addons/slot2_expMemory.cpp
    ${DESMUME_SRC}/addons/slot2_piano.cpp
    ${DESMUME_SRC}/addons/slot2_passme.cpp
    ${DESMUME_SRC}/addons/slot1_none.cpp
    ${DESMUME_SRC}/addons/slot1_r4.cpp
    ${DESMUME_SRC}/addons/slot1_retail_nand.cpp
    ${DESMUME_SRC}/addons/slot1_retail_auto.cpp
    ${DESMUME_SRC}/addons/slot1_retail_mcrom.cpp
    ${DESMUME_SRC}/addons/slot1_retail_mcrom_debug.cpp
    ${DESMUME_SRC}/addons/slot1comp_mc.cpp
    ${DESMUME_SRC}/addons/slot1comp_rom.cpp
    ${DESMUME_SRC}/addons/slot1comp_protocol.cpp
    ${DESMUME_SRC}/mic.cpp          # no-microphone fallback (same as upstream meson default)
    ${DESMUME_SRC}/cheatSystem.cpp
    ${DESMUME_SRC}/texcache.cpp
    ${DESMUME_SRC}/rasterize.cpp
    ${DESMUME_SRC}/metaspu/metaspu.cpp
    ${DESMUME_SRC}/filter/2xsai.cpp
    ${DESMUME_SRC}/filter/bilinear.cpp
    ${DESMUME_SRC}/filter/deposterize.cpp
    ${DESMUME_SRC}/filter/epx.cpp
    ${DESMUME_SRC}/filter/hq2x.cpp
    ${DESMUME_SRC}/filter/hq3x.cpp
    ${DESMUME_SRC}/filter/hq4x.cpp
    ${DESMUME_SRC}/filter/lq2x.cpp
    ${DESMUME_SRC}/filter/scanline.cpp
    ${DESMUME_SRC}/filter/videofilter.cpp
    ${DESMUME_SRC}/filter/xbrz.cpp
    ${DESMUME_SRC}/version.cpp
    ${DESMUME_SRC}/libretro-common/compat/compat_getopt.c
    ${DESMUME_SRC}/libretro-common/file/file_path.c
    ${DESMUME_SRC}/libretro-common/compat/compat_strl.c
    ${DESMUME_SRC}/libretro-common/features/features_cpu.c
    ${DESMUME_SRC}/libretro-common/file/retro_dirent.c
    ${DESMUME_SRC}/libretro-common/file/retro_stat.c
    ${DESMUME_SRC}/libretro-common/rthreads/async_job.c
    ${DESMUME_SRC}/libretro-common/rthreads/rsemaphore.c
    ${DESMUME_SRC}/libretro-common/rthreads/rthreads.c
    ${DESMUME_SRC}/libretro-common/encodings/encoding_utf.c
)

# libretro-common's features_cpu.c uses glibc's HWCAP_ARM_* names; bionic spells them differently.
# Force-include a compat header for that single file (see shim/hwcap_compat.h).
set_source_files_properties(
    ${DESMUME_SRC}/libretro-common/features/features_cpu.c
    PROPERTIES COMPILE_OPTIONS "-include;${CMAKE_CURRENT_SOURCE_DIR}/shim/hwcap_compat.h")

# C files in the list need to be compiled as C; CMake handles that by extension.
add_library(desmume_core STATIC ${CORE_SOURCES})
target_include_directories(desmume_core PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/shim      # glib.h / pcap.h shims (must come first)
    ${DESMUME_SRC}
    ${DESMUME_SRC}/libretro-common/include
    ${DESMUME_SRC}/frontend)

add_library(desmume_jni SHARED
    desmume_core.cpp
    android_audio.cpp
    android_platform.cpp
    jni_bridge.cpp)
target_link_libraries(desmume_jni desmume_core z log android)
