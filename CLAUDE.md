# less-intellij

Monorepo: LESS editör deneyimini IntelliJ'e taşıyan plugin'ler + ayarlar. İçinde üç plugin var:
- `less-ai-plugin/` (`dev.lessai.ai`): AI paneli + inbox (aşağıdaki plan).
- `less-middle-plugin/` (`dev.less.middle`): boş editör ekranını LESS logosu + keymap'ten okunan kısayol listesiyle değiştirir (`EditorEmptyTextPainter` servisini override eder; 2026.3'te servis `open=true`, ayrıca `editorEmptyTextProvider` / `editorEmptyStateComponentProvider` EP'leri var). Runtime'da görsel olarak doğrulanmadı.
- `less-monorepo-plugin/` (`dev.less.monorepo`): proje açılınca (`postStartupActivity`, arka planda `readAction`) content root'ları + proje klasörünü max 8 seviye tarar (gizli, `node_modules`, `build`, `target`, ... ve exclude'lar atlanır), marker dosyalarından (`MarkerFiles`) modül türünü bulur (`ModuleDetector`: Gradle/Maven/sbt + Spring Boot, package.json bağımlılıklarından Expo (`expo` dep veya app.json'da `"expo"`) / React Native / React/Next/Vue/Angular/Svelte/Nest, iOS (`*.xcodeproj`/`*.xcworkspace` klasörü veya Podfile; bundle klasörlerinin içine inilmez), Android (Gradle script'inde `com.android.`), Python, Rust, Go, .NET, Swift, Dart, Ruby, PHP, CMake; iç içe CMakeLists yok sayılır). `ProjectViewNodeDecorator` modül klasörünün ikonunu (IntelliJ'in mavi kareli modül ikonu yerine) türün renkli klasör ikonuyla değiştirir, gri etiket ekler (`Spring Boot · Gradle · Kotlin · workspace (4)`). Marker dosyası değişince `BulkFileListener` 500 ms debounce ile yeniden tarar; Tools > Rescan Monorepo Modules. İlk taramada >=2 modül varsa bir kez özet balonu (modül seti değişince tekrar). İkonlar `scripts/gen-monorepo-icons.py` ile üretilir (16x16 + `_dark`). Derleniyor, IDE'de görsel olarak doğrulanmadı.

Kurulum (yeni makine): `git clone` -> IntelliJ'i kapat -> `scripts/install.sh` (build + tüm plugin'ler + LESS keymap'i IDE config'ine kopyalar).

Aşağıdaki bölüm `less-ai-plugin` içindir. LESS editörünün AI panelini (Claude/Codex/Gemini/Ollama oturumları + ajanlar arası inbox) IntelliJ'e taşıyan Kotlin plugin'i. Resmi Claude Code plugin'i bilerek KULLANILMIYOR (kullanıcı kalitesinden memnun değil).

Tam plan: `docs/plan.md`. Kapsam kararı: **tam LESS eşdeğeri**, dil Kotlin.

