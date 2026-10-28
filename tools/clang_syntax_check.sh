#!/bin/bash
# If zlib.h is missing from the cross sysroot, point ZINC at a dir that has it (the NDK ships zlib).
# Clang front-end syntax check of the core + glue for ARM targets, using zig's bundled
# clang (pip install ziglang). Closer to what the NDK compiles than g++, but it uses
# glibc headers, not bionic: a proxy, not a substitute for a real NDK build.
ZINC="${ZINC:--I/tmp/zinc}"
TARGET="${1:-aarch64-linux-gnu}"; DEF="${2:-HOST_64}"
HERE="$(cd "$(dirname "$0")" && pwd)"; CPP="$HERE/../app/src/main/cpp"; SRC="$HERE/../../desmume/src"
FLAGS="-O0 -w -fno-strict-aliasing -D$DEF -I$CPP/shim -I$CPP -I$SRC -I$SRC/libretro-common/include -I$SRC/frontend $ZINC"
{ grep -o '\${DESMUME_SRC}/[^ ]*' "$CPP/CMakeLists.txt" | grep -E '\.(c|cpp)$' | sed "s|\${DESMUME_SRC}|$SRC|";
  ls $CPP/desmume_core.cpp $CPP/android_audio.cpp $CPP/android_platform.cpp; } > /tmp/clang_list.txt
chk() { o=/tmp/clang_obj_$(basename $1).o; case "$1" in *.c) python3 -m ziglang cc -target $T -std=gnu11 $F -c "$1" -o $o;; *) python3 -m ziglang c++ -target $T -std=gnu++14 $F -c "$1" -o $o;; esac >/tmp/clang_$(basename $1).log 2>&1 || echo "FAILED: $1"; rm -f $o; }
export -f chk; export T="$TARGET" F="$FLAGS"
xargs -a /tmp/clang_list.txt -P 1 -I{} bash -c 'chk {}'
echo "done ($TARGET)"
