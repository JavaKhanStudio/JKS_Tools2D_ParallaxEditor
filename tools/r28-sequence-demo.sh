#!/usr/bin/env bash
# r28-sequence-demo.sh PAGE [OUT_DIR] — the demo game on a page the editor exported (ParallaxDemo --page), off Simon's
# screen: prints each SEQUENCE layer's cycle, to compare with the Cycle line of the editor's Textures tab, and writes
# OUT_DIR/page.png (at once) and page-5s.png (scrolled 5 s).
#
#   tools/driver-probe.sh < tools/r28-sequence-probe.txt
#   tools/r28-sequence-demo.sh editor/build/r28/out/proj/r28.jplax editor/build/r28/demo
#
# As tools/demo-shots.sh: cage's headless display on a nested Xwayland; ATELIER_NO_OFFSCREEN=1 runs it in a window.
set -u
cd "$(dirname "$0")/.." || exit 1
PAGE="$(realpath "${1:?a .jplax page}")" || exit 1
OUT="$(realpath -m "${2:-demo/build/r28-sequence}")"
mkdir -p "$OUT"
./gradlew -q :demo:installDist >/dev/null || exit 1
RUN="cd demo/assets && ../build/install/demo/bin/demo --page \"$PAGE\" --shots \"$OUT\" >\"$OUT/demo.log\" 2>&1"
if [ "${ATELIER_NO_OFFSCREEN:-0}" = 1 ]; then
	bash -c "$RUN"
else
	WLR_BACKENDS=headless ALSOFT_DRIVERS=null timeout 120 cage -- tools/nested-x.sh 1280x720 bash -c "$RUN" >"$OUT/cage.log" 2>&1
fi
grep "SEQUENCE" "$OUT/demo.log" || { echo "no cycle printed:"; tail -20 "$OUT/demo.log"; exit 1; }
