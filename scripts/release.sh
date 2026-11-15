#!/bin/bash
# Builds both plugins and writes updatePlugins.xml pointing at GitHub release assets.
# Usage: GITHUB_REPO=owner/less-intellij scripts/release.sh
# CI (.github/workflows/release.yml) runs this on every v* tag and uploads the results to the release.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${GITHUB_REPO:?Set GITHUB_REPO, e.g. GITHUB_REPO=owner/less-intellij}"

# Locally use the IDE's bundled JBR; on CI JAVA_HOME comes from setup-java.
IDEA_APP="${IDEA_HOME:-$HOME/Applications/IntelliJ IDEA.app}"
[ -d "$IDEA_APP" ] || IDEA_APP="/Applications/IntelliJ IDEA.app"
[ -d "$IDEA_APP" ] && export JAVA_HOME="$IDEA_APP/Contents/jbr/Contents/Home"

# VERSION (e.g. 0.2.0) overrides the version of every plugin; TAG defaults to v$VERSION.
VERSION="${VERSION:-}"
./gradlew buildPlugin --no-daemon -q ${VERSION:+-PpluginVersion=$VERSION}

TAG="${TAG:-v${VERSION:-$(date +%Y.%m.%d-%H%M)}}"
BASE="https://github.com/$GITHUB_REPO/releases/download/$TAG"
OUT=updatePlugins.xml
echo '<?xml version="1.0" encoding="UTF-8"?>' > "$OUT"
echo '<plugins>' >> "$OUT"
for dir in less-*-plugin; do
  ver="${VERSION:-$(sed -n 's/.*getOrElse("\(.*\)").*/\1/p' "$dir/build.gradle.kts")}"
  id=$(sed -n 's/.*id = "\(.*\)"/\1/p' "$dir/build.gradle.kts" | head -1)
  name=$(sed -n 's/.*name = "\(.*\)"/\1/p' "$dir/build.gradle.kts" | head -1)
  zip=$(basename "$(ls "$dir"/build/distributions/*.zip)")
  cat >> "$OUT" <<XML
  <plugin id="$id" url="$BASE/$zip" version="$ver">
    <idea-version since-build="263"/>
    <name>$name</name>
  </plugin>
XML
done
echo '</plugins>' >> "$OUT"

echo "Wrote $OUT. Plugin repository URL to add in IntelliJ:"
echo "  https://github.com/$GITHUB_REPO/releases/latest/download/updatePlugins.xml"
echo "Upload the zips:"
echo "  gh release create $TAG less-*-plugin/build/distributions/*.zip --repo $GITHUB_REPO"
