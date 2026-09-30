#!/usr/bin/env bash
# start-demo.sh libgdx — opens the demo's window on the desktop: the board's "Start the demo" button (r102,
# .atelier/actions.toml). The Godot demo is the library's (JKS_Tools2D_ParallaxBackground, tools/start-demo.sh godot).
#   libgdx  demo/ (ParallaxDemo): the showcase pages, SPACE variant, ENTER next scene, N tint, LEFT/RIGHT scroll, R reset.
# The board's server runs as a service, without the desktop's display variables: this script defaults them to the
# logged-in session's (Wayland socket wayland-0, Xwayland :0 and mutter's cookie), so the window opens on the screen.
# It returns when the window is closed.
set -euo pipefail
cd "$(dirname "$0")/.."
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"
[ -S "$XDG_RUNTIME_DIR/wayland-0" ] && export WAYLAND_DISPLAY="${WAYLAND_DISPLAY:-wayland-0}"
export DISPLAY="${DISPLAY:-:0}"
if [ -z "${XAUTHORITY:-}" ]; then
	for f in "$XDG_RUNTIME_DIR"/.mutter-Xwaylandauth.*; do [ -f "$f" ] && export XAUTHORITY="$f"; done
fi

case "${1:-}" in
	libgdx)
		./gradlew -q :demo:installDist
		cd demo/assets
		exec ../build/install/demo/bin/demo ;;
	*)
		echo "usage: tools/start-demo.sh libgdx" >&2
		exit 2 ;;
esac
