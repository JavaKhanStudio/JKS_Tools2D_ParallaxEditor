#!/usr/bin/env bash
# sibling-core-check.sh — proves ./gradlew :editor:run builds the library from a checkout beside this one (settings.gradle's
# includeBuild): a change to that checkout's core shows in the editor with nothing published.
#
#   tools/sibling-core-check.sh [LIBRARY_CHECKOUT]     (default ../JKS_Tools2D_ParallaxBackground)
#
# Clones this repository and the library side by side into a scratch directory (neither checkout is touched), adds a
# line to core that prints a marker when the editor loads GVars_Serialization, runs :editor:run for 90 s (off screen for
# an agent, gradle/offscreen.gradle) and looks for the marker in its output. Then the same with -PparallaxFromCentral,
# which must NOT print it. Exit 0 when both hold.
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd) || exit 2
LIB=$(cd "${1:-$ROOT/../JKS_Tools2D_ParallaxBackground}" && pwd) || exit 2
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
MARK="SIBLING-CORE-$$"

git clone -q --no-local "$LIB" "$WORK/JKS_Tools2D_ParallaxBackground" || exit 2
git clone -q --no-local "$ROOT" "$WORK/JKS_Tools2D_ParallaxEditor" || exit 2
SER="$WORK/JKS_Tools2D_ParallaxBackground/core/src-jvm/jks/tools2d/parallax/heart/GVars_Serialization.java"
sed -i "0,/^{/s//{\n\tstatic { System.out.println(\"$MARK\"); }/" "$SER"
grep -q "$MARK" "$SER" || { echo "could not patch $SER" >&2; exit 2; }

cd "$WORK/JKS_Tools2D_ParallaxEditor" || exit 2
# run [ARGS]: the editor for 90 s; sets RAN (1 when it was still running when stopped, not crashed) and MARKED.
run() {
	timeout 90 ./gradlew --no-daemon :editor:parallaxSource :editor:run "$@" >"$WORK/run.log" 2>&1
	RAN=$([ $? = 124 ] && echo 1 || echo 0)
	MARKED=$(grep -q "$MARK" "$WORK/run.log" && echo 1 || echo 0)
	grep -m1 '^parallax-background:' "$WORK/run.log"
	[ "$RAN" = 1 ] || { echo "the editor stopped before 90 s:"; tail -20 "$WORK/run.log"; }
}

status=0
run
if [ "$RAN$MARKED" = 11 ]; then echo "sibling:  the patched core's marker printed: OK"
else echo "sibling:  FAIL (ran $RAN, marker $MARKED)"; status=1; fi
run -PparallaxFromCentral
if [ "$RAN$MARKED" = 10 ]; then echo "central:  no marker, the editor took the jar from Maven Central: OK"
else echo "central:  FAIL (ran $RAN, marker $MARKED)"; status=1; fi
exit $status
