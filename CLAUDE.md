# less-intellij

Monorepo: LESS editör deneyimini IntelliJ'e taşıyan plugin'ler + ayarlar. İçinde iki plugin var:
- `less-ai-plugin/` (`dev.lessai.ai`): AI paneli + inbox (aşağıdaki plan).
- `less-middle-plugin/` (`dev.less.middle`): boş editör ekranını LESS logosu + keymap'ten okunan kısayol listesiyle değiştirir (`EditorEmptyTextPainter` servisini override eder; 2026.3'te servis `open=true`, ayrıca `editorEmptyTextProvider` / `editorEmptyStateComponentProvider` EP'leri var). Runtime'da görsel olarak doğrulanmadı.

Kurulum (yeni makine): `git clone` -> IntelliJ'i kapat -> `scripts/install.sh` (build + iki plugin + LESS keymap'i IDE config'ine kopyalar).

Aşağıdaki bölüm `less-ai-plugin` içindir. LESS editörünün AI panelini (Claude/Codex/Gemini/Ollama oturumları + ajanlar arası inbox) IntelliJ'e taşıyan Kotlin plugin'i. Resmi Claude Code plugin'i bilerek KULLANILMIYOR (kullanıcı kalitesinden memnun değil).

Tam plan: `docs/plan.md`. Kapsam kararı: **tam LESS eşdeğeri**, dil Kotlin.

## Durum
- Aşama 0, 1, 2 kodlandı ve derleniyor; **hiçbiri IDE'de çalışırken görülmedi**. Aşama 2: `session/` (AiProvider, SessionStatus, NewSessionRequest, CommandBuilder, AiSession, SessionManager, SessionListener), `toolwindow/` (AiToolWindowFactory, SessionListPanel, SessionContentPanel, SessionCellRenderer), `dialog/NewSessionDialog`, `actions/` (New/Rename/Close/Next/Previous), `plugin.xml` action kayıtları, keymap: Cmd+3 AI paneli, Cmd+T yeni oturum, Opt+Cmd+[ / ] oturum geçişi.
- Bilinen belirsizlikler: `TerminalWidget.sendCommandToExecute` kabuk henüz başlamadıysa (deferStart=true) komutu bekletiyor mu; oturum durumu şimdilik sahte (STARTING -> 3 sn sonra IDLE).
- Sıradaki: Aşama 3 (Claude hook'larıyla gerçek durum), 4 (Codex/Gemini/Ollama durumu), 5 (inbox), 6 (layout'ta `AIAssistant` -> `AI`, ayarlar sayfası).

