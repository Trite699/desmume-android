#!/bin/bash
# Compiles the DeSmuME core + Android glue with the host compiler (no NDK needed)
# to verify the source list, shims and symbols link. Not a runtime test on Android.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
CPP="$HERE/../app/src/main/cpp"
SRC="$HERE/../../desmume/src"
OUT="${OUT:-/tmp/ds_hostbuild}"
mkdir -p "$OUT"
FLAGS="-O1 -w -fno-strict-aliasing -DHOST_64 -I$CPP/shim -I$CPP -I$SRC -I$SRC/libretro-common/include -I$SRC/frontend"
grep -o '\${DESMUME_SRC}/[^ ]*' "$CPP/CMakeLists.txt" | grep -E '\.(c|cpp)$' | sed 's|\${DESMUME_SRC}/||' > "$OUT/list.txt"
compile() {
  f="$1"; o="$OUT/$(echo "$f" | tr '/' '_').o"
  case "$f" in
    *.c) gcc -std=gnu11 $FLAGS -c "$SRC/$f" -o "$o" ;;
    *)   g++ -std=gnu++14 $FLAGS -c "$SRC/$f" -o "$o" ;;
  esac || echo "FAILED: $f"
}
export -f compile; export FLAGS SRC OUT
xargs -a "$OUT/list.txt" -P "$(nproc)" -I{} bash -c 'compile {}'
for f in desmume_core android_audio android_platform; do
  g++ -std=gnu++14 $FLAGS -c "$CPP/$f.cpp" -o "$OUT/glue_$f.o" || echo "FAILED: $f"
done
g++ -std=gnu++14 $FLAGS -c "$HERE/host_test.cpp" -o "$OUT/host_test.o"
g++ "$OUT"/*.o -o "$OUT/host_test" -lz -lpthread
echo "LINK OK: $OUT/host_test"
