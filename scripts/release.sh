#!/bin/bash
# Builds both plugins and writes updatePlugins.xml pointing at GitHub release assets.
# Usage: GITHUB_REPO=owner/less-intellij scripts/release.sh
# Then: commit + push updatePlugins.xml, and upload the zips to the release (command printed at the end).
set -euo pipefail
cd "$(dirname "$0")/.."
: "${GITHUB_REPO:?Set GITHUB_REPO, e.g. GITHUB_REPO=owner/less-intellij}"

IDEA_APP="${IDEA_HOME:-$HOME/Applications/IntelliJ IDEA.app}"
[ -d "$IDEA_APP" ] || IDEA_APP="/Applications/IntelliJ IDEA.app"
export JAVA_HOME="$IDEA_APP/Contents/jbr/Contents/Home"
./gradlew buildPlugin --no-daemon -q

TAG="${TAG:-v$(date +%Y.%m.%d-%H%M)}"
BASE="https://github.com/$GITHUB_REPO/releases/download/$TAG"
OUT=updatePlugins.xml
echo '<?xml version="1.0" encoding="UTF-8"?>' > "$OUT"
echo '<plugins>' >> "$OUT"
for dir in less-*-plugin; do
  ver=$(sed -n 's/^version = "\(.*\)"/\1/p' "$dir/build.gradle.kts")
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
echo "  https://raw.githubusercontent.com/$GITHUB_REPO/main/updatePlugins.xml"
echo "Upload the zips:"
echo "  gh release create $TAG less-*-plugin/build/distributions/*.zip --repo $GITHUB_REPO"
