# Branches, versions and releases

## Branches

| Branch        | What it holds                                                                  |
|---------------|--------------------------------------------------------------------------------|
| `develop`     | Day-to-day work. Always on a `-SNAPSHOT` version.                               |
| `release/X.Y` | Stabilising a version: only fixes, no new features. Created from `develop`.     |
| `master`      | What has been released. Every release is tagged here (`vX.Y.Z`).                |

CI builds every push and pull request on JDK 17, 21 and 25: `./gradlew build`, then `:editor:distZip :demo:distZip`.

## Two versions

`gradle.properties` holds two:

- `version`: the editor's, the `X.Y.Z` of `ParallaxEditor-X.Y.Z.zip`. It went on from the library's 2.5.0 when the
  editor got this repository, and moves on its own from there.
- `parallaxVersion`: the library the editor is built with, `io.github.javakhanstudio:parallax-background` from Maven
  Central. The editor writes pages with that library's code, so it is also the page format the editor writes.

On `develop`, `parallaxVersion` is the library's `-SNAPSHOT` (settings.gradle looks it up in Central's snapshot
repository). A release is built on a released library: the Release workflow refuses a `-SNAPSHOT` `parallaxVersion`.
When the editor needs a library change, release the library first (its RELEASING.md), then set `parallaxVersion` to it
on the release branch.

## Releasing X.Y.Z

```bash
git checkout develop && git pull
git checkout -b release/2.5                      # stabilise, only fixes from here

# set the final version, and a released library
sed -i 's/^version=.*/version=2.5.0/' gradle.properties
sed -i 's/^parallaxVersion=.*/parallaxVersion=2.5.0/' gradle.properties
./gradlew -PparallaxFromCentral build :editor:distZip   # the released library, not a checkout beside this one
git commit -am "Prepare 2.5.0"
git push -u origin release/2.5                   # CI builds it

git checkout master && git merge --no-ff release/2.5
git tag -a v2.5.0 -m "2.5.0"
git push origin master --follow-tags             # the tag starts the Release workflow

# back to develop for the next version
git checkout develop
git merge --no-ff master
sed -i 's/^version=.*/version=2.6.0-SNAPSHOT/' gradle.properties
# and parallaxVersion back to the library's current -SNAPSHOT, if the editor follows it
git commit -am "Start 2.6.0"
git push
```

The Release workflow refuses to run when the tag and `gradle.properties` disagree. A tag with a suffix (`v2.5.0-rc1`) is
published as a GitHub pre-release.

Each release attaches `ParallaxEditor-X.Y.Z.zip` to the GitHub release: the editor, the library it was built with, and
the sample projects of `editor/Files/Demos`. Nothing is published to Maven Central from this repository.
