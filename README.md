# less-intellij

Monorepo with IntelliJ plugins that bring the LESS editor experience to IntelliJ IDEA.

- `less-ai-plugin`: brings the LESS editor's AI panel to IntelliJ IDEA: Claude, Codex, Gemini and Ollama sessions in terminal tabs, per-session status, and an agent-to-agent inbox.

Status: early development. See the plan in the repo history for scope.

- `less-middle-plugin`: replaces the empty editor screen with a LESS logo and a shortcut cheat sheet read from the active keymap.

## Install

Close IntelliJ, then run `scripts/install.sh` (builds both plugins, installs them and the LESS keymap). Needs IntelliJ IDEA 2026.3 installed; its bundled JBR is used as the JDK.

## Updates via GitHub

IntelliJ cannot tell where a manually installed plugin came from, so add a custom plugin repository once:
`Settings > Plugins > ⚙ > Manage Plugin Repositories > +` and enter
`https://raw.githubusercontent.com/<owner>/<repo>/main/updatePlugins.xml` (the repo must be public).

To publish a new version: bump `version` in the plugin's `build.gradle.kts`, run
`GITHUB_REPO=<owner>/<repo> scripts/release.sh`, commit and push `updatePlugins.xml`, then run the printed `gh release create ...` command to upload the zips.