## Durum
- Aşama 0-6 kodlandı ve derleniyor; **hiçbiri IDE'de çalışırken görülmedi**. Aşama 5: `inbox/` (MailboxService, InboxNotifier, MailMessage, MailboxListener) + `toolwindow/InboxPanel` (panelde Sessions ve Inbox sekmeleri). Aşama 6: `settings/` (LessAiSettings, LessAiConfigurable; Settings > Tools > LESS AI), layout'ta `AIAssistant` -> `AI`. Aşama 2: `session/` (AiProvider, SessionStatus, NewSessionRequest, CommandBuilder, AiSession, SessionManager, SessionListener), `toolwindow/` (AiToolWindowFactory, SessionTabsPanel: oturumlar `JBEditorTabs` ile editör tarzı sekme; sekmede durum noktası + ad + kapat butonu, orta tık kapatır, çift tık yeniden adlandırır; terminalde Cmd+W oturumu kapatır), `dialog/NewSessionDialog`, `actions/` (New/Rename/Close/Next/Previous), `plugin.xml` action kayıtları, keymap: Cmd+3 AI paneli, Cmd+T yeni oturum, Opt+Cmd+[ / ] oturum geçişi.
- Oturum kapanışı: komut `<cmd> && exit` olarak yazılır (CLI temiz çıkınca kabuk da çıkar; hata kodunda terminal açık kalır ki hata okunsun), `TerminalWidget.addTerminationCallback` kabuk bitince oturumu `SessionManager.close` ile kapatır. IDE'de doğrulanmadı.
- Bilinen belirsizlikler: `TerminalWidget.sendCommandToExecute` kabuk henüz başlamadıysa (deferStart=true) komutu bekletiyor mu.
- **Durum tespiti (Aşama 3+4, derleniyor, IDE'de doğrulanmadı)** — `status/` paketi, oturum başına `StatusCoordinator` (oturum disposable'ına bağlı; kapanınca polling durur, geçici dosyalar silinir). Geçici dosyalar: `PathManager.getSystemPath()/less-ai/{status,prompts}/<sessionId>.*`.
  - Claude (otoriter): `--settings <id>.settings.json` ile hook'lar (`SessionStart`/`Stop` -> idle, `UserPromptSubmit`/`PostToolUse` -> working, `Notification` -> stdin'de `permission_prompt`/"needs your permission" varsa approval, yoksa idle) `<id>.hook.sh` (POSIX sh, jq yok) üzerinden `<id>.status` dosyasına yazar; `StatusFileWatcher` 500 ms'de bir okur. İlk hook geldikten sonra sezgi yok sayılır.
  - Codex: `-c 'notify=["sh","-c","printf idle > <id>.status"]'` (tur bitince idle; kullanıcının kendi `notify` ayarını bu oturumda ezer) + sezgi.
  - Hepsi için sezgi (`OutputHeuristicStatusSource`): JediTerm `TerminalTextBuffer.addModelListener` ile kirli bayrağı + `TerminalWidget.getText()` son 15 satır; metin değişiyor -> WORKING, 2.5 sn sessiz -> IDLE, `(y/n)`, `do you want to proceed`, `allow this`, `approve` ... -> WAITING_APPROVAL.
  - Başlatma: `CommandBuilder.buildCommand(request, LaunchContext)`; `MailboxLaunch` env (`LESS_AI_NAME`, `LESS_AI_DIR`) + prompt dosyası (`--append-system-prompt "$(cat f)"`, Codex `-c "developer_instructions=$(cat f)"` TOML string) ekler. Komut POSIX kabuk sözdizimi varsayar (fish'te `$(...)` 3.4+ ister).
- Sıradaki: IDE'de elle doğrulama (aşağıdaki riskler), sonra düzeltmeler. Henüz yok: LESS'in `less` MCP sunucusunun karşılığı (ajanlara IDE araçları).

## Build
Makinede sistem JDK'sı ve Gradle yok. IntelliJ'in gömülü JBR'si (Java 25) kullanılır:
```
export JAVA_HOME="$HOME/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"
./gradlew buildPlugin                      # tüm plugin'ler, <modül>/build/distributions/*.zip
./gradlew :less-ai-plugin:runIde           # sandbox IDE (ai)
./gradlew :less-middle-plugin:runIde       # sandbox IDE (middle)
./gradlew :less-monorepo-plugin:runIde     # sandbox IDE (monorepo)
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
- `LESS-keymap.xml` -> `~/Library/Application Support/JetBrains/IntelliJIdea2026.3/keymaps/LESS.xml` (macOS keymap'inden türeyen; Cmd+1 Project, Cmd+2 FocusEditor (LESS'te Cmd+2 = code editor), Cmd+4 Terminal, Cmd+5 Database, Cmd+6 Run, Cmd+7 Git log, Cmd+0 Commit, Cmd+Shift+M Maven, Cmd+O GotoFile, Cmd+Shift+P GotoAction, Cmd+D SelectNextOccurrence, Cmd+Enter FindUsages, Cmd+Shift+W CloseProject, Cmd+3 AI paneli, Cmd+T `LessAi.NewSession` (`Vcs.UpdateProject` kaldırıldı)). Cmd+Shift+Left/Right: editörde Insert/Visual modda IDE satır başı/sonu seçimi; Normal modda IdeaVim ile sekme geçişi (`ide-config/ideavimrc`, `sethandler` + `<Action>(PreviousTab/NextTab)`); odak bir tool window'dayken (Cmd+4 Terminal, Cmd+3 AI = oturumlar) `LessAi.PreviousToolWindowTab/NextToolWindowTab` (`actions/ToolWindowTab*.kt`, odak editördeyken devre dışı kalır). Bunların hiçbiri IDE'de doğrulanmadı.
- `window.layouts.xml` (hedef layout: solda Project+Commit, altta Services+Version Control, sağda Terminal+AI+Database) ve `active-keymap.xml` (`LESS` keymap'ini aktif eder) `options/` altına IDE KAPALIYKEN kopyalanmalı; IDE açıkken kapanırken üzerine yazar. Layout JSON'daki `isVisible` anahtarı tahmin, doğrulanmadı. Layout'ta `AIAssistant` zaten `AI` ile değiştirildi.


## Eklenenler (sonradan)
- `less-middle-plugin`: alt çubukta `SystemMonitorWidget` (RAM/CPU/Disk/IP; `vm_stat`, MXBean; VPN tüneli + fiziksel IP varsa "Leaked IP" — bu yorum tahmin, LESS'in mantığı doğrulanmadı).
- Yeni oturum diyaloğu: `ClaudeProfiles.detect()` ~/.claude ve ~/.claude-* klasörlerini bulur; varsayılan profil (~/.claude) için `CLAUDE_CONFIG_DIR` verilmez; son seçilen `LessAiSettings.lastClaudeProfileDir` ile hatırlanır.
- Güncelleme: versiyon kökteki `VERSION` dosyasında; `.github/workflows/release.yml` yalnız `release-<versiyon>` tag'inde çalışır (tag `VERSION` ile eşleşmezse fail), zip'leri + `updatePlugins.xml`'i release'e koyar; IntelliJ'e `https://github.com/oguzmuay/less-intellij/releases/latest/download/updatePlugins.xml` bir kez eklenir. CI'daki IDE indirme yolu denenmedi.
- Cmd+P = `LessMiddle.TogglePreview` (`less-middle-plugin/preview/`; keymap'te `ParameterInfo`'nun Cmd+P'si kaldırıldı). Sıra: (A) seçili editör `TextEditorWithPreview` ise layout SHOW_EDITOR <-> SHOW_PREVIEW (Markdown, SVG = `ImageFileEditorProvider`'ın `SvgEditor`'ı, Mermaid); (B) sekmede TextEditor + metin dışı editör varsa `FileEditorManager.setSelectedEditor` ile geçiş; (C) .html/.htm için yerleşik aynı-sekme önizleme yok (`JcefHtmlEditorProvider`/`WebPreviewEditorProvider`/`HtmlPreviewFileEditorProvider` yalnız özel sanal dosyaları kabul ediyor), bu yüzden `HtmlPreviewEditorProvider` (`TextEditorWithPreviewProvider` + JCEF `loadHTML(text, file.url)`, 300 ms debounce) eklendi; JCEF ayrı bundled plugin (`com.intellij.modules.jcef`), optional depends + `less-middle-jcef.xml`.
- `TextFirstLayoutListener`: açılan her split editör text-only başlar (Markdown ayarındaki varsayılan layout'u ezer). IDE'de doğrulanmadı: state restore'un listener'dan sonra layout'u geri yazıp yazmadığı, `loadHTML` ile göreli asset'lerin (css/img) yüklenmesi, önizlemedeyken odak/`FILE_EDITOR` data context'i, HTML'de IdeaVim/vurgulamanın split içinde normal çalışması.
- Esc düzeltmesi (`session/TerminalEscapePassthrough`): klasik `JBTerminalPanel.handleKeyEvent` önce pre-key handler'ları, sonra `TerminalEscapeKeyListener`'ı çağırır; o da tool window id'si `"Terminal"` değilse düz Esc'te `activateEditorComponent()` yapıp Esc'i yutar (javap ile doğrulandı). Pre-handler düz Esc'i `terminalStarter.sendBytes(0x1b)` ile PTY'ye yazıp tüketir. IDE'de doğrulanmadı.
- Notes modalı (`less-middle-plugin/notes/`): `LessMiddle.OpenNotes`, ideavimrc'de Normal/Visual Cmd+N (Insert'te IDE'nin Generate'i). Not = sekme, app seviyesinde `less-notes.xml`. Modal içinde Cmd+Shift+Left/Right sekme, Cmd+T yeni, Cmd+W sil, çift tık / Cmd+R / Cmd+Shift+R yeniden adlandır, Esc/Cmd+N kapat. Notlar global (app seviyesi), proje başına değil. Her notun sağında checklist (`ChecklistPanel`, `Note.items`): Cmd+L metin <-> "Add item" alanı, Enter ekler, listede Space işaretler, Enter düzenler, Backspace siler, Opt+Up/Down taşır. IDE'de doğrulanmadı.
- Vim kopya kağıdı: boş editör ekranında IDE kısayollarının yanında Vim sütunu (`vim/VimShortcuts.EMPTY_SCREEN`; alan dar/kısaysa çizilmez), Normal modda `g?` = `LessMiddle.VimCheatSheet` popup'ı. Liste `ide-config/ideavimrc` ile elle senkron tutulmalı.
- javap: `~/Library/Java/JavaVirtualMachines/openjdk-25.0.2/Contents/Home/bin/javap` var.

## Notlar
- Cmd+2 çakışması: IntelliJ'in `air` plugin'i `Activateair.threadsToolWindow`'u (Agent Sessions) macOS keymap'inde Cmd+2'ye bağlıyor; `LESS-keymap.xml` bu action'ı ve `ActivateBookmarksToolWindow`'u kaldırıyor. `air` ayrıca Cmd+Enter'ı (`Air.Chat.Prompt.SendNow`) kullanıyor, FindUsages ile çakışabilir.
