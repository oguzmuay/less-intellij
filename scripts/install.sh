#!/bin/bash
# Builds both plugins and installs them (plus the LESS keymap) into the newest IntelliJ IDEA config.
# Usage: scripts/install.sh   (close IntelliJ first, then reopen it)
set -euo pipefail
cd "$(dirname "$0")/.."

IDEA_APP="${IDEA_HOME:-}"
for c in "$HOME/Applications/IntelliJ IDEA.app" "/Applications/IntelliJ IDEA.app"; do
  [ -z "$IDEA_APP" ] && [ -d "$c" ] && IDEA_APP="$c"
done
[ -d "$IDEA_APP" ] || { echo "IntelliJ IDEA not found. Set IDEA_HOME."; exit 1; }
export JAVA_HOME="$IDEA_APP/Contents/jbr/Contents/Home"

CONFIG=$(ls -d "$HOME"/Library/Application\ Support/JetBrains/IntelliJIdea* 2>/dev/null | sort | tail -1)
[ -d "$CONFIG" ] || { echo "No IntelliJ config dir found. Start the IDE once first."; exit 1; }
if pgrep -f "$IDEA_APP/Contents/MacOS/idea" >/dev/null; then
  echo "IntelliJ is running. Quit it first (it rewrites its config on exit)."; exit 1
fi

./gradlew buildPlugin --no-daemon -q
for dir in less-*-plugin; do
  z=$(ls -t "$dir"/build/distributions/*.zip | head -1)   # newest zip only
  echo "Installing $z"
  rm -rf "$CONFIG/plugins/$dir"                             # drop old jars of previous versions
  unzip -q -o "$z" -d "$CONFIG/plugins"
done

mkdir -p "$CONFIG/keymaps"
cp ide-config/LESS-keymap.xml "$CONFIG/keymaps/LESS.xml"
echo "Done. Start IntelliJ and pick the LESS keymap in Settings > Keymap."
