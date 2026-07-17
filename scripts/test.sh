#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD="$ROOT/build/test-classes"

rm -rf "$BUILD"
mkdir -p "$BUILD"

mapfile -t SOURCES < <(find "$ROOT/stubs/src" "$ROOT/src/main/java" "$ROOT/test" -name '*.java' -print | sort)
javac --release 17 -d "$BUILD" "${SOURCES[@]}"
java -cp "$BUILD" TestHarness