## Build
Makinede sistem JDK'sı ve Gradle yok. IntelliJ'in gömülü JBR'si (Java 25) kullanılır:
```
export JAVA_HOME="$HOME/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"
./gradlew buildPlugin                      # her iki plugin, <modül>/build/distributions/*.zip
./gradlew :less-ai-plugin:runIde           # sandbox IDE (ai)
./gradlew :less-middle-plugin:runIde       # sandbox IDE (middle)
```
- Hedef IDE: IntelliJ IDEA Ultimate 2026.3 (build 263.7307.200), `~/Applications/IntelliJ IDEA.app`; kök `build.gradle.kts` IDE yolunu bulur (`IDEA_HOME` env, sonra `~/Applications`, `/Applications`), alt projeler `local()` ile ona karşı derler. IDE Java 25 ister, toolchain 25.
- JBR'de `javap` yok; API keşfi için `unzip -p <jar> <class> | strings` kullanıldı.
- Spring Boot kuralları (global CLAUDE.md) bu projede geçerli DEĞİL (Kotlin IntelliJ plugin'i).

## Terminal API bulguları (2026.3)
- `plugins/terminal/lib/terminal.jar`: `TerminalToolWindowManager.createShellWidget(workingDir, tabName, requestFocus, deferStart)` yerleşik Terminal penceresine sekme ekler (bizim paneli değil).
- Kendi tool window'umuza gömmek için: `LocalTerminalDirectRunner.createTerminalRunner(project).startShellTerminalWidget(disposable, ShellStartupOptions.Builder().workingDirectory(..).build(), deferStart)` -> `widget.component` (derleniyor, runtime doğrulanmadı).
- Yeni terminal motoru: `TerminalToolWindowTabsManager` (intellij.terminal.frontend.jar), API değişken olabilir.

## LESS davranışı (referans: /Applications/LESS.app binary'sinden çıkarıldı)
- AI paneli: oturum listesi, durumlar `starting / working / idle (your turn) / waiting for approval/input`; Enter ile oturuma git; Cmd+T yeni oturum; Cmd+Shift+Left/Right sekme; sağlayıcı seçimi (Claude, Codex, Gemini, Ollama); Claude için hesap adı + config klasörü; Ollama için model; sekme adı (boş = otomatik); binary yolları `/opt/homebrew/bin`, `/usr/local/bin`.
- Ajanlara env: `LESS_AI_NAME`, `LESS_AI_DIR`. Sistem prompt: Claude `--append-system-prompt`, Codex `developer_instructions`; ayrıca `--mcp-config` / `mcp_servers.less` ile editör araçları MCP sunucusu `less`.
- **Inbox**: posta klasörü = `README.md`, `inbox/<agent>.md` (Claude, Codex, Gemini, Ollama), `shared/`, `.gitignore`. Gönderme: hedefin `inbox/<agent>.md` dosyasına `## from <ad> <zaman>` bölümü eklemek. Alma: kendi `inbox/<ad>.md` dosyasını okumak (yeni mesajlar sonda). Paylaşılan dosyalar `shared/`. Ollama dosya okuyamaz, yalnız mesaj alır. Ajana "Your inbox is <dir>/inbox/<ad>.md; sorulunca veya yeni posta bildirilince oku" denir.
- IntelliJ'de karşılığı yerleşik `idea` MCP sunucusu; ajanlara bağlamak kapsam dışı/ileride.

## Kullanıcının IntelliJ ayarları (`ide-config/`)
- `LESS-keymap.xml` -> `~/Library/Application Support/JetBrains/IntelliJIdea2026.3/keymaps/LESS.xml` (macOS keymap'inden türeyen; Cmd+1 Project, Cmd+2 Select Opened File, Cmd+4 Terminal, Cmd+5 Database, Cmd+6 Run, Cmd+7 Git log, Cmd+0 Commit, Cmd+Shift+M Maven, Cmd+O GotoFile, Cmd+Shift+P GotoAction, Cmd+D SelectNextOccurrence, Cmd+Enter FindUsages, Cmd+Shift+W CloseProject). Cmd+Shift+ok bağları KALDIRILDI (kullanıcı istemedi). Cmd+3 boş; plugin'in AI paneline bağlanacak.
- `window.layouts.xml` (hedef layout: solda Project+Commit, altta Services+Version Control, sağda Terminal+AIAssistant+Database) ve `active-keymap.xml` (`LESS` keymap'ini aktif eder) `options/` altına IDE KAPALIYKEN kopyalanmalı; IDE açıkken kapanırken üzerine yazar. Layout JSON'daki `isVisible` anahtarı tahmin, doğrulanmadı. Plugin tool window ID'si `AI` olacağı için layout'taki `AIAssistant` girdisi `AI` ile değiştirilmeli.


## Notlar
- Cmd+2 çakışması: IntelliJ'in `air` plugin'i `Activateair.threadsToolWindow`'u (Agent Sessions) macOS keymap'inde Cmd+2'ye bağlıyor; `LESS-keymap.xml` bu action'ı ve `ActivateBookmarksToolWindow`'u kaldırıyor. `air` ayrıca Cmd+Enter'ı (`Air.Chat.Prompt.SendNow`) kullanıyor, FindUsages ile çakışabilir.
