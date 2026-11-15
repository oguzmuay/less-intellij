# LESS AI paneli için IntelliJ plugin'i

## Context
LESS editöründeki AI paneli (Claude / Codex / Gemini / Ollama oturumları, durum göstergeleri, sekme yönetimi) IntelliJ'de yok. Resmi Claude Code plugin'i istenmiyor. Sıfırdan Kotlin plugin'i yazılacak; kapsam "tam LESS eşdeğeri".

## Keşif bulguları (bu makinede)
- IntelliJ IDEA Ultimate 2026.3 (build 263.7307.200), `~/Applications/IntelliJ IDEA.app`, JBR/Java 25 içinde gömülü.
- **Sistem JDK'sı, Gradle ve Kotlin yok.** Build için IDE'nin JBR'si `JAVA_HOME` olarak kullanılacak, Gradle wrapper ile Gradle indirilecek (ağ gerekir).
- Terminal plugin'i bundled: `ShellTerminalWidget`, `TerminalToolWindowManager`, `TerminalTabState` (`plugins/terminal/lib/terminal.jar`). Yeni terminal motoru (`TerminalToolWindowTabsManager`) da var; API'ler 2026.3'te değişken, uyumluluk ilk spike'ta doğrulanacak.
- Proje konumu: `/Users/oguzugurdogan/Documents/projects/less-ai-intellij` (üst klasör bu makinede yok, oluşturulur). Orada `git init` (Aşama 0); `.gitignore` (build/, .gradle/, .idea/, .intellijPlatform/) ve ilk commit.
- LESS inbox mekanizması (binary'den çıkarıldı, `crates/less-ai/src/agents.rs`): her projede bir posta klasörü (`LESS_AI_DIR`) var: `README.md`, `inbox/<agent>.md` (Claude, Codex, Gemini, Ollama), `shared/`, `.gitignore`. Ajan kendi adını `$LESS_AI_NAME` env'inden alır. Gönderme = hedefin `inbox/<agent>.md` dosyasına `## from <ad> <zaman>` başlıklı bölüm eklemek; alma = kendi inbox dosyasını okumak (yeni mesajlar sonda). Dosya paylaşımı `shared/`. Ollama dosya okuyamaz, yalnız mesaj alır. Ajanlara sistem prompt'u olarak (Claude: `--append-system-prompt`, Codex: `developer_instructions`) bu kullanım ve "Your inbox is .../inbox/<ad>.md; sorulduğunda veya yeni posta bildirilince oku" talimatı verilir. LESS ayrıca editör araçlarını MCP sunucusu (`less`) olarak ajanlara açar; IntelliJ'de zaten yerleşik `idea` MCP sunucusu var, onu ajanlara bağlamak ileride ayrı iş.

## Yaklaşım
Kotlin + IntelliJ Platform Gradle Plugin 2.x, hedef IU 2026.3 (`local()` ile kurulu IDE'ye karşı derleme, 1 GB indirme yok), `bundledPlugin("org.jetbrains.plugins.terminal")`.

### Bileşenler
1. **Tool window `AI`** (sağ anchor): üstte oturum listesi (isim, sağlayıcı, durum rozeti), altta seçili oturumun terminal widget'ı.
2. **Oturum modeli** (`AiSession`, `AiProvider`, `SessionStatus`): STARTING / WORKING / IDLE / WAITING_APPROVAL.
3. **Yeni oturum diyaloğu**: sağlayıcı (Claude, Codex, Gemini, Ollama), sekme adı (boş = otomatik), Claude için hesap adı + config klasörü (`CLAUDE_CONFIG_DIR`), Ollama için model listesi (`ollama list`). Binary arama yolları: `/opt/homebrew/bin`, `/usr/local/bin`.
4. **Terminal entegrasyonu**: her oturum bir shell terminal widget'ında ilgili komutla başlar.
5. **Durum tespiti**
   - Claude: oturum başına geçici `--settings` dosyası ile `UserPromptSubmit`, `Stop`, `Notification` hook'ları; hook küçük bir script ile durum dosyasına yazar, plugin dosyayı izler.
   - Codex: `notify` config + çıktı sezgisi.
   - Gemini / Ollama: terminal çıktısı sezgisi (en kırılgan kısım; ilk sürümde "best effort").
6. **Eylemler**: rename, close, new session, next/previous session. Kısayollar mevcut `keymaps/LESS.xml` içine eklenir: `meta 3` (AI panelini aç), `meta T` (yeni oturum), `meta shift OPEN_BRACKET/CLOSE_BRACKET` zaten sekme geçişi için IDE varsayılanı, oturum geçişi için panel odaktayken aynı tuşlar.
7. **Layout**: `AIAssistant` yerine plugin'in tool window ID'si sağ panele (`Terminal`, `Database` ile) eklenir; staged `window.layouts.xml` güncellenir.

8. **Inbox (ajanlar arası posta)**: `MailboxService` proje başına posta klasörünü (`<proje>/.idea/less-ai/` veya `~/.local/share/less-ai-intellij/<proje>/`, varsayılan ikincisi, git'e girmesin) oluşturur, `README.md` ve `.gitignore` yazar. Oturum başlatılırken env: `LESS_AI_NAME`, `LESS_AI_DIR`; sistem prompt'u eklenir. Dosya izleyici (VFS listener) inbox dosyalarındaki değişimi yakalar: yeni mesaj gelince hedef oturumda rozet gösterir ve ajan idle ise terminale "you have new mail" satırı yazar. UI: panelde Inbox sekmesi (ajan başına okunmamış sayı, mesaj listesi, kullanıcıdan ajana mesaj gönderme kutusu, `shared/` klasörünü açma).

### Aşamalar
0. Repo: `Documents/projects/less-ai-intellij` oluştur, `git init`, `.gitignore`, ilk commit.
1. Spike: boş plugin, Gradle wrapper + JBR ile derleme, `runIde` / IDE'ye yükleme; tool window içinde bir terminal widget'ı açıp `claude` çalıştır (API doğrulaması).
2. Oturum listesi + yeni/rename/close + 4 sağlayıcı.
3. Claude hook tabanlı durum.
4. Codex/Gemini/Ollama durumu, hesap/config ve model seçimi.
5. Inbox: MailboxService, env + prompt enjeksiyonu, dosya izleme, Inbox sekmesi.
6. Keymap + layout entegrasyonu, ikon, ayarlar sayfası (binary yolları).

## Kritik dosyalar (yeni proje)
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- `src/main/resources/META-INF/plugin.xml`
- `src/main/kotlin/.../toolwindow/AiToolWindowFactory.kt`
- `.../session/{AiSession,AiProvider,SessionStatus,SessionManager}.kt`
- `.../terminal/AiTerminalLauncher.kt`
- `.../status/{ClaudeHookStatusSource,OutputHeuristicStatusSource}.kt`
- `.../actions/*.kt`
- Mevcut: `~/Library/Application Support/JetBrains/IntelliJIdea2026.3/keymaps/LESS.xml`

## Riskler
- Terminal API'leri deneysel/değişken; spike başarısız olursa JediTerm tabanlı kendi widget'ımıza düşme planı ayrıca konuşulur.
- Codex/Gemini durum tespiti güvenilir olmayabilir.
- Gradle/JBR kurulumu ağ ister; ilk build uzun sürer.

## Doğrulama
- `./gradlew buildPlugin` hatasız biter; `runIde` ile sandbox IDE açılır.
- Elle: yeni Claude oturumu başlar, prompt gönderilince durum WORKING → IDLE olur, onay istendiğinde WAITING_APPROVAL görünür.
- Çoklu oturum, rename, close, kısayollar çalışır; IDE yeniden başlatılınca layout'ta AI paneli sağda gelir.
