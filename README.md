# DeSmuME for Android (experimental port)

An Android front end for the DeSmuME core in `../desmume/src`. The core is used unmodified;
Android-specific glue lives in `app/src/main/cpp/`.

**Status: untested on a device.** The native code compiles with g++ (x86_64) and with clang
for aarch64 (via zig as a proxy), and the core initialises on the host. No ROM has been run,
and a real NDK build has not been done.

## Build
1. Android Studio (Koala or newer), with NDK and CMake 3.22.1 from the SDK Manager.
2. Open this `android/` folder. Let it generate the Gradle wrapper, or run `gradle wrapper --gradle-version 8.7`.
3. `./gradlew assembleDebug`, install, pick a `.nds` file you own.

ABIs: arm64-v8a, armeabi-v7a, x86_64. Edit `abiFilters` in `app/build.gradle.kts` to change.

## Controls
On-screen pad and touch screen; hardware gamepads (Nintendo-style mapping: Android B = DS A);
keyboard X/Z/S/A, Q/W, Enter, Backspace. Back button opens the menu (reset, save/load state).
Battery saves (.dsv) are stored in app-private storage under `files/desmume/`.

## Design notes
- `desmume_core.{h,cpp}`: JNI-free wrapper (init / load / run frame / input / state).
- `android_audio.cpp`: ring-buffer sound core, drained by the emulation thread into `AudioTrack`.
- `android_platform.cpp`: symbols the core expects from a frontend (`SNDCoreList`, `core3DList`,
  `execute`) plus pcap stubs. `shim/` replaces `glib.h` and `pcap.h` so upstream sources stay untouched.
- Interpreter CPU and software 3D rasteriser only (no `HAVE_JIT`, no OpenGL ES).

## Known gaps
No JIT, no GLES renderer (slow on weak devices), Wi-Fi bridge and microphone stubbed,
master-brightness postprocess not applied, no settings UI, one save-state slot, audio/video
sync is approximate. Licence: MIT.

## Checks without an NDK
- `tools/host_build_check.sh`: builds core + glue with host g++ and links a smoke test
  (`host_test <rom.nds>` runs 600 frames if you give it a ROM).
- `tools/clang_syntax_check.sh <target> <HOST_64|HOST_32>`: compiles everything with zig's clang.
