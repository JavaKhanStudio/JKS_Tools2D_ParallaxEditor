#!/usr/bin/env bash
# agent-screen-check.sh — proves an agent session here cannot put a window on Simon's screen (r54, after the library's
# r118).
#
# .claude/settings.json hands every Claude session in this checkout (the board's workers too) an empty DISPLAY and a
# WAYLAND_DISPLAY that names no socket. A window outside cage then fails to open instead of popping up, whatever
# launched it; cage (WLR_BACKENDS=headless) needs neither and renders as before. With that environment this script:
#   1. plays the demo through cage (tools/demo-shots.sh): must pass;
#   2. plays it on "the screen" (ATELIER_NO_OFFSCREEN=1): must FAIL, GLFW finding no display.
# Exit 0 when both hold. Needs cage, Xwayland, python3.
set -u
ROOT=$(cd "$(dirname "$0")/.." && pwd) || exit 2
cd "$ROOT" || exit 2
eval "$(python3 -c '
import json, shlex
env = json.load(open(".claude/settings.json"))["env"]
for k in ("DISPLAY", "WAYLAND_DISPLAY"):
    print("export %s=%s" % (k, shlex.quote(env[k])))
')" || { echo "FAIL: .claude/settings.json sets no DISPLAY/WAYLAND_DISPLAY"; exit 1; }
[ -z "$DISPLAY" ] || { echo "FAIL: .claude/settings.json leaves DISPLAY=$DISPLAY"; exit 1; }
[ ! -e "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/$WAYLAND_DISPLAY" ] || { echo "FAIL: WAYLAND_DISPLAY=$WAYLAND_DISPLAY is a live socket"; exit 1; }
OUT=$(mktemp -d); trap 'rm -rf "$OUT"' EXIT
status=0
if tools/demo-shots.sh "$OUT/cage" >/dev/null 2>&1; then
	echo "ok: cage renders without the screen"
else
	echo "FAIL: cage could not render with the screen stripped, see a rerun of tools/demo-shots.sh"; status=1
fi
if ATELIER_NO_OFFSCREEN=1 timeout 120 tools/demo-shots.sh "$OUT/screen" >/dev/null 2>&1; then
	echo "FAIL: a window opened outside cage"; status=1
elif grep -q "Failed to open display" "$OUT/screen/demo.log" 2>/dev/null; then
	echo "ok: a launch outside cage fails (GLFW: no display)"
else
	echo "FAIL: the launch outside cage failed for another reason, see $OUT/screen/demo.log"; status=1; trap - EXIT
fi
exit $status
