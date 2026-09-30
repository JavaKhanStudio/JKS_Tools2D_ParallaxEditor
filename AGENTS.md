# Agent guide

What README.md does not say: the rules that break silently here. Concepts, settings and file shapes are in README.md
and in the library's README; branches and releases in RELEASING.md.

## Modules

| Module    | What it is | Java | Run / check |
|-----------|------------|------|-------------|
| `editor/` | The desktop tool that builds pages and exports `.plax`. Sources `src/` and `mains/`. | 17 | `./gradlew :editor:run` (workingDir `editor/`, finds `editor/Files`) |
| `demo/`   | A small game using the library. | 17 | `./gradlew :demo:run` (workingDir `demo/assets`): the `demo/showcase` pages, SPACE variant, ENTER scene, N tint, LEFT/RIGHT scroll, R reset; `tools/demo-shots.sh` plays it off screen. `./gradlew :demo:lab` (workingDir the root): the grading lab, `demo/lab`. `./gradlew :demo:stress`: frame time of generated pages |

- The library (`core`, the file formats, the Godot and jME readers) is
  [JKS_Tools2D_ParallaxBackground](https://github.com/JavaKhanStudio/JKS_Tools2D_ParallaxBackground), a repository and a
  board of its own. The editor takes it as `io.github.javakhanstudio:parallax-background:$parallaxVersion`; with that
  repository checked out beside this one, `settings.gradle` builds its `core` from source instead
  (`./gradlew :editor:parallaxSource` says which, `-PparallaxFromCentral` forces Maven). A change to what a page holds
  starts there: the stored field and its format bump in the library, a snapshot, then the control here.
- Never open a window on Simon's screen. With `ATELIER_AGENT` set or `CLAUDECODE=1`, `:editor:run`, `:demo:run`,
  `:demo:lab` and `:demo:stress` render in a headless cage (`gradle/offscreen.gradle`, off with
  `ATELIER_NO_OFFSCREEN=1`, only when Simon asked to watch), and fail when there is no cage. A new JavaExec task calls
  `rootProject.offscreen(it)` or `rootProject.headless(it)`, or the build stops. Anything else that opens one goes
  through cage or `tools/offscreen.sh`; a nested X server inside cage comes from `tools/nested-x.sh`. Cage renders on
  the NVIDIA GPU, a desktop window on the Intel one: `:demo:stress` prints which, compare numbers of the same.
- Every dependency version, the library's (`parallaxVersion`) and the editor's `version` are in `gradle.properties`.
- Repositories go in `settings.gradle` only: `FAIL_ON_PROJECT_REPOS` fails the build on a module-level one.
- CI (`.github/workflows/ci.yml`, JDK 17, 21 and 25) runs `./gradlew build`, then `./gradlew :editor:distZip
  :demo:distZip`, against the library from Maven Central.
- `editor/Files` holds the 2019 sample `.plax` files, the library's proof that format 1 still loads (it keeps byte
  copies in `core/test-data/samples`). Never re-export or overwrite them.

## Editor

- State is static, in `editor/src/jks/tools2d/parallax/editor/gvars/GVars_*`, and there is one project at a time.
- There are no editor tests, and a compiling control is not a working one: the 2019 editor shipped buttons and
  dialogs with empty listeners. After wiring a control, run `./gradlew :editor:run` and use it
  (`tools/driver-probe.sh` drives it off screen).
- Panels read the window size when built; `vue/Vue_Edition` rebuilds them after a resize.
- `GVars_UI.init` sets VisUI's global `scaleFactor`; skin styles are shared, so copy a style before changing it.
- Every control has a `setName` (`driver/Names`): `--driver-port` and the presenter's scripts find controls by it. Name a
  new control, and treat a rename as breaking those scripts.
- README.md's "Using the editor", and the library README's layer-settings table, document every setting: rename or
  rescale one and fix both.

## Demo

- `ParallaxLab --shots` has a copy in the library, `shots/.../ParallaxShots`, which the reader frame checks run: change
  how a scene is shot, change it there too.

## Credits

- NOTICE and README.md's Credits exist because a 2019 commit deleted the credit to Rahul Verma's
  ParallaxBackground-libgdx, which Apache-2.0 requires. Never trim them.

## Words

French names are kept: `vue` = a screen of the editor, `transfert` = the cross-fade between two pages, `decal` = a
layer's starting offset in percent of the world.
