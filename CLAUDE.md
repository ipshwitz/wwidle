# CLAUDE.md — Wyrm & Whelp: Idle Hoard

Living reference for building this game with Claude. Update this file whenever a
decision changes — it is the source of truth for architecture and conventions,
not a historical log (that's [CHANGELOG.md](CHANGELOG.md)).

## Project overview

- **Name:** Wyrm & Whelp: Idle Hoard
- **Genre:** Idle/incremental clicker (Adventure Capitalist–style), Android, Kotlin
- **Package:** `com.wyrmwhelp.idlehoard`
- **Theme:** Dungeons & Dragons–flavored. All naming (classes, screens, UI copy,
  game entities) uses D&D-appropriate terms — no leftover terminology from other
  projects (e.g. a prior food/kitchen-themed idle game). If a name doesn't fit a
  fantasy hoard/dragon theme, rename it before it lands in the codebase.

## Assets

- **`/assets`** (repo root) holds source game assets — logos, background art,
  and other raw design files. This is separate from Android's own
  `app/src/main/assets` runtime-assets folder (for files bundled into the APK
  and read via `AssetManager`). Files land in `/assets` before being copied
  into `app/src/main/res/drawable-nodpi/` (as-is, no density upscaling — for
  single full-bleed art rather than density-bucketed icon sets) to actually
  use from Compose. In use so far: `main-bg.png` → `drawable-nodpi/main_bg.png`,
  `GameScreen`'s background (behind a 50%-opacity white overlay so the art
  doesn't fight with the lair list for attention); `closed-chest.png` /
  `open-chest.png` → `drawable-nodpi/closed_chest.png` / `open_chest.png`,
  `FloatingMenu`'s FAB art for its collapsed/expanded states — both have real
  transparent backgrounds now (open-chest.png's initial export didn't; it was
  re-exported and re-copied); `menu-help_social.png` / `menu-unlocks.png` /
  `menu-upgrades.png` / `menu-stewards.png` / `menu-level_up.png` →
  `drawable-nodpi/menu_*.png`, the wooden-sign art for those `FloatingMenu`
  items (each image already has its label baked in — no separate text overlay
  needed). All five were re-exported once already: the first versions had a
  transparent margin baked into the canvas around the actual sign shape
  (1672x941), which silently threw off `SIGN_ASPECT_RATIO` and every size
  derived from it — the sign rendered smaller than its bounding box implied,
  which is why menu items looked oddly far apart even at a 4dp
  `spacedBy` and why a same-height `CloseButton` looked mismatched next to
  the header. Recropped tight to the sign art itself (1626x536, no padding) —
  `SIGN_ASPECT_RATIO` must always match whatever the current export's real
  dimensions are, not be left as a stale copy-pasted value. `menu-shop.png` /
  `menu-settings.png` → `drawable-nodpi/menu_shop.png` /
  `drawable-nodpi/menu_settings.png`, the same wooden-sign treatment for the
  two remaining `FloatingMenu` items that had been using the plain labeled
  `Surface` fallback — both already came in pre-cropped to the correct
  1626x536, no re-export needed this time. Every `FloatingMenu` item now has
  real sign art; the plain-`Surface` fallback in `MenuItemPlank` (and
  `SectionOverlayCard`'s plain-title fallback) stay in place for whatever
  section gets added next before its own art exists. `woodenwall-1.png`
  → `drawable-nodpi/woodenwall_1.png`, a tavern-
  interior background used by `SectionOverlayCard` (via `AppBackground`,
  which now takes an `imageRes` param instead of always using `main_bg`) —
  no transparency needed for this one, it's an opaque full-bleed backdrop
  like `main_bg.png`, not an icon. `x.png` → `drawable-nodpi/x.png`, crossed
  swords in a wooden ring with a drop shadow — real transparent background
  (verified via corner pixel alpha), used by the shared `CloseButton`
  composable in place of `Icons.Default.Close` for any "close this
  overlay/dialog" affordance. **Before copying any new *icon* art (things
  meant to sit on top of other content) into `drawable-nodpi/`, verify it
  actually has a transparent background** (check corner pixel alpha — `file`
  reporting "RGBA" only means an alpha channel exists, not that it's used;
  `open-chest.png`'s first export was RGBA but fully opaque). Full-bleed
  backdrops like `main_bg.png`/`woodenwall_1.png` are the exception — they're
  meant to be opaque. `coin.png` → `drawable-nodpi/coin.png`, an ornate gold
  coin (rope-braid rim, griffin-head emblem) — real transparent background,
  used by `GameHeader` next to the gold total in place of the `closed_chest`
  placeholder it launched with. `lair-kobold.png` / `lair-rat.png` /
  `lair-bugbear.png` → `drawable-nodpi/lair_kobold_warren.png` /
  `lair_giant_rat_burrow.png` / `lair_bugbear_warcamp.png` (named by lair id,
  not monster name, since a couple of tiers already share a monster
  initial), the first real `CreatureAvatar` portraits (v0.20.2) — square
  (1254x1254), genuinely transparent corners, no re-export needed.
  `lair-goblin.png` / `lair-orc.png` / `lair-gnoll.png` →
  `drawable-nodpi/lair_goblin_camp.png` / `lair_orc_encampment.png` /
  `lair_gnoll_den.png` (v0.31.1) — regenerated versions of three of the
  candidates called out below as held back for not matching the
  established style; the regenerated art matches the
  bold-silhouette/cel-shaded look of the kobold/rat/bugbear portraits, so
  these three were wired in as-is, same square (1254x1254), genuinely
  transparent corners, no re-export needed. `lair-hobgoblin.png` /
  `lair-ogre.png` / `lair-owlbear.png` → `drawable-nodpi/lair_hobgoblin_barracks.png` /
  `lair_ogres_cave.png` / `lair_owlbear_roost.png` (v0.42.1) — a
  regenerated `lair-hobgoblin.png` (the original export called out below
  as style-mismatched was replaced with one matching the established
  bold-silhouette/cel-shaded look) plus two brand-new portraits, all
  three verified square (1254x1254) with genuinely transparent corners,
  no re-export needed. `lair-troll.png` / `lair-wyvern.png` /
  `lair-young-dragon.png` → `drawable-nodpi/lair_troll_warren.png` /
  `lair_wyvern_aerie.png` / `lair_young_dragons_lair.png` (v0.43.1) —
  three more, same square (1254x1254), genuinely transparent corners, no
  re-export needed; `lair-young-dragon.png` renders a blue dragon despite
  the catalog's flavor text calling this tier a "Young Red Dragon" (and
  likewise for the still-unwired Adult/Ancient tiers) — used as supplied
  rather than second-guessed, same as every other art asset in this
  file. See the Open Questions entry on creature portrait art for what's
  still outstanding. `tv.png` → `drawable-nodpi/tv.png`
  (v0.31.2), a hand-illustrated wooden "scrying TV" (gold filigree, a
  wizard scene on-screen) — real transparent background, square
  (754x754) — used by the redesigned `WelcomeBackDialog` to front its
  rewarded-ad prompt instead of a plain button; see that bullet under
  Tech stack. `media-play.png` → `drawable-nodpi/media_play.png`
  (v0.32.0, art refreshed in v0.33.1 — square went from 754x754 to
  1144x1144, still genuinely transparent, no code changes needed since
  `QuickSpeedBoostAdButton` sizes it in dp, not raw pixels), a carved
  wooden circular play button (silver triangle, blue gem inlay, studded
  rim) — replaces `QuickSpeedBoostAdButton`'s earlier hand-drawn Canvas
  medallion+triangle; see that bullet under Tech stack.
  `new-notification.png` → `drawable-nodpi/new_notification.png`
  (v0.32.0), an ornate gold star — real transparent background, square
  (754x754) — the "something new to look at" badge on `FloatingMenu`'s
  chest toggle and menu planks; see that bullet under Tech stack.
  `gems.png` → `drawable-nodpi/gems.png` (v0.36.0), a leather pouch
  spilling colorful gems — real transparent background, square
  (754x754) — used on the Level Up screen next to the Gems balance and
  the new "Currently earning" counter, and by `LevelUpRewardDialog` in
  place of the generic `open_chest` art the other reward dialogs share
  (this reward literally *is* Gems); see the `LevelUpContent` bullet
  under Tech stack.
  `loading-video.mp4` (v0.37.0) is the one asset so far that doesn't go
  through `drawable-nodpi/` — it's a *video*, not a still image, so it
  goes into `app/src/main/res/raw/loading_video.mp4` instead (Android's
  resource folder for raw files played back via a resource URI; raw
  resource names are restricted to lowercase + underscores, hence the
  rename from the source file's hyphenated name). An older dragon and a
  baby dragon counting Gold and Gems, 416x752 (~9:16, matching a phone
  screen), 6 seconds, 24fps, has an audio track (128kbps) that's
  deliberately muted on playback — see the `LoadingScreen` bullet under
  Tech stack.
  **`open-chest.png` is also the app icon (v0.37.1)**, replacing Android
  Studio's default robot/green-grid template
  (`app/src/main/res/mipmap-*dpi/ic_launcher*`,
  `mipmap-anydpi/ic_launcher.xml`/`ic_launcher_round.xml`). Since there's
  no Android Studio Image Asset Studio available in this environment, the
  full icon set was generated programmatically (PowerShell +
  `System.Drawing`, script not checked in — one-off) straight from the
  1254x1254 source: an **adaptive icon** (`mipmap-*dpi/ic_launcher_foreground.png`,
  transparent background, the chest scaled to ~67% of the canvas so its
  full silhouette — including the coin spill and open lid — clears the
  ~66dp/108dp safe-zone circle every launcher mask uses) over a flat
  `@color/ic_launcher_background` (`#3E2417`, `FantasyPalette.woodDark` —
  see `values/colors.xml`), plus **legacy square/round PNGs**
  (`mipmap-*dpi/ic_launcher.png`/`ic_launcher_round.png`, chest baked
  onto an opaque wood-dark square/circle backdrop instead of relying on
  adaptive masking, for older/other launchers that don't support it) at
  the standard mdpi(48)→xxxhdpi(192) sizes, and a bonus
  `assets/app-icon-512.png` for a future Play Store listing (not wired
  into the app itself). `monochrome` in both adaptive-icon XMLs points at
  the same full-color foreground bitmap rather than a separate
  hand-drawn silhouette — Android 13+ themed-icon mode only ever reads
  its *alpha channel* as a mask and paints over with the system's own
  tint color, so a second monochrome-specific asset would render
  identically to reusing this one. Verified live on-device: the home
  screen, launcher, and task switcher all show the chest correctly
  circle-masked by Pixel Launcher, with no clipping of the lid/coins —
  the faint lighter ring some launchers draw around a dark adaptive-icon
  background (visible here since the backdrop is dark; essentially
  invisible on a white one, like Play Store's own icon) is the
  launcher's own ambient-shadow chrome applied to every adaptive icon,
  not an artifact of this asset.
  `avatar-<f|m>-<class>.png` (26 files, one per gender/D&D-class
  combination — v0.40.0) → `drawable-nodpi/avatar_<f|m>_<class>.png`, the
  avatar-picker portraits (`AvatarPickerDialog`/`GameHeader`'s
  `MedallionEmblem` — see the Tech stack bullet). Square (1024x1024),
  **opaque RGB with a plain white background, no alpha channel at all** —
  unlike every other icon asset in this file, these are deliberately used
  as-is rather than re-exported for transparency: they're cropped to a
  circle in code (`ContentScale.Crop` + `clip(CircleShape)`, same pattern
  as `LairRow`'s creature portraits) small enough that the square's white
  corners fall outside the visible circle, so the opaque background never
  actually shows.
- **`/SQL`** (repo root) holds every SQL script that needs to be run against
  the Supabase project, sequentially numbered (`001_create_cloud_saves_table.sql`,
  `002_...`) in the order they should be applied. Each is a one-time script run
  manually in the Supabase Dashboard's SQL Editor (no migration tooling wired
  up) — see the file's own header comment for exactly what to run and any
  dashboard-only settings (e.g. toggles) it doesn't cover.

## Workflow rules

These apply to every change made in this repo, however small:

1. **Bump the version every change**, using `A.B.C` (Major.Minor.Patch) semantic
   versioning in [app/build.gradle.kts](app/build.gradle.kts) (`versionName`, and
   increment `versionCode` by 1 each bump):
   - **Patch (A.B.C → A.B.C+1):** bug fixes, tuning, small tweaks, refactors.
   - **Minor (A.B.C → A.(B+1).0):** new features/systems added, backward-compatible.
   - **Major ((A+1).0.0):** breaking save-data changes, ground-up reworks, or the
     jump from pre-release (0.x.x) to first stable release (1.0.0).
   - Current version: **0.45.0** (Settings gained "Reset Account" and
     "Delete Account" — see the `DangerZoneCard` bullet under Tech stack
     and [CHANGELOG.md](CHANGELOG.md)).
2. **Log every change in [CHANGELOG.md](CHANGELOG.md)**, newest entry on top, in
   plain simplified language (what changed, not a diff dump), with a date and
   time in US Eastern (EST/EDT) for each entry.

## Build environment notes

- **Windows JDK loopback-socket bug (JDK-8305163):** running `gradlew` from a
  shell whose `TEMP`/`TMPDIR` resolves to a long path (as happens under this
  Claude Code session's scratchpad) makes the JVM fail with
  `java.io.IOException: Unable to establish loopback connection` before any
  build task runs — it's a Unix-domain-socket path-length limit, unrelated to
  the project. Fix: point Java at a short temp dir for the invocation, e.g.
  `TMPDIR="D:/gtmp" TEMP="D:\gtmp" TMP="D:\gtmp" JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=D:/gtmp" ./gradlew.bat <task>`.
- **compileSdk 36 / AGP 8.13.2 dependency ceiling:** don't bump
  `androidx.core`/`androidx.core-ktx` past 1.17.x or `androidx.lifecycle-*`
  past 2.9.x without also bumping AGP — newer versions require compileSdk 37,
  which needs AGP 9.1.0+. This bit us again with `androidx.navigation:navigation-compose`
  — 2.10.0 pulls in `lifecycle-*:2.11.0` transitively and hits the same
  ceiling; 2.9.4 (paired with lifecycle 2.9.3, matching our pin) works fine.
  Same rule applies to any future AndroidX dependency: check what `lifecycle-*`
  version it pulls in before bumping.
- **Emulator screencap/screenrecord returned a blank frame on this host for
  several sessions**, then started working again unexplained (same Pixel_8
  AVD, `-no-window`, `swiftshader_indirect`) — likely a transient host/driver
  compositor issue rather than anything fixable in-project. Don't assume
  either state: try a real `screencap` first, and if it comes back
  byte-identical to a previous blank capture (or a suspiciously round file
  size), fall back to `adb shell uiautomator dump` (pull with
  `adb pull //sdcard/window_dump.xml <path>`, double leading slash to dodge
  Git Bash path mangling) to verify UI text/state instead.
- **`adb shell input tap` can queue up and deliver taps long after the
  issuing command returns**, especially after a rapid loop of many taps
  (e.g. a bash `for` loop grinding gold). A tap fired from a stale queue
  lands on whatever the UI happens to show *at delivery time*, not what was
  on screen when the tap was issued — this produced several apparently
  "impossible" state jumps during testing (gold/ownership changing more
  than a single tap should cause, taps seemingly hitting elements that
  weren't at those coordinates) that all turned out to be delayed delivery,
  not app bugs. After any large tap loop, `sleep` generously (5–10s+) and
  re-check state before trusting it or issuing more taps; when a result
  looks inexplicable mid-test, the fastest way to tell real bug from queue
  lag is a fully isolated repro — fresh `pm clear` + relaunch, one single
  deliberate tap, one screenshot.
- **Cold start got much slower (~5s → ~15s on this emulator) after adding
  the Google Mobile Ads SDK** (`AdManager`'s `init` block runs
  `MobileAds.initialize()` + starts loading a rewarded ad the moment Hilt
  first constructs it, which happens as soon as `GameViewModel` is
  created). This is expected, known SDK behavior — native library loading
  plus a WebView engine spin-up for ad rendering (watch for a
  `SandboxedProcessService` process starting in Logcat, that's it) — not a
  regression to chase down. Real devices and warm ART caches on later
  launches are faster than a cold emulator boot; don't budget test waits
  based on pre-ads timings anymore. When testing anything that needs the
  app fully up (e.g. pulling its Room database), wait 15s+ after launch,
  not the ~5s that was enough before.
- **Google Play Billing (v0.27.0) does the same thing, worse, if
  initialized eagerly — so it isn't.** Starting `BillingClient.startConnection()`
  unconditionally at app launch (the same pattern `AdManager` uses for
  ads) measured at 25-75s+ cold start on this emulator, on top of the
  ads SDK's own ~15s — Play Store's internal handshake for an
  unpublished/unlisted app appears to retry substantially (visible in
  Logcat as a burst of `Finsky`/`AppInfoManager-Perf`/`ItemStore`
  activity) before giving up. `BillingManager.connect()` is deliberately
  **not** called from `init` — see that class's doc and
  `GameViewModel.ensureBillingConnected()` — it's only triggered when the
  Shop section actually opens, which brought cold start back down to the
  pre-Billing ~15-25s baseline. If billing-related cold-start slowness
  ever reappears, check first whether something reintroduced an eager
  `connect()` call before assuming it's environmental.

## Tech stack & architecture

**Pattern:** MVVM + Clean Architecture, DI via Hilt.

- **Presentation (UI):** Jetpack Compose screens/composables. ViewModels expose
  `StateFlow` to the UI. `@HiltViewModel` throughout.
  - **`LoadingScreen`** (`ui/common/LoadingScreen.kt`, v0.37.0) — the very
    first thing rendered, gating everything else. `GameViewModel.isLoading:
    StateFlow<Boolean>` starts `true` and flips `false` right after the
    init sequence below (local load, sign-in, cloud merge, offline
    earnings, `gameEngine.start()`) settles — `MainActivity`'s
    `WyrmWhelpApp` reads it first thing and, while `true`, renders only
    `LoadingScreen` and returns, before touching `openSection`/`gameState`/
    any other collected flow. `LoadingScreen` itself plays
    `loading_video.mp4` (see Assets) full-screen and looping via a plain
    `android.widget.VideoView` wrapped in `AndroidView` — deliberately not
    Media3/ExoPlayer, since a single looping 6-second clip doesn't
    justify a new dependency when `VideoView` already handles
    playback/looping/aspect-fit on its own. Muted via
    `MediaPlayer.setVolume(0f, 0f)` in `setOnPreparedListener` regardless
    of the source having an audio track — a loading screen playing
    unexpected sound would be jarring, and nothing else in the app has
    music/SFX yet to make an exception for. A plain "Loading your
    hoard…" `Text` sits near the bottom over the video.
    **This cannot mask the ads-SDK cold-start delay** (`AdManager`'s
    `MobileAds.initialize()`, ~15s+ on a cold/un-warmed emulator — see
    the Build environment notes) — that init runs synchronously inside
    Hilt's construction of `GameViewModel` itself (`AdManager` is one of
    its constructor params), which happens when `MainActivity.onCreate`
    first accesses the `by viewModels()` delegate, *before* `setContent`
    produces a single composable frame. Until that resolves, Android's
    own automatic app-icon splash screen is still the only thing on
    screen — confirmed via a screen recording of a cold start pulled
    from the emulator (`adb shell screenrecord`) and sent to the user
    directly for review, since screenshot-polling at 1s granularity
    couldn't reliably land inside the actual (often sub-2-second, but
    highly variable run-to-run) `LoadingScreen` window on a warm
    emulator. Not a bug in this feature — just the ceiling on what an
    in-app loading screen can cover; shortening the ads-SDK delay itself
    (e.g. injecting `AdManager` lazily via `dagger.Lazy<AdManager>` so
    constructing `GameViewModel` doesn't block on it) is a separate,
    larger change nobody's asked for yet.
    **Minimum 5-second floor (v0.42.2)** — the init sequence above (local
    load, sign-in, cloud merge, offline earnings) can settle in well
    under a second on a warm relaunch, which barely showed the loading
    video at all before this. `GameViewModel.init` now stamps
    `loadStartedAtMs` (`System.currentTimeMillis()`) the moment its
    coroutine starts, and right before `_isLoading.value = false`, `delay`s
    whatever's left of `MIN_LOADING_SCREEN_DURATION_MS` (5,000ms) if the
    real work finished sooner — purely cosmetic, doesn't change what the
    load sequence itself does or how long a genuinely slow cold start
    (ads SDK init, etc. — already well past 5s on its own) takes; the
    floor only ever adds a wait, never shortens one. Verified live via
    temporary `Log.d` timestamps bracketing the delay (screenshots and
    `uiautomator dump` both proved too slow/unreliable on this emulator
    to catch a sub-5-second window — see the Build environment notes'
    screencap/uiautomator gotcha) — both a cold start (3,354ms real load)
    and a forced-restart "warm" start (3,348ms — `force-stop` kills the
    whole process, so `AdManager`'s init cost still applies each time)
    correctly padded out to just over 5,000ms (5,004ms/5,072ms) before
    `isLoading` flipped false, confirmed via `adb logcat` timestamps, then
    the temporary logging was removed.
  - `GameViewModel` — implemented (`ui/game/GameViewModel.kt`): wraps
    `GameEngine`, starts its tick loop and settles offline earnings once on
    creation, exposes claim/hire-Steward/start-load actions plus a `buyQuantity:
    StateFlow<BuyQuantity>` (see `BuyQuantity` bullet below for how it drives
    `claimLair` — the selection itself isn't persisted, resetting to `X1`
    each launch). `GameScreen`/`LairCard`/
    `WelcomeBackDialog`/`GameHeader` (`ui/game/`) are the first real screen,
    wired up in `MainActivity` via `by viewModels()`.
  - **`GameHeader`** (`ui/game/GameHeader.kt`) — the game screen's top bar,
    replacing a plain `CenterAlignedTopAppBar` that only showed "X gp".
    Custom `Row`, not a Material `TopAppBar` — needs its own
    `Modifier.statusBarsPadding()` since it lost the insets handling
    `TopAppBar` provided for free (missing this let the header render
    underneath the status bar icons the first time). Takes one
    `GameHeaderState` data class (bundles `goldPieces`/`goldPerSecond`/
    `platinumPieces`/`buyQuantity`) plus an `onCycleBuyQuantity` callback —
    no `GameViewModel` reference inside the composable; `GameScreen` collects
    the flows and assembles the state object. Styled to match the app's
    cozy-fantasy chrome (wooden signs, parchment, carved edges) instead of
    flat Material colors, entirely via Compose drawing — gradients,
    `CutCornerShape`, `TextStyle.shadow`, and `Canvas`/`drawBehind` only
    where a built-in shape/modifier couldn't do it (the medallion's ring and
    shield, the banner's grain lines and carved bottom edge) — no new image
    assets, consistent with the stated "Canvas for animation, no sprite
    pack" art style. Colors live in a separate `FantasyPalette` data class
    (`ui/common/FantasyPalette.kt`; `colors` param, defaults to
    `FantasyPalette.Default`) so the whole look can be retinted without
    touching drawing code; it's a standalone palette, not wired into
    `ui/theme/Color.kt`/`Theme.kt` (those are still the untouched default M3
    template) — promoted out of `GameHeader` (it started as that screen's
    private `GameHeaderColors`) once `LairCard` needed the same look too.
    Three sections left-to-right:
    - `MedallionEmblem` — gold sweep-gradient ring + embossed wood disc +
      engraved shield-`Path` silhouette, standing in for the not-yet-built
      avatar system ("a handful of pre-created avatar images they can choose
      from").
    - A `Column`: the total-gold `GlowingGoldText` (`ui/common/GlowingGoldText.kt`
      — two stacked `Text`s — a dark offset copy for an engraved look, a
      bright gold copy with a wide colored shadow standing in for a glow,
      since `TextStyle.shadow` only takes one shadow — promoted out of
      `GameHeader` once `WelcomeBackDialog` needed the same look) next to
      `coin.png` (a small ornate gold coin, swapped in for the initial
      `closed_chest` placeholder once real coin art existed) for a touch of
      flavor, then a `ParchmentStrip` (cream gradient box) holding
      gold-per-second and Platinum Pieces (labeled "pp" — "Premium Coins" in
      the user's own description, but kept the existing 5E-flavored
      `platinumPieces` name rather than introduce a second label for the
      same currency). `FontFamily.Serif` approximates "fantasy-style"
      lettering — there's no bundled display font to use instead yet.
    - `WoodenButton` (`ui/common/WoodenButton.kt`) — the buy-quantity
      selector, restyled with `CutCornerShape` (matching the angled corners
      on `FloatingMenu`'s wooden signs) and a wood gradient instead of a
      plain white `Surface`. Shared with `LairCard`'s Claim button (see
      below) rather than kept private to the header, once the card redesign
      needed the same carved-wood look — takes `modifier`/`contentPadding`
      separately (mirroring Material's own `Button`) so the header can force
      a fixed size (`Modifier.size(...)`, `contentPadding = PaddingValues(0.dp)`,
      so cycling "x1"↔"x100" doesn't reflow the row) while `LairCard` just
      takes the default padding and lets the button size to its text.
      `enabled = false` fades the wood/border/text itself since it isn't a
      Material component and gets no disabled treatment for free.
    A hand-drawn dragon/wyrm silhouette in the banner background (mentioned
    as optional in the request) was skipped — no dragon art asset exists,
    and a few `Path` calls wouldn't read as one convincingly the way the
    shield silhouette does for a simple heraldic shape.
    Gold-per-second is computed inline in `GameScreen` (`sum of
    incomePerCycle(count) / effectiveProductionSeconds` across owned lairs)
    but only counts Steward-managed lairs — an unmanaged lair no longer runs
    continuously on its own (see `LairRow`/`GameEngine` below), so including
    it here would overstate what the player is actually earning while not
    tapping.
  - **Avatar selection (v0.40.0)** — `domain/model/Avatar.kt` defines
    `AvatarOption(id, gender, className)` and `AVATAR_CATALOG`, built by
    crossing every D&D 5E class with real avatar art
    (`AVATAR_CLASSES` × `AvatarGender.MALE`/`FEMALE`) against 26 source
    portraits (see the Assets section) — 13 classes × 2 genders. `id`
    (e.g. `"f_wizard"`) is what's actually persisted
    (`GameState.selectedAvatarId: String?`, null = the original
    engraved-shield placeholder) — a domain model deliberately free of any
    `R.drawable` reference; `ui/common/AvatarArt.kt`'s `avatarDrawableRes`
    is the one `when` mapping an id to its real drawable, shared between
    `GameHeader` and `AvatarPickerDialog` (same "domain has no Android
    resources" split as `LairRow`'s private `lairPortraitRes`, just
    promoted out of `private` since two files need it here).
    `GameEngine.selectAvatar(avatarId)` validates against
    `isValidAvatarId` before writing (silently no-ops on a bad id rather
    than throwing — a stale/garbled id should never brick the header)
    and is the only place `selectedAvatarId` changes.
    **`MedallionEmblem` is now tappable** — `GameHeader` gained an
    `onAvatarClick` callback, wired in `GameScreen` to a local
    `showAvatarPicker` boolean that opens `AvatarPickerDialog`. The
    medallion itself switched from a bare `Canvas` to a `Box` (`Canvas`
    for the gold ring plus, conditionally, either the original
    wood-disc/shield-`Path` placeholder or the real portrait `Image` —
    `ContentScale.Crop` + `clip(CircleShape)`, sized to `fillMaxSize(0.76f)`
    to land on the exact same coverage the wood disc used, so the ring
    frames a real portrait exactly like it framed the shield) —
    `clip(CircleShape).clickable(...)` on the outer `Box` for the tap
    target, same pattern `LairRow`'s `CreatureAvatar` already uses.
    `AvatarPickerDialog.kt` is a plain `Dialog` (same parchment-scroll
    chrome as `WelcomeBackDialog`) holding one `LazyVerticalGrid`
    (`GridCells.Fixed(4)`) mixing full-width header/row items
    (`item(span = { GridItemSpan(maxLineSpan) })`) with per-avatar tiles:
    a "Default (no avatar)" row first, then a "Female" label and its 13
    tiles, then "Male" and its 13 — each tile a circular crop ringed gold
    when it's the current selection. Tapping any tile (including
    Default, `onSelect(null)`) both applies the choice and closes the
    dialog immediately — no separate confirm step, matching how every
    other one-tap choice in this app behaves. **Persists like a currency,
    not like run progress** — `GameStateEntity`/`GameStateDto` each
    gained one nullable `selectedAvatarId` column/field (Room bumped to
    **database version 15**; no JSON encoding needed for a single
    nullable string, unlike the list-shaped fields nearby), and
    `GameEngine.performLevelUp()` explicitly carries `selectedAvatarId`
    into the fresh post-reset `GameState` alongside Platinum Pieces and
    the permanent boost tiers — it's a player identity choice, not
    something a Level Up should ever clear. Verified live on-device:
    selecting the female Fighter portrait immediately swapped the
    header's medallion art and closed the dialog; force-stopping and
    relaunching the app showed the same Fighter portrait still in place
    (confirming real Room persistence, not just in-memory state);
    reopening the picker showed the Fighter tile correctly ringed as the
    active selection; tapping "Default (no avatar)" correctly reverted
    the header back to the original engraved-shield placeholder; the
    grid scrolls cleanly through both the Female and Male groups.
    **Sized up (v0.40.1), per immediate explicit follow-up** ("i want the
    avatar to be a little bigger... make the entire header a little
    larger and make the border around the avatar smaller"):
    `MedallionEmblem`'s box grew 52.dp → 66.dp, the gold ring's stroke
    shrank (inset 0.11 → 0.06 of radius, stroke width 0.22 → 0.14) so
    more of the medallion is portrait/shield rather than frame, and the
    portrait/wood-disc fill grew to match (0.76 → 0.86 of the box) —
    the shield placeholder's own relative radius grew too (0.42 → 0.46)
    to fill the freed-up space proportionally. `GameHeader`'s outer `Row`
    padding went from `vertical = 8.dp` to `10.dp` so the taller
    medallion still sits comfortably inside the wooden banner. Verified
    live on-device: the medallion is visibly larger with a noticeably
    thinner gold ring and a bigger, easier-to-see portrait, and the
    header banner grew to match with no clipping.
  - **`WelcomeBackDialog`** (`ui/game/WelcomeBackDialog.kt`) — the offline-
    earnings pop-up, restyled from a plain Material `AlertDialog` to match
    the cozy-fantasy chrome: a plain `Dialog` (not `AlertDialog` — none of
    its title/text/button slots would let this look like anything other than
    a Material dialog) with `usePlatformDefaultWidth = false`, containing a
    parchment-gradient `Column` with a carved wood border and a
    `WoodenButton` ("Claim") instead of a Material `TextButton`.
    **Split top/bottom (v0.31.2), per explicit request** ("redesign...
    so the bottom half has the new tv graphic"): the top half is the
    earnings recap (`open_chest` art, the title, `GlowingGoldText` for the
    amount earned, the away-time line) unchanged from before; a thin
    carved `HorizontalDivider` separates it from the bottom half, which is
    the rewarded-ad prompt fronted by `tv.png` (a hand-illustrated
    "scrying TV," real transparent background — see the Assets section) in
    place of reusing the chest icon a second time. While `isDoubled` is
    false, a short flavor line ("Tune in to double your haul!") plus the
    "Watch Ad to Double" `WoodenButton` sit under the TV; once doubled, the
    TV art stays in place (removing it would make the bottom half flicker
    empty right as the reward lands) but the flavor line/button are
    replaced by a bold "Broadcast complete — earnings doubled!"
    confirmation line. `adUnavailableMessage` still surfaces under the
    button exactly as before. Verified live on-device (offline-earnings
    timestamp backdated 30 minutes via a direct Room DB edit, since there's
    no way to actually leave the app running in the background for real
    during testing): watching the ad doubled the displayed total (742.46M
    → 1.48B gp) and correctly swapped the bottom half from the button to
    the confirmation line, with the same amount landing in the real
    balance after Claim.
  - **`BuyQuantity`** (`ui/game/BuyQuantity.kt`) — the `X1`/`X10`/`X100`/
    `NEXT`/`MAX` enum cycled by tapping `GameHeader`'s small selector box
    (`.next()` wraps around). Fully wired up: `BuyQuantity.resolve(lair,
    unitsOwned, availableGp)` is the single source of truth for both what
    `LairCard` previews (its Claim button's quantity/cost) and what
    `GameViewModel.claimLair` actually purchases, so the two can't drift
    apart. `X10`/`X100` are fixed; `NEXT` targets this lair's next
    [MILESTONE_STEPS] rung (owning 21 buys 4, reaching 25) — past the last
    defined rung (10,000) it falls back to rounding up to the next multiple
    of 10,000; `MAX` is `CreatureLair.maxAffordableUnits` (can resolve to 0,
    in which case both `LairCard` and `claimLair` `coerceAtLeast(1)` so a
    cost preview still shows and the button still correctly disables).
  - **`LairRow`** (`ui/game/LairRow.kt`) — wraps each lair list item as two
    separate containers sharing a `Row`: a circular `CreatureAvatar` on the
    left, `LairCard` on the right (`GameScreen`'s `LazyColumn` calls
    `LairRow`, not `LairCard`, directly). `Modifier.height(IntrinsicSize.Min)`
    on the `Row` plus `fillMaxHeight().aspectRatio(1f)` on the avatar makes
    the avatar a perfect circle that automatically matches the card's own
    height — no magic numbers kept in sync between the two; since v0.22.0
    that height is `LairCard.CARD_HEIGHT` (a fixed value, not content-driven
    the way it used to be — see that bullet), so the avatar ends up exactly
    that size too, unchanged mechanism, just a fixed rather than variable
    source height.
    `CreatureAvatar` shows real portrait art for the lairs that have it —
    `lairPortraitRes(lairId)` (v0.20.2: Kobold Warren, Giant Rat Burrow,
    Bugbear Warcamp so far, from `lair-kobold.png`/`lair-rat.png`/
    `lair-bugbear.png` in `/assets`, copied into `drawable-nodpi/` as
    `lair_<lairId>.png` after verifying each is a genuinely transparent
    square via corner pixel alpha) — clipped to the circle with
    `ContentScale.Crop` and the same carved-border ring as the placeholder.
    Every other lair still falls back to the placeholder: a rarity-tinted
    radial-gradient disc with a carved border and the monster's first letter
    in serif type — not unique per creature (a few tiers share an initial)
    but the rarity color band and the full name in the card right next to it
    already disambiguate. **Gotcha hit wiring the real art in:** the portrait
    `Image` must use `Modifier.matchParentSize()`, not
    `fillMaxHeight()/fillMaxWidth()` like the placeholder `Canvas` uses —
    this `Box` sits inside a `Row` with `Modifier.height(IntrinsicSize.Min)`,
    and unlike `Canvas` (which has no intrinsic size of its own), `Image`
    reports its painter's real intrinsic size during that intrinsic-height
    measurement pass; `fillMaxWidth()/fillMaxHeight()` let that leak through
    and blew the whole row up to the portrait's raw pixel size the first
    time this ran on the emulator. `matchParentSize()` sizes strictly off
    the already-resolved `Box` instead, sidestepping the intrinsic query
    entirely — reach for it any time an `Image`/`AsyncImage` needs to sit
    inside a `Box` that itself lives inside an `IntrinsicSize`-measured
    parent.
    Tapping the avatar starts the lair's production cycle exactly like
    tapping the card (see the redesigned gold-collection flow below): both
    share one hoisted `onStartLoad` action, and `canStartLoad` (`owned.count
    > 0 && !owned.hasSteward && !owned.isLoading`) gates both tap targets.
    **Tap-ability and visual dimming are two separate signals as of
    v0.22.2** — they used to be the same `canStartLoad` boolean, which meant
    a Steward-managed lair (never tappable, since there's nothing to tap —
    the Steward already collects it) was permanently rendered at 0.55 alpha
    even while actively producing. `isBright` (`owned.count > 0 &&
    (owned.hasSteward || !owned.isLoading)`) is the separate signal
    `CreatureAvatar`'s `alpha` now actually reads; only an owned,
    Steward-less lair mid-load still dims, same as before. Owns the
    `coinBurstTrigger` counter passed into `LairCard` as a plain `Int`
    parameter instead of `LairCard` keeping that counter as local `remember`
    state — needed so both tap targets fire the same `CoinBurstOverlay`
    (which still only renders over the card, not the avatar). Unlike the old
    tap-to-collect flow, `LairRow` doesn't bump the counter on tap anymore —
    a `LaunchedEffect` watches `OwnedLair.completedLoads` (see `GameEngine`
    below) and bumps `coinBurstTrigger` whenever it changes, so the burst
    fires at the moment a started cycle actually finishes, not at the moment
    of the tap.
  - **Gold collection redesign (v0.20.0):** a lair without a hired Steward no
    longer fills continuously in the background — it sits idle at 0%
    (`OwnedLair.cycleProgressSeconds` pinned to 0) until tapped. Tapping it
    (`GameEngine.startLairLoad`) starts the cycle; it fills up over its
    normal production time with no further input, then automatically
    credits the gold and fires the coin-burst effect the instant it
    completes — there's no separate "ready, waiting to be collected" state
    to tap through anymore. Tapping again while already mid-cycle is a
    no-op. This also means an idle unmanaged lair earns nothing while the
    app is closed unless it happened to be mid-cycle when the player left —
    a deliberate, confirmed change to offline earnings (see `applyOfflineEarnings`
    below). **Steward-managed lairs are completely unaffected** — they keep
    auto-collecting every completed cycle continuously, online or offline,
    with no confetti, exactly as before; `isLoading`/`completedLoads` are
    meaningless once `hasSteward` is true.
  - **`LairCard` layout (redesigned v0.22.0)** — piloted first as a
    standalone HTML mockup (an Artifact, reskinned from a layout description
    the user supplied from another of their games) before being ported to
    Compose, replacing the original design where the *entire* card
    background doubled as its own progress-fill bar. No Material `Card` —
    a custom `Box`, but now a **fixed height** (`CARD_HEIGHT`, 96.dp — no
    longer `Modifier.height(IntrinsicSize.Min)` sized off content) split
    into two equal `weight(1f)` rows:
    - **Top row**: the lair name and challenge rating on one truncating
      line — `"Kobold Warren (1/8 CR)"`, built as a single
      `buildAnnotatedString` (bold ink name + smaller/muted `SpanStyle` for
      the `"(${lair.challengeRating} CR)"` suffix) so the two truncate
      together via one `Text(maxLines = 1, overflow = Ellipsis)` rather than
      as independent `Text`s that could wrap or truncate inconsistently —
      the monster type (e.g. "Kobold") that used to sit on its own italic
      line is dropped entirely; there wasn't room for it alongside a real
      progress bar, and the lair name already implies it in every case so
      far. Below that, a dedicated progress-bar track (an 18.dp-tall
      `Box`, `clip(RoundedCornerShape(9.dp))`, `palette.woodDark` @ 0.35
      alpha for the dark inset groove) containing the animated
      `rarityColor(tier)`-gradient fill, a 5.dp white-to-transparent gloss
      strip along the fill's own top edge, and the income/cycle-time line
      (`"${gp} gp / ${cycle time}"`, via `ui/format/CycleTimeFormat.kt` —
      see below) centered *on top of* the bar itself instead of sitting on
      a separate line elsewhere in the card, as it did before this
      redesign. An unclaimed lair (`owned.count == 0`) shows the track
      empty with a muted "Claim to begin" prompt instead of a real
      fraction. **Gotcha hit here:** the first pass computed the bar's
      height by applying `.padding(top = 4.dp)` *before* `.clip(...)` in
      the modifier chain, which shrinks the box `clip` actually bounds
      rather than adding space above it — the progress label's text
      (sized off `MaterialTheme.typography.labelSmall`'s default line
      height, taller than the shrunken clipped area) visibly spilled down
      into the buy-button row below. Fixed by moving the top gap to
      `Arrangement.spacedBy(...)` on the parent `Column` instead of
      `.padding` on the bar itself, and by giving the label an explicit
      small `fontSize`/`lineHeight` rather than trusting the default
      Material type scale to fit a compact custom-height bar — the same
      class of bug as the earlier `IntrinsicSize`/`Image` gotcha under
      `LairRow`: a Material text style's real metrics rarely match a
      hand-sized container exactly, so hand-sized containers need an
      explicit, matching text size, not an inherited default.
    - **Bottom row**: a `BuyButton` (private composable in this file — not
      the shared `WoodenButton`, which is a single-line pill shape and
      doesn't fit a two-line stacked label) takes most of the width — a
      gold gradient (`palette.goldBright`/`goldDeep`) when affordable
      (superseded by a per-tier rarity gradient in v0.22.1, then a dimmed
      version of that same gradient when unaffordable in v0.22.3 — see
      those bullets), two centered lines (quantity, then price). A
      fixed-width (`OWNED_BOX_WIDTH`, 56.dp) `OwnedBox` panel to
      its right replaces the old "Owned: N" text line — a recessed
      `palette.woodDark`-tinted panel with the count and an "owned" label.
    The outer `Box` keeps a translucent parchment gradient base
    (`FantasyPalette.parchmentShade`/`parchment`) and a per-tier
    `rarityColor(tier)` border (matching `GameHeader`'s cozy-fantasy chrome
    rather than flat Material blocks), and its `clickable`
    (`enabled = owned.count > 0 && !owned.hasSteward && !owned.isLoading`,
    `onClick = onStartLoad`) still starts the cycle on a tap anywhere on
    the card, same as before — only the internal layout changed, not the
    tap contract. **The Steward button is gone** — hiring a Steward lives
    solely in the Stewards menu section (see `StewardsContent` below), not
    on every card; `LairCard` no longer takes `onHireSteward`.
    The income/cycle-time text itself (`"${gp} gp / ${cycle time}"`,
    v0.21.5) is unchanged in content from before this redesign, just
    relocated onto the bar — `ui/format/CycleTimeFormat.kt` exists because
    neither `GoldFormat` nor the existing `DurationFormat` cooldown
    formatter (whole-minutes-only) covers the ms-to-multi-day range a
    lair's actual cycle time can span; [productionSeconds] (the same value
    `GameScreen` already computes for its gold-per-second sum) is passed
    down through `LairRow` as its own prop just for this display.
  - **Rarity visibility fix (v0.22.1)** — v0.22.0's redesign shipped with a
    gold gradient on the buy button's affordable state, same as the old
    single-row card. That collided with gold *also* being this game's
    legendary-tier `rarityColor` band: once most lairs were affordable (the
    ordinary mid/late-game state), gold covered nearly every card
    regardless of tier, both overwhelming the screen and making "gold =
    legendary" meaningless. Piloted several fixes as an HTML mockup (four
    variants compared side by side) before building the chosen combination:
    - `BuyButton`'s affordable gradient is now `rarityGradient(lair.tier)` —
      a new private hand-picked light/dark pair per tier (not derived
      algorithmically from `rarityColor`, since an automatic lighten/darken
      can drift far enough to stop reading as the same hue at the
      extremes) — so gold only appears on an actually-legendary lair now.
      The unaffordable state was a flat brick-red at this point (superseded
      in v0.22.3 — see that bullet).
    - A solid `RARITY_STRIPE_WIDTH` (6.dp) stripe in the flat `rarityColor`
      runs down the card's left edge, and the whole card background gets a
      faint (`alpha = 0.16f`) rarity wash layered over the parchment
      gradient — both reinforcing the tier even before looking at the
      button. The content `Column` picks up `padding(start = RARITY_STRIPE_WIDTH)`
      so the name/progress bar and the bottom row both clear the stripe.
    - The outer border dropped its own rarity tint (`rarity.copy(alpha = 0.7f)`)
      to a neutral `Color.Black.copy(alpha = 0.35f)` — a colored border
      alongside a colored stripe read as cluttered in the mockup
      comparison, and the stripe/wash already carry that signal.
  - **Unaffordable-state color fix (v0.22.3)** — the flat brick-red
    unaffordable color from v0.22.1 turned out to have its own problem once
    live: with plenty of gold in hand and only the highest tiers still out
    of reach, the muted red buttons looked more clickable than the game's
    normal affordable buttons, not less — red reads as an alert/call-to-action
    color, the opposite of "disabled." `unaffordableLight`/`unaffordableDark`
    are gone; `BuyButton` now always renders its own `rarityGradient(tier)`
    and fades the whole button to `UNAFFORDABLE_ALPHA` (0.45f, applied via
    `Modifier.alpha`) when `!affordable`, matching `WoodenButton`'s existing
    disabled treatment elsewhere in the app rather than inventing a second
    "disabled" language. Verified live across all five tiers on the
    emulator — affordable buttons stay vivid, unaffordable ones fade to a
    muted version of the same rarity color instead of switching to red.
  - **Progress-bar smoothing (v0.21.2)** — `LairCard`'s fill fraction used to
    be derived per-composable from raw `OwnedLair.cycleProgressSeconds` /
    `CreatureLair.effectiveProductionSeconds`, animated via
    `animateFloatAsState` with its tween duration tied to
    `GameEngine.TICK_INTERVAL_MS` (33ms) so it wouldn't visibly step between
    ticks. That broke down once milestone/Speed-Boost stacking pushed a
    lair's cycle time down near or below the tick interval itself: a
    Steward-managed lair's `advanceLair` loop can complete several cycles
    inside one tick, leaving only a `remaining` modulo-remainder to display,
    and sampling that remainder once every 33ms — with a 33ms tween chasing
    it just as fast — read as the fill bar "bouncing up and down starting
    and stopping at random spots" instead of animating, because the target
    itself was aliased against too coarse a sampling rate; no client-side
    easing curve fixes a signal that's already lost information at the
    source. Fixed by moving the fraction calculation into the engine itself,
    matching a smoothing pattern already proven in another project (map of
    per-item progress recomputed once per tick, exposed as its own
    `StateFlow`, `animateFloatAsState` in the UI on a *fixed*, tick-rate-independent
    tween):
    - `GameEngine.lairProgress: StateFlow<Map<String, Float>>` — recomputed
      in `tick()` via a new private `computeLairProgress(state)`, once per
      tick, right after `advance()` updates `state`. An idle unmanaged lair
      (owned, not `isLoading`) is pinned at `0f`. Below
      `GameEngine.PROGRESS_SOLID_THRESHOLD_SECONDS` — the point past which a
      lair's own `effectiveProductionSeconds` can complete inside a single
      tick, so the raw ratio stops being meaningful — it reports a flat `1f`
      instead of the raw ratio, a continuously solid bar, the *truthful*
      picture once cycles complete far faster than a human can watch one
      fill. `GameViewModel.lairProgress` exposes it straight through;
      `GameScreen` collects it and passes `lairProgress[lair.id] ?: 0f` down
      through `LairRow` to `LairCard`'s new `progress: Float` param (`LairCard`
      no longer takes `speedBoostMultiplier`/`globalSpeedMultiplier` — it
      doesn't need to derive anything itself anymore).
    - `LairCard`'s `animateFloatAsState` now uses a **fixed tween**
      (`PROGRESS_ANIMATION_DURATION_MS`), deliberately *not* tied to
      `TICK_INTERVAL_MS` the way it used to be — tracking the tick rate
      exactly meant the animation re-synced to a fresh target almost
      immediately every tick, so a fast-resetting target produced visible
      bounce instead of being smoothed over several ticks.
    - **Snap-on-reset (v0.21.3)** — the first-pass 150ms tween fixed the
      bounce but introduced a new, subtler issue: since [progress] only ever
      increases within a cycle and drops exactly once on completion,
      tweening *that* drop the same smooth way made the bar visibly slide
      backward into its next cycle instead of resetting cleanly — and since
      that backward slide ate into the tween's own window, it also cut the
      next forward tween short enough that a moderately fast lair's bar
      rarely looked like it actually reached full before resetting again.
      `LairCard` now `remember`s the previous recomposition's raw `progress`
      value per card; any *decrease* (never a natural part of the ramp —
      there's no other way `progress` goes down) swaps the `animationSpec`
      to `snap()` for that one frame instead of `tween(...)`, so the bar
      jumps instantly to empty and the next fill starts clean.
    - **Tuning pass (v0.21.4)**, after the snap-on-reset fix still left two
      complaints: some lairs' bars still snapped back around 80–90% instead
      of visibly reaching the end, and the solid cutoff kicked in too early
      (around the 200-owned Speed milestone, not much past the first rung)
      for the player's taste. Two independent constant changes:
      - `PROGRESS_ANIMATION_DURATION_MS`: 150ms → **60ms**. A tween always
        lags its target by roughly its own duration while continuously
        chasing it — with 150ms and a production cycle only a few hundred
        ms long, that lag alone was enough that the animated value never
        caught up before the next reset snapped it back down. 60ms (still
        ~2x `TICK_INTERVAL_MS`, enough to smooth per-tick quantization)
        shrinks that lag enough for cycles of a few hundred ms or longer to
        visibly reach close to full before resetting.
      - `PROGRESS_SOLID_THRESHOLD_SECONDS`: `3x TICK_INTERVAL_MS` (~99ms) →
        **`MIN_CONFETTI_PRODUCTION_SECONDS`** (10ms, the same constant the
        coin-burst effect already uses to decide "too fast to bother
        animating"). 99ms was calibrated purely to the tick rate's own
        sampling limits, but that meant lairs went solid as early as the
        200-owned Speed milestone (16x) — nowhere near their own milestone
        ladder being exhausted. 10ms lines the cutoff up with a value
        already deliberately calibrated to "genuinely extreme, beyond
        ordinary milestone stacking" — a lair maxing its own individual
        Speed milestones alone (400 owned, 64x) sits right at this line
        rather than well past it, so most lairs keep showing a real
        (if very fast, and below `TICK_INTERVAL_MS`, not perfectly smooth)
        animation until additional stacking (the global "Everything" Speed
        bonus, or a purchased Speed Boost) pushes them further — an
        accepted trade-off over freezing the bar prematurely.
      Confirmed via bursts of screenshots: a lair previously solid at
      ~200 owned (37.5ms cycle) now shows a real fill climbing past 90%
      before resetting; moderately-fast Steward-managed lairs that used to
      snap back around 80–90% now visually read as consistently
      near-full/solid between samples, since the shorter tween lets them
      actually reach close to 100% each cycle.
    - **Tick-rate tuning (v0.21.6)** — v0.21.4's fixes still left a
      remaining complaint: a lair right above `PROGRESS_SOLID_THRESHOLD_SECONDS`
      (a Kobold Warren at 38ms — still comfortably above 10ms, so genuinely
      animating rather than solid) visibly bounced instead of climbing
      smoothly. Root cause was sampling resolution, not animation tuning:
      `GameEngine.TICK_INTERVAL_MS` at 33ms meant a 38ms cycle only got
      ~1.1 samples of `lairProgress` per cycle — one sample can't
      distinguish "just started" from "about to finish," so the raw target
      itself (not just its rendering) jumped around. Lowered
      `TICK_INTERVAL_MS` to **8ms** (a 38ms cycle now gets ~4-5 samples)
      and `LairCard.PROGRESS_ANIMATION_DURATION_MS` to **20ms** in lockstep
      (short enough to still let a few-tens-of-ms cycle visibly climb most
      of the way to full, long enough — ~2.5x the new tick interval — to
      smooth the now much finer per-tick steps). This doesn't eliminate the
      underlying limit — there's always some cycle time fast enough to
      alias against whatever the tick rate is — it just pushes the point
      where that stops mattering further out, from "just past the 200-owned
      Speed milestone" to comfortably past it. A ~4x tick-rate increase
      (33ms → 8ms) means the engine's tick loop and `computeLairProgress`
      now run 4x as often; both are cheap arithmetic over the lair catalog
      (~14 entries), so this is a deliberate trade of a small, likely
      negligible CPU/battery cost for meaningfully smoother animation at
      the speeds late-game milestone/Speed-Boost stacking actually reaches
      — revisit if it ever shows up as a real battery concern. Confirmed via
      a burst of screenshots: the previously-bouncing 38ms lair now holds a
      stable, consistently near-full fill across consecutive frames instead
      of jumping to random low values.
    - **Container change (v0.22.0)** — none of the engine-side computation
      or animation-timing logic above changed when `LairCard` was
      redesigned; only *where* `animatedFillFraction` gets drawn did — a
      dedicated progress-bar track inside the top row instead of the whole
      card's own background. The old bright leading-edge line (drawn via
      `drawBehind` at the fill's own right edge) didn't carry over; the new
      track uses a gloss highlight strip along its top edge instead,
      matching the mockup this layout was piloted from.
  - **`CoinBurstOverlay`** (`ui/game/CoinBurst.kt`) — a one-shot radial burst
    of small gold coins (plain `Canvas`-drawn circles with a darker rim, per
    the stated art style — no sprite asset) fired only when a manually
    started cycle actually completes (from either the card or its avatar —
    see `LairRow` above), not a Steward's automatic collection, since both
    tap targets share `LairRow`'s hoisted `coinBurstTrigger` counter, which
    only gets bumped by the `LaunchedEffect` watching `OwnedLair.completedLoads`
    — a Steward's auto-collect runs inside `GameEngine`'s tick loop and never
    touches that counter. Uses a
    counter rather than a boolean so a
    second completion mid-animation restarts the effect (`key(trigger)` tears
    down and relaunches the old one) instead of being a no-op. Rendered as
    the last child in `LairCard`'s `Box` (`Modifier.matchParentSize()`, no
    pointer input) so it draws over the card's content without blocking taps
    on it. Clips to the card's own rounded-rect bounds like everything else
    in that `Box` — coins bursting past the edge just get clipped there,
    which reads fine at this card size and duration (~650ms). Below a 10ms
    production time (`GameEngine.MIN_CONFETTI_PRODUCTION_SECONDS` —
    reachable only after dozens of stacked Speed Boost levels) the
    completion doesn't bump `completedLoads` at all, so the burst is skipped
    entirely; the gold is still credited either way.
  - **`SettingsContent`** (`ui/settings/SettingsContent.kt`) — the Settings
    section's real content: an Account card (sign up/in/out), a Cloud
    Sync card (automatic-every-5-minutes note, last-synced time, manual
    "Sync Now" — **gated to signed-in players since v0.38.2**: per
    explicit request, a guest can no longer tap it. A guest's anonymous
    identity already syncs automatically every 5 minutes same as anyone
    else's — see `GameViewModel.runCloudSyncLoop` — but that identity has
    no recovery story on reinstall regardless, so a manual on-demand sync
    doesn't actually buy a guest anything; the disabled button shows an
    explanatory note instead, mirroring `ShopContent`'s guest-gated "Buy
    Platinum Pieces" section both in look and in reasoning — keep a
    manually-triggered action tied to a recoverable account), and
    (v0.28.2) a plain centered version footer reading
    `BuildConfig.VERSION_NAME` directly — the one thing on this screen
    that isn't ViewModel state, since it's a compile-time constant, so
    it's not threaded in as a parameter like everything else here.
    No separate `AuthViewModel`/`SettingsViewModel` exists —
    this account/sync state all lives directly on `GameViewModel` (see its
    class doc), since signing in or out directly changes which cloud row
    the save syncs to; splitting that across two ViewModels would just mean
    passing the new user id back and forth. Same pure-display-plus-callbacks
    pattern as `StewardsContent`/`ShopContent` — takes `userEmail` (null
    means guest), `pendingVerificationEmail`, `isAuthActionInProgress`,
    `authMessage`, `isSyncing`, `lastSyncedAt` plus `onSignUp`/
    `onVerifySignUpCode`/`onResendSignUpCode`/`onCancelSignUpVerification`/
    `onSignIn`/`onSignOut`/`onSyncNow`/`onDismissAuthMessage` callbacks, all
    wired straight to matching `GameViewModel` methods in `MainActivity`. A
    guest sees a short intro plus "Create Account"/"Sign In" buttons that
    reveal an inline email/password form (Material `OutlinedTextField`,
    palette-tinted, not a fully custom wooden field — the one place in the
    app using stock Material input styling rather than a hand-drawn Canvas
    control); submitting "Create Account" transitions the card into a
    third state — a verification-code entry step (`VerificationCodeForm`,
    driven by `pendingVerificationEmail` being non-null — see the sign-up
    bullet below for why this step exists) — instead of going straight to
    signed-in; a fully signed-in player sees their email and a "Sign Out"
    button. The email/password form collapses back to its two buttons
    immediately on submit (optimistic) rather than waiting for the result
    — success and failure both just show as an `authMessage` banner
    afterward, and a failed attempt means tapping the button again to
    retry with fresh fields, a deliberate simplicity trade-off over
    persisting the typed values through a retry. The verification-code
    form does *not* auto-collapse the same way — it stays open (with
    Verify/Cancel buttons and a "Resend code" text link) until either the
    code succeeds (`pendingVerificationEmail` goes back to null) or the
    player cancels, so a wrong code doesn't force retyping the whole
    email/password again.
  - **Account/sync on `GameViewModel`** — `userEmail: StateFlow<String?>`
    (drives IAP gating — see Monetization), `pendingVerificationEmail`
    (drives Settings into the code-entry step), `authMessage`/
    `isAuthActionInProgress` for the sign-up/in/out forms,
    `lastSyncedAt`/`isSyncing` for the sync card. `signUp(email, password)`
    calls `AuthRepository.signUp`, which upgrades the *current* guest
    session in place via Supabase's `updateUser` (not `signUpWith`, which
    would create an unrelated new user) — same user id, same cloud save, no
    merge needed. **Sign-up is deliberately two-step, not one** — an
    anti-bot/anti-spam gate on account creation, not just an
    email-ownership nicety (added in 0.16.0 per an explicit user request):
    `signUp` only *starts* the upgrade; if `currentUserEmail()` still reads
    null right after (the expected case — see the Auth section's dashboard
    requirement below), `pendingVerificationEmail` is set and the player
    must call `verifySignUpCode(code)` — which calls
    `AuthRepository.verifySignUpCode` (Supabase's `verifyEmailOtp` with the
    `EMAIL_CHANGE` OTP type, not `SIGNUP` — from Supabase's point of view
    the account already exists as our anonymous user and we're just
    setting its previously-empty email, the same flow as a normal email
    change) — before the account is actually live. `resendSignUpCode()`
    (`AuthRepository.resendSignUpCode`, `auth.resendEmail(EMAIL_CHANGE,
    email)`) and `cancelSignUpVerification()` (clears
    `pendingVerificationEmail`, backs out to the plain guest buttons) round
    out that step. If the Supabase project has email confirmation turned
    *off* instead, `currentUserEmail()` is already non-null right after the
    initial `signUp` call, and the whole code step is skipped — the account
    is just live immediately, same as before 0.16.0 (though this defeats
    the anti-bot purpose the feature exists for — see the Auth section).
    `signIn(email, password)` calls `AuthRepository.signIn`
    (`auth.signInWith(Email)`) to switch to a *different*, already-existing
    account — a different user id — so `GameViewModel` downloads that
    account's cloud save and reconciles it against the current live state
    via `mergeGameStates` (the same merge used on launch), then re-uploads
    the merged result. `signOut()` syncs the outgoing account's cloud row
    one last time, calls `AuthRepository.signOut`, then immediately calls
    `ensureSignedIn()` again to re-establish a fresh guest session — local
    play is never interrupted, only the cloud identity changes (a signed-
    out account's cloud row is untouched afterward; the next guest gets a
    brand-new empty one, consistent with the existing "reinstall = new
    anonymous identity" design). Cloud sync now also runs on a repeating
    5-minute timer (`runCloudSyncLoop`, alongside the existing 30s local
    `runAutosaveLoop` — the two run as separate concurrent coroutines under
    `viewModelScope`, not chained, since both are infinite loops), in
    addition to the existing once-per-launch sync and the Settings screen's
    manual "Sync Now" button — all three funnel through one private
    `syncToCloud()`.
  - **Found while building this:** Supabase returns `""` (not null) for a
    guest's email — `AuthRepository.currentUserEmail()` normalizes blank to
    null so "is this a guest" has one clean signal. Don't assume a Supabase
    string field is null just because it's logically absent; check for
    blank too.
  - **Leaderboard usernames (v0.39.0, reworked to an inline field in
    v0.39.1)** — a `public.profiles` table (`user_id` primary key,
    `username`, unique case-insensitively via a `lower(username)`
    functional index — see `SQL/003_create_profiles_table.sql`),
    deliberately a *separate* table from `cloud_saves` rather than a new
    column on it: a username needs to be publicly readable (any player,
    for a future leaderboard join) while `cloud_saves.state` stays
    private to its own row — mixing the two would mean relaxing RLS on
    the whole save blob just to expose a name.
    `AuthRepository.currentUsername()`/`setUsername()` (implemented in
    `SupabaseAuthRepository` via plain Postgrest calls, same
    `.from(table).select{...}`/`.upsert(...){ onConflict = ... }` shape
    `SupabaseCloudSaveRepository` already established) back
    `GameViewModel.username`/`submitUsername`/`dismissUsernameMessage`.
    A private `refreshUsernameState()` keeps `username` in sync with the
    current session, called after every point `userEmail` changes — the
    immediate (no-confirmation-needed) branch of `signUp`,
    `verifySignUpCode`, `signIn`, `signOut`, and once at app launch for an
    already-signed-in session, so a pre-existing account from before this
    feature shipped picks it up too, not just brand-new sign-ups; a null
    `userEmail` (guest, or right after `signOut`) clears `username`
    without a network call at all, since guests never see the field.
    `isValidUsername` (`domain/model/Username.kt`, unit-tested) gates the
    field client-side: 3-20 characters, letters/digits/underscore only.
    **v0.39.0 first shipped this as `UsernamePromptDialog`, a `Dialog`
    that popped up unprompted the instant registration completed** —
    scrapped entirely per explicit correction: no more surprise pop-up,
    and the field now only exists inside `SettingsContent.kt`'s
    `AccountCard`, inline, right under the "Signed in as ..." line —
    label, `OutlinedTextField`, and a "Save" button in a `Row`, with a
    small helper/error caption underneath (see `UsernameField`, private
    to that file now that nothing outside it needs to render this
    anymore). It only renders inside `AccountCard`'s `userEmail != null`
    branch, so — matching the explicit "only usable once logged in"
    requirement — a guest never sees any trace of it, the same gating
    `SyncCard`'s "Sync Now" and `ShopContent`'s "Buy Platinum Pieces"
    already use. The field's local text `remember`s keyed on `username`
    so it re-syncs to the confirmed value after a successful save (or
    when a different username loads in, e.g. right after sign-in) instead
    of holding a stale edit, and "Save" stays disabled until the typed
    value is both a valid username *and* different from what's already
    saved. There's no more `needsUsername`/`dismissNeedsUsername`/
    `promptUsernameChange` on `GameViewModel` at all — nothing needs a
    "should a dialog be open" flag once there's no dialog.
    **A real bug caught via live device testing, not hypothetical:**
    `GameViewModel.usernameErrorMessage(e)` originally pattern-matched
    `e.message` for the substring "duplicate"/"unique" to detect an
    already-taken username. This is fundamentally broken with
    supabase-kt 3.0.1's `RestException` — its `message` property (see
    that class's source) bundles the *full* request diagnostics (URL,
    headers — including this session's own bearer token!) after the
    clean one-line `error` property, and the `setUsername` upsert's own
    `Prefer: resolution=merge-duplicates` header means that dump always
    contains the literal word "duplicate" regardless of what actually
    failed — confirmed live when a genuine "table doesn't exist yet"
    `NotFoundRestException` got mis-displayed as "That username is
    already taken." Fixed to check `RestException.statusCode == 409`
    (PostgREST's real status code for a unique-constraint violation)
    and fall back to `RestException.error` (never `.message`) for any
    other failure — both more correct *and* closes what would otherwise
    have been a real secret-leaking bug, since the raw `.message` would
    have put the caller's own auth bearer token directly in the dialog
    for any non-duplicate error. Verified live on-device against the
    real Supabase project, in two passes: first before the SQL script
    had been run (table-less), confirming the error path — saving any
    username showed the false "already taken" message every time before
    the fix, and correctly showed "Could not find the table
    'public.profiles' in the schema cache" (accurate, no token leakage)
    after; then again after `SQL/003_create_profiles_table.sql` had
    actually been run — a save now succeeds with no error, and a fresh
    app process launched afterward reads the saved username straight
    back from Supabase, confirming genuine round-trip persistence
    against the real table.
    **`SQL/003_create_profiles_table.sql` must be run once** in the
    Supabase dashboard before any of this persists — same category of
    manual dashboard dependency as `001_create_cloud_saves_table.sql`
    and the AdMob/Play Console setup documented elsewhere in this file.
    Before it's run, every `setUsername` call fails with the
    `NotFoundRestException` above (surfaced honestly, not silently) —
    expected, not a regression, until the script is applied. Already
    run and confirmed working as of v0.39.1.
  - **Leaderboard (v0.41.0)** — three ranked boards (All-Time / Weekly /
    Monthly gold earned), reachable from the menu as "Leaderboard" (no
    sign art yet — falls back to `FloatingMenu`'s plain-`Surface`
    treatment, same as every section added before its own art exists).
    Built from an explicit 3-question design pass: ranked by
    `GameState.lifetimeGoldEarned` deltas over each window (the user's own
    answer, plus the exact reset times — "Monthly resets the last day of
    every month at 11:59pm EST. Weekly resets every Sunday night at
    11:59pm EST. The rankings update once every hour"); guests excluded
    entirely (only players with a `profiles` username ever appear); the
    screen shows a top-50 list plus the current player's own rank pinned
    below it if they're outside that top 50.
    **Rankings are computed server-side, once an hour — never live, and
    never by the client.** `SQL/004_create_leaderboards.sql` (run once,
    same manual-dashboard-script category as `001`/`003`) creates:
    - `leaderboard_baselines(user_id, period, period_start, baseline_gold)`
      — each player's `lifetime_gold_earned` as of the start of the
      *current* week/month, so "earned this period" is just current value
      minus this baseline. A row only rolls over once the stored
      `period_start` is older than the real current period's start
      (`on conflict ... do update ... where period_start < excluded.period_start`)
      — a player who creates their account mid-period simply starts
      counting from whenever they were first observed, not retroactively
      to the period's actual boundary. No client-facing RLS policy at all
      — only `refresh_leaderboards()` ever touches this table.
    - `leaderboard_rankings(period, user_id, username, gold_earned, rank,
      computed_at)` — what the app actually reads (`order by rank limit
      50`, publicly readable via one `select using (true)` policy, same
      shape as `profiles`'s own public-read policy). `period` is one of
      `all_time`/`weekly`/`monthly`; `all_time` needs no baseline since
      it's `lifetime_gold_earned` itself.
    - `refresh_leaderboards()` — a `SECURITY DEFINER` function (so it can
      read every player's `cloud_saves` row despite its own-row-only RLS —
      only this function, owned by postgres and never exposed to
      clients, needs that access) that rolls over baselines where due,
      then rebuilds all three `leaderboard_rankings` sets from scratch via
      `row_number() over (order by ... desc)`, joined against `profiles`
      (an inner join — this is the actual mechanism that excludes guests,
      since a guest has no `profiles` row at all). Scheduled hourly via
      **pg_cron** (`cron.schedule('refresh-leaderboards', '0 * * * *',
      ...)`) — the script unschedules any existing job of the same name
      first so re-running it doesn't stack duplicates, and also runs the
      function once immediately so the boards aren't empty until the
      first scheduled tick. **Needs the pg_cron extension enabled** — the
      script's own `create extension if not exists pg_cron` handles this
      on most Supabase projects; if it errors, enable "pg_cron" via
      Dashboard > Database > Extensions first, then re-run the script.
    `domain/model/Leaderboard.kt` (`LeaderboardPeriod` enum —
    `wireName`/`label` pairs matching the SQL `period` values —
    `LeaderboardEntry(rank, username, goldEarned, isCurrentUser)`),
    `domain/repository/LeaderboardRepository.kt` (`fetchTop`/
    `fetchCurrentUserEntry`), `data/remote/SupabaseLeaderboardRepository.kt`
    (plain `.from("leaderboard_rankings").select { filter {...}; order("rank",
    Order.ASCENDING); limit(...) }`, same Postgrest-kt DSL
    `SupabaseAuthRepository` already established — `fetchTop` marks
    `isCurrentUser` by comparing each row's `user_id` against the live
    session's own id, no separate flag needed from the server) — bound in
    `SupabaseModule.kt`'s existing `SupabaseRepositoryModule`.
    `GameViewModel.loadLeaderboard(period)` fetches both the top list and
    the player's own entry together; a guest short-circuits with an empty
    list and no network call at all, same "nothing to fetch" shortcut
    `refreshUsernameState` already uses. `ui/leaderboard/LeaderboardContent.kt`
    is the section's real content — a `LeaderboardTabRow` (same
    `CutCornerShape` tab-button look as `ShopContent`'s `ShopTabRow`/
    `UpgradesContent`'s `UpgradeTabButton`, duplicated per this project's
    established per-file-duplication convention) over a `LazyColumn` of
    `LeaderboardRow`s (rank, username, gold earned via `GoldFormat`,
    highlighted gold when `isCurrentUser`); a guest sees the same board
    everyone else does (never hidden outright) plus a `GuestNoteCard`
    explaining they won't see themselves in it, matching `ShopContent`'s
    "look, don't touch" treatment for its own guest-gated IAP section.
    `MainActivity`'s `LaunchedEffect(openSection)` calls `loadLeaderboard()`
    the moment the section opens, same pattern as `markStewardOpportunitiesSeen`;
    switching tabs calls the same method with the new period.
    **A real bug caught via live device testing, not before the user ran
    the SQL script (v0.41.1):** `loadLeaderboard`'s first pass
    short-circuited the *entire* fetch for a guest ("if no `userEmail`,
    skip straight to an empty list") — copied from `refreshUsernameState`'s
    genuinely-nothing-to-fetch shortcut without noticing the two cases
    differ: a guest truly has no username to look up, but
    `leaderboard_rankings`' RLS policy is `select using (true)` — public,
    no session required at all — so the *top list* has nothing to do with
    whether the viewer is signed in; only `fetchCurrentUserEntry` (which
    needs a real account to look up) should ever be guest-gated. This
    wasn't caught before the fix shipped because verification up to that
    point only ever exercised the guest path *before* the SQL script had
    been run — an empty list looked like the correct "nothing to show yet"
    state either way, masking that guests weren't fetching at all. Caught
    once the user ran `SQL/004_create_leaderboards.sql` for real (its
    final `select cron.schedule(...)` returned a job id, confirming the
    whole script succeeded) and a real signed-in test account
    ("TestName1") was already sitting in `leaderboard_rankings` from the
    script's own immediate `refresh_leaderboards()` call — confirmed
    directly via a plain `curl` against the Supabase REST API using only
    the public anon key (no session at all), which correctly returned all
    three period rows for that account. The app itself, still on the
    pre-fix build, showed "No one's on this board yet" as a guest despite
    that real row existing — the actual bug, not a false negative.
    Fixed by only guarding the `fetchCurrentUserEntry` call on
    `_userEmail.value != null`, not the whole fetch. **Verified live
    on-device after the fix:** as a guest, the Weekly tab correctly shows
    "#1 TestName1 — 0 gp" (accurate — their baseline was captured at
    script-run time, so 0 earned since), and the All-Time tab correctly
    shows "#1 TestName1 — 2.00Qa gp", matching the real
    `lifetime_gold_earned` value read back via the same `curl` check —
    real cross-account data, rendered correctly for a guest viewer, with
    the `GuestNoteCard` still shown above it as intended.
  - **Settings' "Danger Zone" — Account Reset and Account Delete (v0.45.0)**
    — two new destructive actions, added per explicit request, built from a
    short confirmed design pass (Reset's exact scope, Delete's guest-vs-signed-in
    availability, and confirmation friction for each) rather than guessed:
    - **Reset Account** — `GameEngine.resetProgress()` wipes the current run
      back to the exact starting shape a brand-new `GameState` begins with
      (one Kobold Warren, 0 gold, 0 Gems, `totalLevelUps` back to 0,
      `selectedAvatarId`/`totalAdsWatched`/`lifetimeGoldEarned`/ad-watch
      cooldowns all reset too — genuinely starting over, unlike
      `performLevelUp`, which treats those as permanent identity/milestone
      state) — **except** Platinum Pieces and everything bought with them
      (`GameState.platinumPieces`, every permanent boost tier, every
      currently-running `activeTemporaryBoosts` instance), confirmed
      explicitly: real-money purchases (and Platinum earned for free)
      survive a reset. Available to a guest or a signed-in player alike —
      it never touches the Supabase auth session, only game progress.
      `GameViewModel.resetAccount()` also clears the player's leaderboard
      username via a new `AuthRepository.clearUsername()` (deletes the
      `profiles` row outright, since `username` is `not null` — a new
      delete policy for that table ships in `SQL/005_account_management.sql`,
      since `003`'s original policies only covered select/insert/update),
      then persists the fresh state to both Room
      (`GameRepository.replaceGameState`, see below) and — if signed in —
      the cloud row.
    - **A real Room bug caught while building this, not live**: `saveGameState`
      (`GameStateDao.saveAll`) only ever *upserts* — a lair no longer present
      in the state being saved (e.g. every lair but Kobold Warren, right
      after a reset) would keep sitting in the `owned_lairs` table
      untouched, and get silently read back into `GameState.lairs` on the
      next load, quietly undoing the reset. Fixed with a new
      `GameRepository.replaceGameState(state)` /
      `GameStateDao.replaceAll(...)` (clears `owned_lairs` in the same
      `@Transaction` before upserting) used specifically for Reset/Delete's
      full wipe — `saveGameState`/`saveAll` are untouched and still power
      every routine autosave, since a plain upsert is exactly right there
      (a lair's count only ever grows or resets wholesale, never needs
      mid-save deletion). **Verified live on-device** via a direct Room DB
      edit seeding extra state (5,000 gp, 77 pp, 12 Gems, `totalLevelUps=2`,
      two permanent boost levels, and a stray `giant_rat_burrow` row at 50
      owned) on a disposable fresh guest install (a throwaway `pm clear`,
      not the long-running dev test save, specifically so this destructive
      test wouldn't cost the project its existing hand-built test fixture):
      tapping Reset Account correctly zeroed Gold/Gems/lair ownership back
      to just Kobold Warren while the header still showed the full 77 pp,
      and pulling the Room DB straight afterward confirmed
      `owned_lairs` contained only `kobold_warren` — the stale
      `giant_rat_burrow` row was genuinely gone, not just hidden by the UI
      — with `platinumPieces=77` and both permanent boost levels intact in
      `game_state`.
    - **Delete Account** — `AuthRepository.deleteAccount()` calls a new
      `delete_own_account()` Postgres RPC (`SQL/005_account_management.sql`,
      same manual-dashboard-script category as `001`/`003`/`004` — **must
      be run once before this actually works**, otherwise the RPC call
      404s) since the client SDK has no direct "delete my own account"
      call — the real admin API needs a service-role key that must never
      ship in the app. The function is `SECURITY DEFINER` (runs as
      `postgres`, which can write to `auth.users`) but only ever deletes
      `auth.uid()`'s own row, never a caller-supplied id, so granting it to
      every `authenticated` session is safe; `cloud_saves`/`profiles`/both
      leaderboard tables already cascade off `auth.users`, so one delete
      cleans up everything. **Only offered to a real, signed-in email
      account** — confirmed explicitly, since a guest's anonymous session
      is already meaningless to "delete" (reinstalling does the same
      thing) — `SettingsContent`'s `DangerZoneCard` hides the button
      entirely for a guest, and `GameViewModel.deleteAccount()` guards
      against it too. `GameViewModel.deleteAccount()` calls the RPC, then
      —unlike `signOut()`, which deliberately preserves local progress —
      wipes local Room via `gameEngine.loadState(GameState())` +
      `gameRepository.replaceGameState(GameState())`, then calls
      `authRepository.signOut()` followed by `ensureSignedIn()` to
      re-establish a brand-new anonymous session, mirroring `signOut()`'s
      own re-establish shape so play can continue immediately. **Not
      verified end-to-end** (same category as `BillingManager`'s
      unverified real-purchase path) — the SQL script hasn't been run
      against the real Supabase project yet, and a real live test would
      need a disposable, fully-verified email account (real inbox access
      for the sign-up OTP) to exercise honestly; verified instead via code
      review plus a compile-time check of the exact supabase-kt 3.0.1
      `Postgrest.rpc(...)`/`.delete { filter {...} }` signatures
      (`javap`-inspected against the real `postgrest-kt` jars, same
      verification method `BillingManager` used for the Billing Library).
    - **`DangerZoneCard`** (`ui/settings/SettingsContent.kt`) — a
      `ParchmentCard` with a muted rust-red border (`DANGER_COLOR`,
      local to this file — `FantasyPalette` has no "danger" tone of its
      own) holding both actions, always shown on the Account tab below the
      Universal Steward status line. Confirmation friction matches
      severity, per explicit design: `ResetAccountConfirmDialog` is a
      plain two-button Cancel/Reset dialog (same shape as
      `LevelUpContent.kt`'s `LevelUpConfirmDialog`); `DeleteAccountConfirmDialog`
      additionally requires typing "DELETE" into a text field before its
      Delete button enables, since that action is permanent and removes
      the account entirely. Both buttons use `dangerPalette(palette)` — a
      local helper that retints just `WoodenButton`'s wood/gold tones red,
      since `WoodenButton` takes a whole `FantasyPalette` for its coloring
      rather than a single accent color — keeping the same carved-wood
      look as every other button in the app rather than inventing a
      second button style for this one case.
  - **`AdManager`** (`ads/AdManager.kt`) — the app's ad integration, via the
    Google Mobile Ads SDK (`play-services-ads`). `@Singleton`, same
    app-scoped pattern as `GameEngine`: constructed once by Hilt,
    initializes `MobileAds` and starts loading a rewarded ad for every
    `RewardedPlacement` immediately in its `init` block rather than waiting
    for a screen to ask for one — so an ad is normally already loaded by
    the time a player reaches a rewarded placement instead of loading on
    demand. `RewardedPlacement` is a small enum, one entry per real AdMob
    ad unit id (`OFFLINE_EARNINGS_DOUBLE`, `SHOP_PLATINUM` as of 0.18.0,
    `SHOP_SPEED_BOOST` as of 0.29.0, `AD_BOOST_INCOME` as of 0.34.0) —
    adding a new rewarded spot means adding an entry there; `AdManager`
    itself tracks a loaded-ad slot per placement (`Map<RewardedPlacement,
    RewardedAd>`) generically rather than one hardcoded field per
    placement, so it doesn't need touching again for a fifth. Two entry
    points, both placement-scoped: `isAdReady(placement)`/
    `showAd(placement, activity, onRewardEarned, onUnavailable)`. `showAd`
    always kicks off loading that placement's *next* ad afterward (on
    reward, on a mid-ad failure, and on a plain dismissal alike), and
    clears its held `RewardedAd` before showing so a second tap mid-show
    can't reuse a consumed one.
  - **Real ad units, forced into test mode for debug builds.** There is no
    separate test ad unit for either placement — both ids in
    `RewardedPlacement` are the actual production ones. To avoid ever
    loading/serving a real ad from a dev device (Google's invalid-traffic
    policy explicitly warns against this), `AdManager`'s `init` block
    registers `AdRequest.DEVICE_ID_EMULATOR` as a Google test device
    whenever `BuildConfig.DEBUG` is true — Google then serves its test
    creative (clearly labeled "Test Ad" on screen) through these same real
    ad unit ids, with no revenue or policy impact, and this applies to
    every placement automatically (it's a device-level registration, not
    per-ad-unit). **This must never be removed from debug builds.** A
    physical dev device (not an emulator) would need its own hashed
    test-device id added too — Logcat prints the exact id/line to add the
    first time that unregistered device requests an ad. Release builds
    (`BuildConfig.DEBUG == false`) skip this entirely and serve real ads
    normally.
  - **`GameEngine.grantGold(amount)`** — a flat, one-off gold credit
    outside the normal income/milestone pipeline, added for the Welcome
    Back ad-double reward (`_state.update { it.copy(goldPieces =
    it.goldPieces + amount) }`, nothing fancier).
    `watchAdToDoubleOfflineEarnings` calls it with the same
    `earnings.goldEarned` value already shown in the dialog, then updates
    the dialog's own displayed `OfflineEarnings` copy (`earnings.copy(goldEarned
    = earnings.goldEarned * 2)`) so the on-screen number reflects the
    double without a second `applyOfflineEarnings` call (which would
    re-run the whole production-advance pipeline — wrong tool for "just
    add this specific amount again").
  - **Shop's "Watch an Ad" (0.18.0)** — earns `PLATINUM_AD_REWARD_PP` (2 pp)
    once every `PLATINUM_AD_COOLDOWN` (24h), both in new
    `domain/model/AdRewards.kt` alongside `GameState.canWatchPlatinumAd`/
    `platinumAdCooldownRemaining` (pure, fully unit-tested cooldown math).
    The cooldown is tracked on the save itself
    (`GameState.lastPlatinumAdWatchedAt`), not ad-network- or device-side —
    it persists across sessions and syncs with the rest of the save, same
    trust model as everything else in this client-authoritative economy.
    `GameEngine.grantPlatinumAdReward(now)` grants the Platinum and stamps
    the watch time atomically, re-checking the cooldown itself rather than
    trusting `GameViewModel.watchAdForPlatinum` already did (belt-and-braces
    against a race between two rapid taps). `ShopContent`'s `AdRewardRow`
    (generalized from an originally Platinum-only `WatchAdRow` once the
    Speed Boost ad-watch below needed the identical card shape — see that
    bullet) computes its own "Available in Xh Ym" label reactively from
    `gameState.platinumAdCooldownRemaining()` passed in by `MainActivity` —
    no separate countdown timer or polling, it just updates naturally as
    `gameState` ticks. Shared `ui/format/DurationFormat.kt` formats that
    countdown for both `AdRewardRow`'s button label and
    `GameViewModel.platinumAdMessage`'s cooldown text, extracted once it
    was clear both needed the identical "3h 12m" logic.
  - **Ad-watch Speed boost (v0.29.0; moved out of the Shop into
    `QuickAdBoostButton`'s popup in v0.34.0 — see that bullet)** — a
    second, independent rewarded placement (`RewardedPlacement.SHOP_SPEED_BOOST`,
    ad unit id `ca-app-pub-1913393601233746/7941856119`). Originally
    added to the Shop's Temporary tab's "Earn a Free Boost" section above
    the existing "Buy a Boost" list of PP-bought temporary boosts (both
    section labels were new in v0.29.0; the tab had no section headers
    before that) — this bullet's description of the underlying reward
    mechanics is still accurate, only *where it's offered* changed in
    v0.34.0. Confirmed design (asked
    the user to disambiguate before building): watching grants
    `SPEED_BOOST_AD_MULTIPLIER` (2x) Speed for `SPEED_BOOST_AD_DURATION`
    (4h) as an ordinary [ActiveTemporaryBoost] (`domain/model/Boosts.kt`)
    — reusing that existing stacking mechanism completely rather than
    inventing a second one, so it stacks multiplicatively with itself and
    with any PP-bought Speed boost exactly like every other
    `TemporaryBoostCategory.SPEED` entry. Unlike the single-cooldown
    Platinum ad above, this one has **`SPEED_BOOST_AD_MAX_SLOTS` (4)
    independent daily slots**, each on its own 24-hour cooldown — watching
    all 4 back-to-back stacks four concurrent 2x boosts (16x total)
    instead of one shared cooldown only ever allowing one at a time. All
    the math lives in `domain/model/AdRewards.kt` alongside the Platinum
    reward: `GameState.speedBoostAdWatchTimestamps: List<Instant>` records
    *when* each of the last watches happened rather than tracking which
    literal slot (1-4) a watch occupied — a slot is "free" whenever fewer
    than 4 recorded timestamps are still within their own 24-hour window
    (`availableSpeedBoostAdSlots`/`canWatchSpeedBoostAd`/
    `speedBoostAdCooldownRemaining`, the last returning the soonest busy
    slot's own remaining time) — functionally identical to true fixed
    slots without needing to track slot identity. `GameEngine.grantSpeedBoostAdReward(now)`
    prunes expired timestamps, re-checks a slot is actually free (same
    belt-and-braces pattern as `grantPlatinumAdReward`), appends the watch
    and the new `ActiveTemporaryBoost` atomically in one `_state.update`,
    and returns whether it actually granted anything.
    `GameViewModel.watchAdForSpeedBoost`/`speedBoostAdMessage`/
    `dismissSpeedBoostAdMessage` mirror `watchAdForPlatinum`'s shape
    exactly. `speedBoostAdWatchTimestamps` is carried forward in
    `GameEngine.performLevelUp()` — an ad-watch cooldown is per-device
    grind, not run progress, same treatment as `lastPlatinumAdWatchedAt`
    and every other Platinum/ad-related field. Persisted as a JSON-encoded
    `List<Long>` (epoch millis) — `GameStateEntity.speedBoostAdWatchTimestampsJson`
    (Room, bumped to **database version 10**) via the same
    encode-a-list-into-one-String-column pattern already used for
    `activeTemporaryBoostsJson`, and `GameStateDto.speedBoostAdWatchTimestampsEpochMillis`
    (Supabase, a plain `List<Long>` — jsonb needs no encoding trick) with
    an empty-list default so an older cloud save without this field still
    decodes.
  - **Quick-access ad-watch popup (v0.30.0, redesigned into a two-option
    popup in v0.34.0, fronted with `tv.png` art in v0.34.1, tuned to a
    single larger TV in v0.34.2)** — a
    persistent button fixed in the main
    `GameScreen`'s bottom-right corner, per the original explicit
    request: players shouldn't have to "hunt" for an ad-watch reward.
    `ui/game/QuickAdBoostButton.kt`'s `QuickAdBoostButton` is the icon
    itself; tapping it opens `AdBoostPopup` (a plain `Dialog`, same
    parchment-scroll chrome as `WelcomeBackDialog`/`MilestoneReachedDialog`)
    rather than watching one specific ad directly, now that there are two
    rewards to choose from — see the Income boost bullet below. Visually
    the button is the real `media_play.png` art (v0.32.0 — a carved
    wooden circular play button; replaced an earlier hand-drawn Canvas
    medallion+triangle once real ad-specific art existed), **sized and
    bottom-aligned to exactly match `FloatingMenu`'s chest toggle**: same
    72.dp touch target around a 64.dp image, same 24.dp inset from the
    screen's bottom edge (`GameScreen`'s modifier bumped from
    `bottom = 16.dp` to `bottom = 24.dp` to make this line up) — per
    explicit request that the two read as a matched pair rather than one
    looking randomly bigger/higher than the other. A small gold
    `SlotBadge` overlapping the rim shows the *combined* available slots
    across both ad types (out of 8 total) — the popup itself is where the
    exact per-type breakdown lives, so the button stays a simple "there's
    something to watch" count; the play icon dims to `0.55f` alpha when
    the combined count is 0 but stays tappable regardless, so a tap
    during a dry spell still opens the popup and shows each option's own
    "In Xh Ym" cooldown instead of doing nothing. Each option is an
    `AdBoostOptionRow` (title, stacking description, Watch/cooldown
    button) with its own dismissible `AdBoostMessage` underneath it (same
    "✕" affordance used throughout the app) — the two options never share
    state, so watching one never affects the other's cooldown or message.
    **The popup itself is fronted with `tv.png`** (v0.34.1, tuned in
    v0.34.2) — the same "scrying TV" art `WelcomeBackDialog` already uses
    for its own ad prompt, shown once, large (140.dp — v0.34.1 first
    tried it smaller at 96.dp plus a second, smaller copy on each
    individual `AdBoostOptionRow`, but that read as cluttered; explicit
    follow-up feedback was to keep it to one bigger TV at the top and
    drop it from both rows entirely, which is what shipped in v0.34.2)
    above the "Watch an Ad" title. `media_play.png` stays specifically
    the main screen's own trigger-button icon (the thing you tap to
    *open* this popup), distinct from the TV motif used once you're
    actually looking at the ad-watch choices themselves.
    `GameScreen.kt` wraps its `AppBackground`/`Scaffold` content in an
    outer `Box` so this button (and the popup it opens) can be aligned
    `BottomEnd` as a sibling overlay, matching how
    `FloatingMenu`/`SectionOverlayCard` already float over the game in
    `MainActivity`.
    **Live combined "Active" countdown, not a static message (bug fix,
    v0.38.1)** — the post-watch `AdBoostMessage` used to read "2x Speed
    active for 4h!" (or "...Income active for 2h!"), always naming that
    one ad's own fixed duration no matter how many videos were actually
    stacked — a user-reported bug, confirmed live: after watching all 4
    Income slots back to back, the message still just said "2h!" instead
    of reflecting the real combined state. Fixed by threading
    `GameState.activeTemporaryBoostsRemaining()` — the same source
    `ShopContent`'s "Active" card already reads — down through
    `QuickAdBoostButton` → `AdBoostPopup` → `AdBoostOptionRow`, filtered
    per row by `TemporaryBoostCategory` (`SPEED` for the Speed row,
    `PROFIT` for the Income row) and **summed into one "Active — Xh Ym
    left total" line** (an early pass showed one line per stacked
    instance instead — technically accurate, since each watch really
    does expire independently, but per explicit follow-up direction a
    single combined total reads far more clearly than several
    near-identical countdown lines), recomputed every tick so it
    actually climbs with every video watched and counts back down live,
    rather than freezing at whatever it said when the ad finished. The
    post-watch message itself was reworded to stop claiming a duration
    ("Reward earned! ... stacked in — see the live countdown below.")
    now that the live total is the source of truth. Verified live
    on-device via a direct Room DB edit seeding 3 stacked Speed boosts
    and 2 stacked Income boosts with different expiry times: the popup
    correctly showed one combined "Active — 6h 56m left total" line
    under Speed (the sum of ~55m/2h25m/3h55m) and one "Active — 1h 27m
    left total" under Income — confirmed both totals ticking down
    correctly over a real ~65-second wait.
  - **Second ad-watch reward: temporary Income boost (v0.34.0)** — a
    2x Income (Profit) boost for 2 hours, added alongside the existing
    2x-Speed-for-4h reward specifically so `QuickAdBoostButton`'s popup
    would have two real choices rather than one. Exactly mirrors the
    Speed reward's shape in `domain/model/AdRewards.kt`
    (`INCOME_BOOST_AD_MULTIPLIER`/`_DURATION`/`_MAX_SLOTS`/`_COOLDOWN`,
    `GameState.availableIncomeBoostAdSlots`/`canWatchIncomeBoostAd`/
    `incomeBoostAdCooldownRemaining`) and `GameEngine.grantIncomeBoostAdReward`
    (same shape as `grantSpeedBoostAdReward`, granting an
    `ActiveTemporaryBoost(TemporaryBoostCategory.PROFIT, ...)` instead of
    `SPEED`) — four independent daily slots, each its own 24-hour
    cooldown from its own watch, tracked in its own
    `GameState.incomeBoostAdWatchTimestamps` ledger so it can never
    consume or interfere with the Speed reward's slots. New
    `RewardedPlacement.AD_BOOST_INCOME` (`ads/AdManager.kt`, real ad unit
    id `ca-app-pub-1913393601233746/9610995150`) and
    `GameViewModel.watchAdForIncomeBoost`/`incomeBoostAdMessage`/
    `dismissIncomeBoostAdMessage`, same shape as the Speed equivalents.
    **Both ad-watch rewards were pulled out of the Shop entirely in
    v0.34.0** — the Speed one briefly lived in the Shop's Temporary tab
    (v0.29.0–v0.33.x, see that tab's own history in `ShopContent.kt`'s
    class doc) but per explicit request both now live exclusively in
    `QuickAdBoostButton`'s popup, so there's exactly one place to find
    either instead of splitting them between the main screen and the
    Shop; `ShopContent`/`TemporaryBoostsTab` lost the
    `speedBoostAd*`/`onWatchSpeedBoostAd`/`onDismissSpeedBoostAdMessage`
    params entirely (Income was never added to the Shop at all). Room
    bumped to **database version 13** for one new
    `incomeBoostAdWatchTimestampsJson` column, and the Supabase
    `GameStateDto` got a matching `income_boost_ad_watch_timestamps_epoch_millis`
    field with an empty-list default for older cloud saves. Verified
    live on-device: the popup correctly showed Speed already on cooldown
    from earlier testing (0/4, "In 18h...") alongside a fresh Income
    option (4/4, "Watch"); watching Income played the real test ad,
    granted "2x Income active for 2h!", dropped the Income slot count to
    3/4, and — confirming the boost actually reaches gameplay — Kobold
    Warren's displayed income doubled (50 gp → 100 gp) on the main
    screen immediately after. Separately confirmed the Shop's Temporary
    tab no longer shows any ad-watch section at all, going straight from
    the "Active" boosts card to the buy list.
  - **Universal Steward (v0.43.0)** — a special account-wide Steward,
    earned once by watching `UNIVERSAL_STEWARD_AD_THRESHOLD` (100)
    rewarded ads *in total, across every placement* (Welcome Back's
    double, the Shop's Platinum ad, and both Speed/Income ad-boosts —
    not any one of them specifically). Built from an explicit
    back-and-forth design pass rather than guessed: confirmed it doesn't
    have its own multiplier or upgrade tiers ("it doesn't level"), that
    it persists through a Level Up (unlike a per-lair Steward), that it
    only takes effect on a lair once at least one unit is actually
    owned (it doesn't retroactively claim anything), and that the
    Stewards screen should list it first and show every owned lair as
    already staffed once it's active, with no redundant per-lair Hire
    button.
    `domain/model/UniversalSteward.kt` is the whole domain layer for
    this: `GameState.totalAdsWatched: Int` (only ever grows, carried
    over explicitly in `GameEngine.performLevelUp()` alongside
    `selectedAvatarId` — a one-time account milestone, not run
    progress), `GameState.hasUniversalSteward()` (`totalAdsWatched >=
    100`, no separate persisted flag needed since it's a pure
    derivation), `GameState.isLairManaged(owned)` (`owned.hasSteward ||
    (owned.count > 0 && hasUniversalSteward())` — the one check every
    "is this lair managed" call site now uses instead of reading
    `OwnedLair.hasSteward` alone), and `adsWatchedTowardUniversalSteward()`
    (clamped to 100, so the progress UI reads "100 / 100" forever after
    rather than an ever-climbing raw count).
    `GameEngine.recordAdWatched()` increments `totalAdsWatched`
    unconditionally and returns true only on the exact watch that
    crosses the threshold — called once from all four of
    `GameViewModel`'s `onRewardEarned` callbacks (via a private
    `recordAdWatched()` wrapper there that also flips
    `universalStewardUnlocked` on that true), deliberately *not* folded
    into each reward's own `grantXxxAdReward` method, since those can
    return false on a rare cooldown-race edge case even though the
    player genuinely watched the ad — the watch should still count
    either way. `isLairManaged` replaced every direct `owned.hasSteward`
    check across the engine (`computeLairProgress`, `advanceLair`,
    `grantInstantProduction`, `startLairLoad`) and the UI
    (`GameScreen`'s gold-per-second sum, `LairRow`'s
    `canStartLoad`/`isBright`, threaded down into `LairCard` as a new
    `isManaged` param) — `hireSteward` additionally refuses outright
    once `hasUniversalSteward()` is true, so there's no way to waste
    real gold on a redundant per-lair hire, and `stewardOpportunities()`
    (`GameStateExtensions.kt`, the "new feature" badge's source) returns
    empty once it's active, since there's no hiring opportunity left to
    flag.
    `ui/game/UniversalStewardRewardDialog.kt` is the one-time unlock
    pop-up — same parchment-chrome pattern as `LevelUpRewardDialog`/
    `MilestoneReachedDialog`, `open_chest` art (no bespoke art yet),
    rendered by `GameScreen` off `GameViewModel.universalStewardUnlocked`
    exactly like every other one-shot reward flag. `StewardsContent.kt`
    gained a `UniversalStewardCard` — always the first thing in the
    list, before even the `IntroCard` — showing a progress bar and "X /
    100 ads watched" before unlock, or a plain "Active" badge after;
    every per-lair `StewardRow` now takes `hasUniversalSteward` and
    shows the same "Steward Hired" text (no Hire button) once it's true,
    regardless of that lair's own `hasSteward`. `SettingsContent.kt`'s
    Account tab gained a one-line `UniversalStewardStatusLine` next to
    the version footer — a quick glance at the same progress, per an
    explicit "show it in both places" answer, since the real progress
    card with its own fill bar already lives on the Stewards screen.
    Persistence: Room bumped to **database version 16** for one new
    `totalAdsWatched` column (plain `Int`, no JSON encoding needed), and
    the Supabase `GameStateDto` got a matching `total_ads_watched` field
    with a `= 0` default for older cloud saves. Unit-tested in
    `UniversalStewardTest.kt` (the pure `hasUniversalSteward`/
    `isLairManaged`/`adsWatchedTowardUniversalSteward` functions,
    including the "does nothing for a lair with zero units owned" case)
    and `GameEngineTest.kt` (the threshold-crossing return value of
    `recordAdWatched`, an unstewarded lair auto-collecting once earned,
    `hireSteward`/`startLairLoad` correctly refusing once it's active,
    and `performLevelUp` carrying `totalAdsWatched` over). **Verified
    live on-device**, watching four real test ads in sequence (Welcome
    Back double, the Shop's Platinum ad, then Speed from
    `QuickAdBoostButton`) from a forced `totalAdsWatched = 97` starting
    point: the Stewards screen correctly showed "98 / 100" after the
    first extra watch; the 100th watch (the Speed one) correctly fired
    `UniversalStewardRewardDialog` the instant its ad closed; afterward
    the Stewards screen showed the card as "Active" with every owned
    lair reading "Steward Hired," and Settings showed "Universal
    Steward: Active" next to the version footer.
  - **Steward Efficiency (v0.44.0; moved onto the Stewards screen in
    v0.44.1)** — a per-lair Gold upgrade line
    (`domain/model/StewardEfficiency.kt`) that discounts a lair's own
    future unit costs instead of boosting income/speed. Built from an explicit design pass
    before any code: the one hard constraint flagged up front was that a
    cost *discount* can never behave like Profit/Speed's unbounded
    compounding multiplier — nothing can push a lair's cost toward 0%
    without breaking the whole exponential cost curve this game's economy
    runs on — so this is deliberately a small, fixed 10-tier ladder, not
    an open-ended one, hard-capped at 99% off. Confirmed answers that
    shaped it: each tier *sets* the discount outright rather than adding
    to the one before it (10%, 20%, 30%, ..., 90%, 99% — tier 5 is
    "50% off," not "10+20+30+40+50"), it's Gold-funded and resets on a
    Level Up (same as Profit/Speed), and — the one piece that took an
    extra round to pin down — it requires that lair's own *real*
    `OwnedLair.hasSteward` to already be true, deliberately **not**
    satisfied by the account-wide Universal Steward
    (`domain/model/UniversalSteward.kt`) even though that also makes the
    lair auto-collect; this is an explicit choice made with the
    Universal Steward's "no more per-lair Steward costs" framing already
    in view, not an oversight to reconcile later.
    `StewardEfficiency.DISCOUNT_PERCENTAGES` is the flat list of 10
    tier-outright-discounts; `costMultiplier(level)` turns a level into
    the `1 - discount%` factor `CreatureLair.costForNextUnit`/
    `costForUnits`/`maxAffordableUnits` now all take as a new
    `costMultiplier` parameter (default 1.0, i.e. every existing call
    site is unaffected until it explicitly opts in) — the one place a
    discount actually multiplies in, with every other cost-reading call
    site (`GameEngine.purchaseLairs`, `BuyQuantity.resolve`'s `MAX` case,
    `LairCard`'s claim-cost preview, `estimatedNetWorth`'s "what it'd
    cost to reclaim everything" merge heuristic) just threading
    `StewardEfficiency.costMultiplier(owned.stewardEfficiencyLevel)`
    through. Tier costs are deliberately steep — "these need to be large
    costs," per explicit instruction — `10,000 * lair.baseCostGp` for
    tier 1, ×3 per tier after that within the same phase (see the
    v0.44.2 phase-jump rework below for tiers 6-10 specifically),
    reflecting how much more
    powerful a cost discount is than a Profit/Speed percentage point;
    first-pass placeholder, not playtested, same as everywhere else in
    the economy. **Steepened further in v0.44.2**, per explicit feedback
    ("i feel like the steward upgrade cost should be huge gaps between.
    So that Lvl 10 (99%) would actually require multiple Level Ups to
    afford") — the original flat `×3` per tier all the way to tier 10
    made 99% off trivially affordable in a single sitting even at
    realistic late-game gold totals (verified: 99% off would have cost a
    Kobold Warren well under 1B gp against a save already holding
    ~940B). Rather than invent a new cost shape, this reuses the same
    `UpgradePhases`/`upgradeTierCost` "phase jump" utility
    (`domain/model/UpgradeTiers.kt`) that `GpUpgrades`/`GemUpgrades`
    already use: `StewardEfficiency.PHASES = UpgradePhases(beginningTiers
    = 5, midTiers = 3, endTiers = 2)` splits the existing 10 tiers into
    three phases (1-5 / 6-8 / 9-10) with the original `costGrowthRate =
    3.0` still compounding smoothly across the whole line, but a
    `PHASE_JUMP_MULTIPLIER` of **10,000** layered on top once per phase
    crossed (squared entering the 9-10 phase, since it's applied once
    per phase boundary passed). Tiers 1-5 (10%-50% off) are completely
    unchanged in cost from v0.44.0/v0.44.1 — still reachable within a
    normal session, matching what was already verified live. Tiers 6-8
    (60%-80% off) jump roughly 10,000x above where the old flat curve
    would have put them; tiers 9-10 (90%/99% off) jump a further
    10,000x on top of that (100,000,000x total above the old curve at
    that point) — for Kobold Warren specifically, tier 9 costs
    ≈24.5 quadrillion gp and tier 10 ≈73.6 quadrillion gp (only 3x apart
    from each other, same as any two tiers in the same phase), vs. the
    old curve's tier 10 of well under 1 billion gp. That's several
    orders of magnitude past what even a heavily-progressed single run
    is likely to earn, by design — reaching 99% off is meant to require
    the kind of compounding income (Gem batches, Everything upgrades,
    permanent Platinum boosts) that only building up across several
    Level Ups provides, not something a single long session can grind
    out. Every pricier lair's own tiers scale the same way off its own
    `baseCostGp`, so the wall is proportionally just as steep at every
    tier of the game, not just the cheapest lair. Unit-tested: existing
    `StewardEfficiencyTest`/`GameEngineTest` cases all read costs via
    `StewardEfficiency.costForTier(...)` rather than hardcoded numbers,
    so they kept passing unchanged through this rework with no test
    edits needed — confirmed via `testDebugUnitTest`. First-pass
    tuning, not playtested — the exact multiplier is a judgment call,
    easy to revisit if 10,000x per phase turns out to be too steep or
    not steep enough once real play data exists. **Verified live
    on-device**: from a save holding 1.72T gp, Giant Rat Burrow's Buy
    button was tapped six times in a quick loop, sailing straight
    through tiers 1-6 (10%-60% off) exactly as expected since those
    still cost the same as before this change; the resulting Lv 6/10
    row then showed a *disabled* "Buy — 4.37T gp" button for tier 7 —
    already more than the entire 1.72T balance the run started with,
    and exactly matching the formula's predicted tier-7 cost
    (`60 × 10,000 × 10,000 × 3^6 = 4.374 × 10^12`) — confirming both
    that the phase wall lands exactly at the intended tier 6/7 boundary
    and that the cost math itself is correct, not just directionally
    steep.
    `GameEngine.purchaseStewardEfficiencyUpgrade(lairId)` is
    the purchase method (same atomic afford-check-and-deduct shape as
    `purchaseGpLairUpgrade`), gated on `owned.hasSteward` specifically.
    Levels live on the new `OwnedLair.stewardEfficiencyLevel` — resets on
    a Level Up implicitly, since `GameState.lairs` itself resets to the
    starting map, same as `profitUpgradeLevel`/`speedUpgradeLevel`.
    **v0.44.0 first shipped this row on the Upgrades screen**
    (`UpgradesContent.kt`'s `LairUpgradeCard` gained a third
    `UpgradeLineRow` per lair, with `UpgradeLineRow` itself gaining an
    optional `lockedMessage` param — "Hire a Steward first," replacing the
    buy button entirely — for the pre-Steward state, distinct from "can't
    afford it yet"). **Moved onto the Stewards screen instead in v0.44.1**,
    per explicit immediate follow-up request ("i want to move the stewards
    upgrades from the upgrades screen to the steward screen, so they can
    be updated inline with the stewards themselves") — `UpgradesContent.kt`
    lost that third row/param entirely, reverting `LairUpgradeCard`/
    `UpgradeLineRow` to their pre-0.44.0 shape (Profit + Speed only).
    `StewardsContent.kt`'s `StewardRow` (one per owned lair) now renders a
    private `StewardEfficiencyRow` (same fields `UpgradeLineRow` used to
    show — level/max, current discount%, next-tier cost, Buy button)
    directly below that lair's own Hire/"Steward Hired" status, but only
    once `owned.hasSteward` is actually true — a lair without its own real
    Steward hired shows no Steward Efficiency row at all here (there's no
    separate "locked" message needed anymore, since the row simply isn't
    rendered pre-Steward, unlike the old Upgrades-screen placement which
    always showed the line with a locked message). `StewardsContent`
    gained one new `onBuyStewardEfficiency: (String) -> Unit` param,
    wired in `MainActivity` to the same
    `gameViewModel::purchaseStewardEfficiencyUpgrade` the Upgrades screen
    used to call — no `GameEngine`/`GameViewModel`/domain-layer change at
    all for this move, confirming the value of this app's clean
    UI/domain split: only `UpgradesContent.kt`, `StewardsContent.kt`, and
    `MainActivity.kt`'s two call sites needed touching, and every existing
    `GameEngineTest`/`StewardEfficiencyTest` kept passing unchanged since
    they test the domain/engine layer, not the UI that moved. **Verified
    live on-device after the move**: the Stewards screen now shows, for
    every Steward-hired lair, its "Steward Hired" badge followed
    immediately by "Steward Efficiency — Lv X/10" and a Buy button;
    buying one (Kobold Warren) moved it from Lv 1/10 (10% off, cost
    112.14K gp) to Lv 2/10 (20% off, next cost 336.42K gp — exactly `3×`
    the prior tier's cost, matching the formula) with the deduction
    reflected immediately in the header's gold total; the Upgrades
    screen's Gold tab was separately confirmed to show only Profit/Speed
    per lair now, with no trace of Steward Efficiency there. Persistence: Room bumped to **database version 17**
    for one new `OwnedLairEntity.stewardEfficiencyLevel` column (plain
    `Int`), and the Supabase `OwnedLairDto` got a matching
    `steward_efficiency_level` field with a `= 0` default for older cloud
    saves. Unit-tested in `StewardEfficiencyTest.kt` (the discount-table
    math, the 99%-cap-never-free guarantee, and that the multiplier
    actually reduces `costForNextUnit`) and `GameEngineTest.kt` (the
    Steward gate rejecting an owned-but-unstewarded lair, rejecting it
    even with the Universal Steward active, a successful purchase both
    deducting gold and discounting the very next real unit purchase,
    insufficient-gold and past-max-tier rejections, and the level
    resetting on `performLevelUp`). **Verified live on-device**: Kobold
    Warren's Steward Efficiency row correctly showed a buy button (its
    real Steward was already hired from earlier testing) at "37.38K gp"
    (`10,000 × 3.738` — matches the formula exactly); buying it moved the
    row to "Lv 1/10, 10% off this lair's own cost" and the next tier's
    cost to "112.14K gp" (`37,380 × 3`, also exact); back on the main
    screen, Kobold Warren's own displayed next-unit cost dropped from
    2.44B gp to 2.20B gp — precisely a 10% reduction on the real,
    already-escalated cost at that ownership count, confirming the
    discount reaches the actual purchase path, not just the Upgrades
    screen's preview.
  - **"New feature" notification badge (v0.32.0, "new" redefined by
    availability in v0.33.0)** — a small ornate gold star
    (`new_notification.png`) that floats over `FloatingMenu`'s chest
    toggle whenever there's something new/unviewed to look at, and over
    the specific menu plank(s) responsible once the menu is expanded.
    "Seen" state is persisted (Room + Supabase), not session-only, so a
    dismissed badge stays dismissed across restarts.
    **What counts as "new" (v0.33.0, explicit correction to v0.32.0's
    launch design): availability, not visibility.** v0.32.0 originally
    gated the Stewards badge on ownership alone (owned + Steward-less)
    and skipped Upgrades entirely (every lair's upgrade lines are always
    visible regardless of ownership, so "newly visible" never applied
    there). The real intent, given directly: a Steward or upgrade only
    counts as "new" once it's actually *affordable* — an owned
    Steward-less lair sitting unaffordable for hours shouldn't light up
    the badge the instant it's claimed, and Upgrades gets the exact same
    treatment now that affordability (not ownership) is the trigger,
    since every line being always-visible stops being a blocker once
    visibility isn't what's being checked.
    Two parallel seen-sets on `GameState`
    (`domain/model/GameState.kt`): `seenStewardOpportunities: Set<String>`
    (lair ids) and `seenUpgradeOpportunities: Set<String>` (upgrade line
    ids — `"<lairId>:profit"`/`"<lairId>:speed"` for the 28 per-lair
    lines, `"everything:profit"`/`"everything:speed"`, or
    `"gem_efficiency"`). `domain/model/GameStateExtensions.kt` has two
    parallel triples of pure functions, one per badge:
    `stewardOpportunities()`/`upgradeOpportunities()` (private —
    everything currently affordable and not yet maxed: for Stewards,
    `goldPieces >= catalog lair's stewardCostGp`; for Upgrades, a loop
    over every lair's Profit/Speed line plus the two Everything lines and
    Gem Efficiency, each checked against its own `GpUpgrades`/
    `GemUpgrades` cost-for-next-tier function), `unseenStewardOpportunities()`/
    `unseenUpgradeOpportunities()` plus `hasUnseenStewardOpportunity()`/
    `hasUnseenUpgradeOpportunity()` (that set minus the matching
    `seen*Opportunities` field — what actually drives each badge), and
    `withStewardOpportunitiesSeen()`/`withUpgradeOpportunitiesSeen()`
    (folds every *currently* affordable item into the matching seen-set,
    called once when the player opens that section —
    `GameEngine.markStewardOpportunitiesSeen()`/
    `markUpgradeOpportunitiesSeen()`, `GameViewModel` thin wrappers of
    the same names — via a `LaunchedEffect(openSection)` in
    `MainActivity`, the same pattern already used there for
    `ensureBillingConnected()`). Since the seen-set is a plain "once
    marked, stays marked" set rather than re-derived every check, an
    opportunity that becomes unaffordable again (gold spent elsewhere)
    and later becomes affordable again does *not* re-flag — matches
    "dismissed once viewed," not "dismissed until it flickers." Neither
    set is carried over in `GameEngine.performLevelUp()` — unlike
    device/grind state (the ad-watch cooldowns), both reset alongside
    `GameState.lairs`/`goldPieces`/`gems` themselves, since a fresh run's
    opportunities (and what's affordable) are genuinely new again — a
    fresh Gem batch minted off `lifetimeGoldEarned` immediately re-flags
    Gem Efficiency as new in the same reset, which is correct, not a
    residual bug (see `GameEngineTest`'s Level Up test for this exact
    case). `FloatingMenu`'s `itemsWithNewBadge: Set<String>` param is
    unchanged in shape from v0.32.0 — `MainActivity` just unions in a
    second flag now (`buildSet { if (...) add("Stewards"); if (...)
    add("Upgrades") }`). Persistence: Room bumped to **database version
    12** for one new `seenUpgradeOpportunitiesJson` column
    (`GameStateEntity`, same JSON-encode-a-list-into-one-column pattern
    as `seenStewardOpportunitiesJson`/`speedBoostAdWatchTimestampsJson`),
    and the Supabase `GameStateDto` got a matching
    `seen_upgrade_opportunities` field with an empty-list default for
    older cloud saves. Verified live on-device (via a direct Room DB
    edit to set a controlled gold amount and clear both seen-sets, since
    this save's own continuous Steward-managed production made
    organically holding gold at a precise unaffordable level impractical
    to test through the UI alone): claiming a lair whose Steward cost
    was already covered by cash on hand (Owlbear Roost, 10B gp Steward
    cost against ~86B gp held) immediately lit up both the chest and,
    once expanded, both the "Stewards" *and* "Upgrades" planks
    simultaneously (Upgrades lighting up purely from gold crossing
    various lines' next-tier costs, unrelated to the Owlbear claim
    itself); opening each section cleared exactly that section's badge
    independently, and the chest's aggregate badge only fully cleared
    once both had been viewed.
  - **`BillingManager` / real "Buy Platinum Pieces" (v0.27.0, price/PP
    curve revised twice since — v0.27.1, then v0.27.2)**
    (`billing/BillingManager.kt`) — Google Play Billing, replacing the
    disabled "Soon" placeholder with five actual consumable IAP packs
    (`domain/model/PlatinumPurchases.kt`'s `PLATINUM_PURCHASE_OPTIONS`):
    **$0.99→4 pp, $2.99→15 pp, $4.99→30 pp, $6.99→55 pp, $9.99→100 pp.**
    This went through three passes, each driven by explicit feedback:
    - v0.27.0's first pass ($0.99-$49.99, 100-7,000 pp) used a *mild*
      bonus curve (0/10/20/30/40% over the $0.99 tier's linear rate) —
      the guiding constraint at the time was just "the top tier shouldn't
      hand over a year's worth of PP."
    - v0.27.1 pulled the range in to $0.99-$9.99 (100-1,400 pp, same mild
      curve) once that top tier was actually checked against the economy:
      7,000 pp could buy the priciest permanent boost tier
      (`PERMANENT_SPEED_TIERS`'s 10x / `PERMANENT_GEM_TIERS`'s 5x, both
      `basePp = 60.0`, `costGrowthRate = 1.8`) seven repeat copies in one
      sitting (10^7x from that tier alone) — still a "one purchase
      trivializes everything" outcome even at the smaller scale.
    - **v0.27.2 is a deliberate, much steeper devaluation of the currency
      itself**, per explicit instruction that 100 pp for $0.99 "devalued
      the worth of the currency" — the $0.99 tier is now a stingy *teaser*
      (4 pp — `PlatinumPurchasesTest`'s `the entry tier is a deliberately
      stingy teaser...` test confirms it can't even afford the cheapest
      permanent boost tier's first copy, `basePp = 5.0`), while $9.99 tops
      out at 100 pp. Pp-per-dollar now climbs a *real* amount top to
      bottom (~4/$ to ~10/$, ~2.5x) rather than the earlier mild curve —
      the point shifted from "smooth value scaling" to "Platinum should
      feel scarce, and the top pack is the one real purchase." At 100 pp,
      the top tier now caps the same steep permanent-boost tier at just
      *one* repeat copy (`PlatinumPurchasesTest`'s `the top tier still
      can't buy more than a couple...` test pins this at ≤2).
    Across all three passes, the real backstop was never the pack amount
    alone — permanent boost tiers' escalating cost (`Boosts.kt`'s
    `costForPermanentBoostPurchase`) already makes any finite PP amount
    unable to max those out. What the shrinking pack sizes actually
    guard against is the *flat-cost* consumables (Time Skips, temporary
    boosts), which have no such built-in ceiling and would otherwise let
    a large enough PP stash approximate "infinite" via repeat purchases.
    - **Every product id must exist as a consumable in-app product in the
      Google Play Console** under this app's listing before any of this
      actually works — there is no way to create these from code, the
      same dashboard-only dependency as an AdMob ad unit id. Until they
      exist, `BillingManager.productDetails`/`formattedPrices` simply stay
      empty and every `PlatinumPackRow` Buy button in the Shop stays
      disabled (showing the tier's static `priceUsd` as a fallback label)
      rather than crashing or charging the wrong amount.
    - **Consumable, not a permanent unlock** — `handlePurchase` calls
      `consumeAsync` immediately once a purchase reaches
      `Purchase.PurchaseState.PURCHASED`, which both acknowledges it and
      clears it so the same pack can be bought again later. There's no
      "restore purchases" button because a consumed consumable has
      nothing to restore; `queryExistingPurchases()` (run once per
      connection) exists specifically to catch a purchase that completed
      but never got consumed — e.g. the app was killed mid-flow — so that
      Platinum isn't paid for but never granted.
    - **`connect()` is deliberately lazy, not called from `init`.**
      Measured live on-device: letting Play Billing's connection handshake
      run unconditionally at app launch (mirroring how `AdManager` eagerly
      preloads ads) roughly doubled-to-quadrupled cold-start time — the
      already-slow ~15s from `AdManager`'s own ads-SDK init (see that
      bullet) became 25-75s+ once Billing initialized eagerly too, most of
      it spent on Play Store's own internal handshake (visible in Logcat
      as a burst of `Finsky`/`AppInfoManager` activity). Since buying
      Platinum is a rare, deliberate Shop action rather than something
      that must feel pre-loaded like a rewarded ad, `GameViewModel.ensureBillingConnected()`
      is only called from a `LaunchedEffect(openSection)` in
      `MainActivity`'s `WyrmWhelpApp` the moment the Shop section actually
      opens — confirmed after the fix that subsequent cold starts return
      to roughly the pre-Billing ~25s baseline, with the Shop itself still
      opening instantly (only its Buy buttons wait on the connection,
      exactly like waiting for `formattedPrices` to populate).
    - **`GameEngine.grantPlatinum(amount: Long)`** — flat Platinum credit
      from a completed purchase, same one-line shape as `grantGold`.
      `GameViewModel` collects `BillingManager.purchaseEvents` (a
      `SharedFlow`, not a `StateFlow` — purchase outcomes are one-shot
      events, not durable state, so two identical `Granted` results in a
      row must both be observed rather than deduplicated) in its own
      coroutine (`runPlatinumPurchaseEventLoop`, launched independently of
      the main init-load sequence since a purchase can complete at any
      point in the session) and calls `grantPlatinum` on `Granted`,
      surfacing either outcome as `platinumPurchaseMessage` — same
      one-message-then-dismiss shape as `platinumAdMessage`.
    - **Verified:** compiles against the real Billing Library 7.1.1 API
      (`javap`-inspected against the actual `.aar` to confirm exact
      callback signatures before wiring them up — `queryProductDetailsAsync`
      still takes a plain `List<ProductDetails>` in 7.1.1, not a wrapping
      result object). Live on-device: the Shop's guest-mode gate correctly
      hides the five pack rows for a guest (`isSignedIn` unchanged from
      before); an actual purchase couldn't be completed in this dev
      environment (no products exist yet in a Google Play Console listing
      for this unpublished app, and completing account sign-up to even
      reach the gated rows needs a real email inbox for Supabase's
      verification code — see the Auth section), so the end-to-end
      charge-and-grant path is unverified beyond code review + the
      compile-time API check above. Revisit once real products exist.
  - **No Navigation Compose** — tried it first (type-safe routes,
    `NavController`/`NavHost`), then removed it: every `FloatingMenu` section
    is a card that slides up over the still-mounted game rather than a
    separate screen you navigate to (see `SectionOverlayCard` below), so
    there was nothing left for a nav graph to do with only one real
    destination. Don't re-add it reflexively if a section later needs a
    "real" screen — reconsider whether it should instead just be a bigger
    `SectionOverlayCard` first.
  - **`FloatingMenu`** (`ui/menu/FloatingMenu.kt`) — the app-wide hamburger
    toggle, fixed bottom-center, overlaid *above* `GameScreen` in
    `MainActivity`'s `WyrmWhelpApp`. The toggle is a transparent-background
    `IconButton`, not a Material `FloatingActionButton` — a FAB always draws
    its own solid container/shadow, which showed as a box behind the chest
    art. It shows `closed_chest`/`open_chest` art (swapped based on
    `expanded`) instead of a generic menu glyph. Expands upward into a
    vertical stack of tappable `floatingMenuItems` (a `MenuItem(label,
    imageRes?)` list, `Arrangement.spacedBy(5.dp)` between them) — evoking the
    wooden trail signpost in the background art. An item with `imageRes` set
    renders as that wooden-sign image directly, no extra container (the sign
    art already has its label baked in) — every item has its own sign now
    (Shop/Settings were the last two still on the plain labeled `Surface`
    fallback, wired up to `menu_shop`/`menu_settings` in v0.20.1). Tapping any item collapses the menu and calls
    `onItemSelected(label)`, which `WyrmWhelpApp` uses to open a
    `SectionOverlayCard` — it does not navigate anywhere.
  - **`SectionOverlayCard`** (`ui/common/SectionOverlayCard.kt`) — the
    replacement for a separate "Coming Soon" screen: a card that slides up
    from the bottom to cover 92% of the screen height (rounded top corners,
    scrim behind it, game still visibly mounted/peeking above and dimmed
    underneath), with a close button (shared `CloseButton` composable,
    top-right) plus tap-scrim-to-dismiss and back-button-to-dismiss
    (`BackHandler` in `MainActivity`, only enabled while a section is open).
    Driven by one nullable `openSection: String?` in `WyrmWhelpApp` — non-null
    shows the card with that title. Retains the last non-null title internally
    while animating out so the card doesn't go blank mid-exit. The header
    reuses whichever `floatingMenuItems` entry matches the title (same
    wooden-sign image shown on that item in the menu) and straddles the
    card's top edge like a hanging plaque — the card's own `Surface` is inset
    from the top by half the sign's height (`SIGN_HEADER_HEIGHT / 2`, derived
    from `SIGN_HEADER_WIDTH / SIGN_ASPECT_RATIO` rather than a hardcoded `Dp`
    — see `CloseButton` bullet below for why that matters), and the sign sits
    at the very top of the surrounding `Box` (not inside the
    `Surface`), so its top half reads as outside the card over the scrim and
    its bottom half overlaps the card surface. Anchored top-start (not
    centered) so it sits opposite the `CloseButton` (top-end) rather than
    competing with it for the same space — header left, close right. Every
    section has its own sign now; a future section without art yet would
    fall back to a plain bold title with no overlap/inset.
    **Content padding inside
    the surface must clear the sign's actual overlap
    (`SIGN_HEADER_HEIGHT / 2`) plus its own breathing room, not a flat
    guessed value** — using less renders content partly hidden underneath the
    sign, which shipped once and was caught from a screenshot. The card's own
    background (inside the `Surface`, i.e. below the sign's overlap point) is
    `AppBackground` with `imageRes = R.drawable.woodenwall_1` — a tavern
    interior, distinct from `GameScreen`'s landscape — behind the same 50%-
    white overlay. Takes an optional `content: @Composable ColumnScope.() ->
    Unit` (default `ComingSoonPlaceholder()`, the "Coming soon…" text every
    section showed before any of them had real content) — `WyrmWhelpApp`
    passes `UnlocksContent` for the Unlocks section (see below) and leaves
    every other section on the default. Still branches on `title` (matching
    the string a caller passes, e.g. `"Unlocks"`) rather than anything more
    structured — fine for one real section; revisit if a second one needs
    this.
  - **`AppBackground`** (`ui/common/AppBackground.kt`) — the shared
    background-art-plus-50%-white-overlay treatment, parameterized by
    `imageRes` (defaults to `main_bg`, `GameScreen`'s landscape). Also used by
    `SectionOverlayCard` with `woodenwall_1` — keep it parameterized rather
    than hardcoding a single image if a third surface needs this treatment.
  - **`CloseButton`** (`ui/common/CloseButton.kt`) — the app's standard close
    control: crossed swords in a wooden ring (`x.png`), not a Material icon
    glyph. Use this for every "close this overlay/dialog" affordance going
    forward instead of `Icons.Default.Close`. Takes a `size: Dp` (default
    32.dp) and renders as a plain clickable `Image`, not `IconButton` —
    `IconButton` clips content to its own fixed 40dp touch-target box, which
    silently cropped the button the first time `SectionOverlayCard` sized it
    up to match the header sign. In `SectionOverlayCard` it's sized and
    positioned exactly like the header: same height (`SIGN_HEADER_HEIGHT`),
    anchored to the very top of the surrounding box so it straddles the card
    edge the same way (half over the scrim, half over the card), but at
    top-end (opposite corner from the header, which is top-start) so the two
    sit side by side — header left, close right — rather than needing an
    offset to keep clear of each other. An offset was briefly needed when the
    header was still centered and the sign art had a padded (1672x941) export
    inflating `SIGN_HEADER_HEIGHT` (and this button) well past the sign's
    actual size; once the art was recropped to its true bounds (see Assets
    section) and the header moved to top-start, neither hack was needed
    anymore.
- **`HelpSocialContent`** (`ui/helpsocial/HelpSocialContent.kt`, v0.31.0) —
    the "Help & Social" menu section's real content, replacing the
    `ComingSoonPlaceholder` it showed before this. One row per real,
    live account: Facebook, Instagram, X, TikTok, Whatnot, the game's own
    website, and a support email address — all seven in one `SOCIAL_LINKS`
    list (`SocialLink(platform, label, handle, url)`) so adding an eighth
    later is one line. Same pure-display-plus-callback pattern as
    `StewardsContent`/`ShopContent`: takes one `onOpenLink: (String) ->
    Unit` and calls it with the row's raw URL (an `https://` link, or
    `mailto:support@wyrmandwhelp.com` for the email row) — it holds no
    `Context` itself. `MainActivity`'s new `"Help & Social" ->` branch is
    what actually fires the `Intent` (`ACTION_VIEW` on the URL, wrapped in
    a try/catch for `ActivityNotFoundException` that just logs and
    no-ops rather than crashing, matching this project's established
    degrade-gracefully-on-external-failures convention) — `ACTION_VIEW`
    on an `http(s)://`/`mailto:` URI needs no manifest `<queries>` entry,
    since both are on Android's package-visibility allowlist for implicit
    intents.
    Each row's `SocialIcon` is hand-drawn via `Canvas` (this project's
    stated style, "Canvas for animation, no sprite pack" — no new
    drawable asset was added for this) rather than a generic link glyph:
    Facebook/X/Whatnot render as bold single-letter monograms on the
    platform's real brand color, which is literally accurate for two of
    the three (X's current logo really is just a stylized "X"; Facebook's
    really is a lowercase "f" in a circle) rather than an approximation.
    Instagram and TikTok get real hand-drawn `Path` glyphs instead — a
    camera outline with a lens circle and shutter dot on the real
    Instagram gradient, and a musical-note shape with the signature
    cyan/magenta offset shadow on a black disc for TikTok — since both are
    recognized by shape more than by any single letter. Website/Email use
    the app's own `FantasyPalette` wood/gold tones instead of a brand
    color (a carved globe, a carved envelope), since they're first-party
    links, not other platforms. Verified live on-device: tapping Instagram
    opened Chrome straight to the real, live `instagram.com/wyrmandwhelp`
    profile; tapping Email Us correctly launched Gmail as the registered
    `mailto:` handler (its own first-run account setup blocked following
    the compose screen further, unrelated to this app).
- **Domain:** `GameEngine` — core tick loop, income calculation, offline-earnings
  math. `@Singleton` via Hilt. `purchaseLairs(lairId, quantity)` buys several
  units atomically in one `_state.update` (either the full
  `CreatureLair.costForUnits` bulk cost is affordable and all of them are
  bought, or none are — never a partial buy); `purchaseLair(lairId)` is now a
  one-line wrapper around it (`purchaseLairs(lairId, 1) > 0`) kept for the
  existing call sites/tests. `costForUnits`/`maxAffordableUnits` on
  `CreatureLair` are closed-form (geometric-series sum and its inverse), not
  loops — a loop for `maxAffordableUnits` could need an unbounded number of
  iterations for a slow-growth lair once gold reaches the kind of totals
  `GoldFormat`'s letter suffixes exist for.
  - **Tap-to-start gold collection (v0.20.0)** — `advanceLair` (called every
    tick, per lair) branches on `OwnedLair.hasSteward`: a Steward-managed
    lair still loops/carries over progress across multiple completed cycles
    exactly as before; an unmanaged lair does nothing at all unless
    `owned.isLoading` is true, and even then completes **at most one** cycle
    per call ("the player tapped once, they get one load") rather than
    looping through however many cycles `deltaSeconds` covers. `startLairLoad(lairId)`
    (replacing the old `plunderLair`) is what a tap calls — it flips
    `isLoading = true` and resets `cycleProgressSeconds`, returning `false`
    (a no-op) if the lair isn't owned, has a Steward, or is already
    `isLoading`. On completion, gold is credited automatically,
    `isLoading` resets to `false`, and `OwnedLair.completedLoads` increments
    (unless `effectiveProductionSeconds < MIN_CONFETTI_PRODUCTION_SECONDS`,
    a 10ms floor below which the coin-burst effect would just read as a
    flicker — gold is still credited either way, only the counter that
    triggers the burst is skipped; reachable only after ~84+ stacked Speed
    Boost levels). Time Skip (`purchaseTimeSkip`) is deliberately **not**
    routed through `advance`/`advanceLair` — it calls a separate private
    `grantInstantProduction(state, seconds)` that credits every owned lair
    uniformly regardless of `isLoading`, since the Shop's own copy promises
    "instantly grants X of production from every owned lair"; it doesn't
    touch an unmanaged lair's `isLoading`/`cycleProgressSeconds`, so buying a
    Time Skip is a bonus layered on top of the tap cycle, never a substitute
    for tapping. `applyOfflineEarnings` inherits this same gating for free
    (it calls `advance` under the hood) — an unmanaged lair that was idle
    when the app closed earns nothing while away; one that was mid-cycle
    keeps advancing and can complete during the offline window.
  - **Milestones** (`domain/model/Milestone.kt`) — `MILESTONE_STEPS` is the
    shared ownership-count ladder (25/50/100/200/300/400 each ×2, then 500
    ×4, 1,000 ×5, 5,000 ×6, 10,000 ×7), applied two ways, both compounding
    every rung reached *of the same [MilestoneType]* into one running
    multiplier (`milestoneMultiplierFor(unitsOwned, type)`). **Confirmed
    design (v0.21.1): the first six rungs (25 through 400) are
    [MilestoneType.SPEED] — they shrink cycle time; 500 and up are
    [MilestoneType.INCOME] — they boost gold per cycle instead.** The two
    types compound independently (a Speed rung never affects income, an
    Income rung never affects speed) rather than one combined multiplier
    like the original (pre-0.21.1) design:
    - **Individual** — `CreatureLair.individualSpeedMilestoneMultiplier(unitsOwned)`
      / `individualIncomeMilestoneMultiplier(unitsOwned)`, keyed on that one
      lair's own owned count (400 Kobold Warrens is its own 64x *Speed*,
      independent of every other lair; reaching 500 adds a *separate* 4x
      Income on top, not compounded into the same number).
    - **"Everything"** — `GameState.globalSpeedMilestoneMultiplier(catalog)`
      / `globalIncomeMilestoneMultiplier(catalog)` (`domain/model/GameStateExtensions.kt`),
      keyed on the *lowest* owned count across every lair in `catalog` —
      every lair has to reach a rung before the bonus for it applies to all
      of them, not just whichever lair is furthest ahead.
    The Income multipliers feed into `CreatureLair.incomePerCycle(unitsOwned,
    globalIncomeMultiplier = 1.0)` (`baseIncomeGp * unitsOwned *
    individualIncomeMilestoneMultiplier(unitsOwned) * globalIncomeMultiplier`);
    the Speed multipliers feed into `CreatureLair.effectiveProductionSeconds(unitsOwned,
    speedBoostMultiplier, globalSpeedMilestoneMultiplier)` (dividing
    `baseProductionSeconds`, same as the Platinum-bought Speed Boost) — note
    `effectiveProductionSeconds` needs `unitsOwned` now, unlike before
    v0.21.1. Every caller that credits or previews income/speed
    (`GameEngine.advance`/`advanceLair`/`grantInstantProduction`, `LairCard`'s
    "gp / cycle time" text and progress-bar fill, `GameScreen`'s gold-per-second
    sum) computes both global multipliers once per tick/recomposition and
    threads them through separately; callers that don't pass them (existing
    tests, mainly) get the no-bonus default of 1.0 for each.
    `nextMilestoneThreshold(unitsOwned)` (smallest rung still ahead of
    either type, or null past 10,000) is what `BuyQuantity.NEXT` targets —
    unaffected by the type split, since it's just "how far to the next rung
    of any kind."
  - **Milestone-reached pop-up (v0.21.0, Speed/Income split in v0.21.1)** —
    `GameStateExtensions.milestonesCrossed(lairId, previousCount, catalog)`
    is the pure, unit-tested detection function: given the *post*-purchase
    `GameState` plus the owned count *before* the purchase, it returns every
    `MilestoneStep` rung actually crossed as a `MilestoneAnnouncement(lairName,
    threshold, multiplier, isGlobal, type)` — this lair's own individual
    rungs first (a bulk buy can jump straight past several, e.g. 10→100
    reports 25, 50, *and* 100), then any "Everything" global rung this
    purchase newly unlocked by raising the catalog-wide minimum (`lairName =
    "Everything"`, matching `UnlocksContent`'s own grouping label).
    `GameViewModel.claimLair` snapshots the owned count before calling
    `purchaseLairs`, then diffs against the post-purchase state to build the
    list and hands it to `enqueueMilestoneAnnouncements` — multiple rungs
    from one purchase queue up (`pendingMilestoneAnnouncements: ArrayDeque`)
    and surface one at a time via `milestoneAnnouncement: StateFlow<MilestoneAnnouncement?>`,
    each drained by `dismissMilestoneAnnouncement()`. `MilestoneReachedDialog.kt`
    (`ui/game/`) is the pop-up itself — deliberately built as a near-copy of
    `WelcomeBackDialog`'s chrome (plain `Dialog`, parchment gradient, carved
    wood border, `open_chest` art for "a reward was just opened",
    `GlowingGoldText` as the focal number, a `WoodenButton` to dismiss) rather
    than inventing a second dialog look — shows "Kobold Warren — x100" / "2x
    Speed" / "for this lair" (or "Everything" / "for every lair" for a global
    rung), or "4x Income" for a rung ≥500. **The label is read from
    `MilestoneAnnouncement.type`, never assumed** — the original v0.21.0 copy
    hardcoded "x Speed" for every rung, which was wrong for 500+ once the
    Speed/Income split landed in v0.21.1; matches `UnlocksContent`'s own
    per-rung labeling (see below), so keep the two in sync if either wording
    changes. `GameScreen` collects `milestoneAnnouncement` and renders the
    dialog exactly like `welcomeBackEarnings`. Only triggers off `claimLair`
    (purchases) — milestones are ownership-count-based, not tick-based, so
    nothing needs to watch the tick loop for this.
  - **`UnlocksContent`** (`ui/unlocks/UnlocksContent.kt`) — the Unlocks
    section's real content (see `SectionOverlayCard` above), redesigned in
    0.19.1: grouped by lair — a "Kobold Warren" header followed by a
    4-cards-per-row grid of every milestone *rung* that lair has actually
    reached, not a compressed single-bonus summary — owning 50 Kobold
    Warrens shows two cards under one "Kobold Warren" header ("x25"/"2x
    Speed" and "x50"/"2x Speed"), and the "Everything" ladder gets its own
    group the same way. Each card is reduced to its two load-bearing
    numbers (`"x${rung.threshold}"` / `"${rung.multiplier}x ${if (rung.type
    == MilestoneType.SPEED) "Speed" else "Income"}"`, v0.21.1 — previously
    hardcoded "Speed" for every rung, wrong once rungs ≥500 became Income)
    instead of a sentence. `UnlockCardRow` chunks each group's rungs into
    rows of `CARDS_PER_ROW` (4) via `List.chunked`, padding a short final
    row with invisible `Modifier.weight(1f)` spacers rather than letting
    its real cards stretch wider — every card is the same size regardless
    of row length. Built by filtering `MILESTONE_STEPS` against each
    lair's owned count (and against the global "Everything" minimum) —
    nothing here is a preview of what's ahead, a rung simply doesn't
    appear until it's actually been crossed, and a lair with zero rungs
    reached doesn't get a group header at all. A save with nothing
    unlocked at all shows a "No milestones unlocked yet…" placeholder
    instead of an empty screen. Pure display — takes `lairs`/`state`
    passed in by `WyrmWhelpApp` (which already holds the `GameViewModel`
    reference) rather than taking a ViewModel itself.
  - **Upgrades (v0.25.0)** — a permanent Gold/Gem sink layered on top of
    the automatic ownership milestones above, reachable from the
    floating menu between Unlocks and Stewards. Built from a
    confirmed 5-question design pass (tab structure, exact tier counts,
    what Gem upgrades actually affect, Platinum deferred, per-line
    phase jumps) rather than guessed.
    - **`UpgradePhases`/`upgradeTierCost`/`upgradeTotalPercent`**
      (`domain/model/UpgradeTiers.kt`) — the shared beginning/mid/end-game
      shape every upgrade line uses: `UpgradePhases(beginningTiers,
      midTiers, endTiers)` with a `totalTiers`, `phaseOfTier(tier)`
      (1/2/3), and `positionWithinPhase(tier)` (0-indexed within its own
      phase — kept as a tested utility, but **not** used by the cost
      formula; see the gotcha below for why). `upgradeTierCost(tier,
      phases, baseCost, costGrowthRate, phaseJumpMultiplier)` = `baseCost
      * phaseJumpMultiplier^(phase-1) * costGrowthRate^(tier-1)` — the
      `costGrowthRate` exponent is the tier's **absolute** position
      across the whole line, never resetting each phase, with
      `phaseJumpMultiplier` layered on top as a pure additional kicker at
      each boundary. **Gotcha hit building this:** the first-pass
      formula used `positionWithinPhase(tier)` (which *does* reset to 0
      each phase) as that exponent instead — harmless for a short phase,
      but for Gem Efficiency's 67-tier phases at 1.15 growth, phase 1
      alone smoothly compounds past 10,000x before ever hitting the flat
      5x `phaseJumpMultiplier`, so resetting the exponent at the
      boundary made phase 2's *first* tier cheaper than phase 1's *last*
      one — the opposite of the intended "obvious jump." Caught by
      `GemUpgradesTest`'s phase-boundary test failing, not by
      inspection — a lesson to test phase-jump behavior at realistic
      phase lengths, not just short ones, since the bug's effect scales
      with how long a phase runs. `upgradeTotalPercent(level, phases,
      percentPerTierPhase1/2/3)` is unrelated to cost — it's the
      *additive* (not compounding) percentage-point total a line's
      *effect* has accumulated, with the per-tier rate itself increasing
      at each phase boundary; this is where a maxed line's power jump
      actually comes from, since the cost curve's own phase jump only
      makes a line more expensive, not more rewarding.
    - **`GpUpgrades`** (`domain/model/GpUpgrades.kt`) — the Gold tab's 30
      lines, 475 tiers total: 28 per-lair lines (14 lairs × `Profit`/`Speed`
      `UpgradeCategory`, `LAIR_LINE_PHASES` = 5/5/5 = 15 tiers each, base
      cost `lair.baseCostGp * 100`) + 2 "Everything" lines (`Profit` 9/9/10
      = 28 tiers, `Speed` 9/9/9 = 27 tiers, flat 300,000,000 gp base cost)
      — `28*15 + 28 + 27 = 475` exactly, asserted in `GpUpgradesTest`.
      Shared `costGrowthRate = 1.25`, `phaseJumpMultiplier = 8.0`; percent
      per tier is 2%/4%/8% across phases 1/2/3 for both categories.
      `lairProfitMultiplier`/`lairSpeedMultiplier`/`everythingProfitMultiplier`/
      `everythingSpeedMultiplier(level)` each return `1.0 +
      upgradeTotalPercent(...) / 100.0`.
    - **`GemUpgrades`** (`domain/model/GemUpgrades.kt`) — the Gems tab's
      single line, "Gem Efficiency," 200 tiers (67/67/66) — **every** Gem
      upgrade raises the per-Gem income-bonus percentage, per an explicit
      instruction that Gem upgrades should only ever affect that one
      number, nothing else. `BASE_COST_GEMS = 5.0`,
      `costGrowthRate = 1.15`, `phaseJumpMultiplier = 5.0`; per-tier bonus
      (percentage points, additive) is 0.0005%/0.001%/0.002% across
      phases 1/2/3 — small numbers since they stack onto the already
      substantial flat 2%-per-Gem baseline (`LevelUp.kt`'s
      `GEM_INCOME_BONUS_PER_GEM`, made a public `const val` so
      `GemUpgrades`/`gemIncomeMultiplier` can share it). `costForTierGems(tier):
      Long` rounds the fractional Double cost **up** (`ceil`) since Gems
      are a whole-number currency, unlike Gold. `gemIncomeMultiplier(gems,
      gemEfficiencyLevel)` now reads `GEM_INCOME_BONUS_PER_GEM +
      GemUpgrades.bonusPerGem(gemEfficiencyLevel)` as the per-Gem rate —
      spending Gems on this upgrade correctly does **not** shrink the
      bonus from Gems still held; it only raises the rate applied to
      whatever's left.
    - **Platinum tab (v0.26.0)** — read-only, unlike Gold/Gems: every
      permanent boost tier's owned count/combined multiplier and every
      currently-running temporary boost, all actually *bought* in the
      Shop (see the Platinum Upgrades bullet below), not here. Deferred
      at first (v0.25.0, per "dont implement any new platinum upgrades
      right now. Just Gold and Gems") — this tab rendered
      `ComingSoonPlaceholder()` until built.
    - **Everything here resets on Level Up, unlike Boosts** — a lair's
      own `profitUpgradeLevel`/`speedUpgradeLevel` (new fields on
      `OwnedLair`) reset implicitly since `GameState.lairs` itself resets
      to the starting map on Level Up; `GameState.everythingProfitUpgradeLevel`/
      `everythingSpeedUpgradeLevel`/`gemEfficiencyLevel` reset the same
      way `gems` does, by simply not being carried into the fresh
      `GameState` `performLevelUp()` constructs. This was the explicit
      point of the "GP and Gem Upgrades" reset requirement that kicked
      off this session's Level Up redesign — see the Prestige bullet
      under Core game design. Room bumped to **version 8** for these five
      new persisted fields (both `GameStateEntity` and `OwnedLairEntity`);
      the Supabase `GameStateDto`/`OwnedLairDto` got the same five fields,
      each with a `= 0` default for old cloud saves.
    - **`GameEngine`** gained three purchase methods, each atomic
      (afford-check, max-tier-check, and the actual deduction all inside
      one `_state.update`, so a purchase can't partially apply):
      `purchaseGpLairUpgrade(lairId, category)`,
      `purchaseGpEverythingUpgrade(category)`, and
      `purchaseGemEfficiencyUpgrade()` — thin `GameViewModel` wrappers
      forward all three straight through, same shape as
      `claimLair`/`hireSteward`. Every multiplier-consuming call site
      (`advance`/`advanceLair`/`grantInstantProduction`/
      `computeLairProgress` in `GameEngine`, plus `GameScreen`'s
      gold-per-second sum and `LairRow`'s per-card display) now computes
      the two "Everything" multipliers once per tick/recomposition and
      combines them with a lair's own upgrade level before passing the
      result into `CreatureLair.incomePerCycle`'s new
      `upgradeProfitMultiplier` param / `effectiveProductionSeconds`'s new
      `upgradeSpeedMultiplier` param (both default `1.0`, both threaded
      down through `LairRow`→`LairCard` for display, same pattern as
      every other multiplier this game has added incrementally).
    - **`UpgradesContent`** (`ui/upgrades/UpgradesContent.kt`) — a 3-tab
      segmented control (`UpgradeTab.GOLD`/`GEMS`/`PLATINUM`) via a custom
      private `UpgradeTabButton`, **not** the shared `WoodenButton` —
      `WoodenButton`'s `enabled` param gates both its visual state and its
      clickability together, which doesn't work for a tab row where every
      tab (including the currently-selected one, for re-selection) must
      stay clickable regardless of which is active. A shared private
      `UpgradeLineRow` (label, level/maxLevel, effect description, cost
      label, canAfford, onBuy) renders every line in both the Gold and
      Gems tabs so the row shape can't drift between them. `ParchmentCard`/
      `SectionLabel` are duplicated privately in this file rather than
      extracted, matching the established per-file-duplication convention
      already used identically in `ShopContent.kt`/`StewardsContent.kt`/
      `LevelUpContent.kt`. Verified live on an installed device: all three
      tabs render correctly, a real Gold purchase (Kobold Warren Profit)
      deducted the exact displayed cost (373.8 gp) and moved the line from
      Lv 0/15 to Lv 1/15 with a freshly recalculated next-tier cost
      (467.3 gp, matching `costGrowthRate = 1.25`), and a real Gem
      purchase moved Gem Efficiency from Lv 0/200 to Lv 1/200 for the
      displayed 5-Gem cost, with the next cost updating to 6 Gems
      (`ceil(5 * 1.15) = 6`) and the per-Gem bonus display updating from
      +2.0% to +2.1% income.
  - **`StewardsContent`** (`ui/stewards/StewardsContent.kt`) — the Stewards
    section's real content: an intro card explaining what a Steward does,
    then one row per *owned* lair (a lair with zero units doesn't get a row —
    hiring a Steward for nothing owned isn't a real action) showing either a
    "Steward Hired" badge or a `WoodenButton` to hire one for
    `CreatureLair.stewardCostGp`. Forwards hires through `onHireSteward:
    (String) -> Unit` rather than taking a `GameViewModel` itself —
    `WyrmWhelpApp` wires it straight to `gameViewModel::hireSteward`, the
    domain method that already existed (and was already tested) from when
    the button lived on `LairCard`.
  - **`ShopContent`** (`ui/shop/ShopContent.kt`, reorganized into tabs in
    v0.28.0) — the Shop section's real content: a balance card shown
    above the tabs (visible on all of them), then a private `ShopTab`
    enum/`ShopTabRow` driving four tabs — same
    `CutCornerShape`/gold-vs-wood-gradient tab-button look as
    `UpgradesContent.kt`'s `UpgradeTab` row, duplicated privately here per
    this project's per-file-duplication convention rather than shared.
    **"Get PP" is first/default** (previously the Shop opened straight
    into "Permanent Boosts," with earning Platinum buried at the bottom
    of one long scroll — explicit feedback that a player reaching for the
    Shop is usually there to *get* Platinum, not spend it) and holds
    "Earn Platinum" (the real "Watch an Ad") plus, once signed in, "Buy
    Platinum Pieces" (five real Google Play Billing packs as of v0.27.0 —
    see the `BillingManager` bullet below — one `PlatinumPackRow` per
    `PLATINUM_PURCHASE_OPTIONS` tier) right alongside it, so both ways to
    get more pp live on the same tab. "Permanent" holds one
    `PermanentBoostCategoryCard` per category (Speed/Profit/Gem %, each
    stacking its own three tiers — see the Platinum Upgrades bullet
    below); "Temporary" holds an `ActiveTemporaryBoostsCard` live
    countdown when any are running, then one row per
    `TEMPORARY_BOOST_OPTIONS` entry; "Time Skips" holds one row per
    `TIME_SKIP_OPTIONS` entry (six tiers as of v0.26.0, up from two —
    the 24h/168h tiers' title and description read "1 day"/"1 week"
    instead as of v0.28.1, via a private `formatTimeSkipDuration` helper
    that special-cases exactly those two durations and falls back to the
    shared `DurationFormat.format` otherwise — that shared formatter
    itself stayed hours/minutes-only, since it's also used for the
    ad-cooldown and temporary-boost countdown displays, which never reach
    a full day) — each of these three was previously just a
    `SectionLabel`-delimited section in that same one long scroll, now
    split one-to-one into its
    own tab now that the Shop sells five different things.
    `FloatingMenu`'s `"Shop"` entry (its own wooden-sign art as of v0.20.1)
    reaches it. Takes `platinumPieces`, `permanentBoostLevelFor: (PermanentBoostTier)
    -> Int` (a bound `GameState.permanentBoostLevel` reference from the
    caller), `activeTemporaryBoosts: List<Pair<ActiveTemporaryBoost, Duration>>`
    (precomputed via `GameState.activeTemporaryBoostsRemaining()`, same
    "caller computes, view just renders" convention as `platinumAdCooldownRemaining`),
    `platinumPurchasePrices: Map<String, String>` (Play Billing's own live
    formatted price per product id, empty until `BillingManager` resolves
    it — a `PlatinumPackRow`'s Buy button stays disabled and shows the
    tier's static `priceUsd` fallback until its real entry appears here,
    since a purchase can't actually be charged without a live price),
    `platinumPurchaseMessage`, plus `onBuyPermanentBoost`/`onBuyTemporaryBoost`/
    `onBuyTimeSkip`/`onBuyPlatinumPack`/`onDismissPlatinumPurchaseMessage`
    callbacks — `WyrmWhelpApp` wires the callbacks straight to the
    matching `GameViewModel` methods, same pattern as `StewardsContent`'s
    `onHireSteward`. This is the *only* place any permanent/temporary
    boost or Platinum pack is purchased — the Upgrades screen's Platinum
    tab only displays what's already been bought (see above). Verified
    live on-device: all four tabs render and scroll correctly, switching
    tabs preserves the live Platinum balance shown above them, and a real
    permanent-boost purchase on the "Permanent" tab still deducted the
    correct cost and updated the owned count exactly as before the
    reorganization.
  - **Platinum Upgrades (v0.26.0)** (`domain/model/Boosts.kt`) —
    replaced the original Speed Boost/Profit Boost design (a single
    compounding %-per-level line each, like the Gold/Gem upgrade lines)
    entirely, per explicit design: "Permanent speed boosts: 2x, 5x, 10x
    ... Permanent income boost: 1.5x, 2x, 5x ... Permanent gem percentage
    boost: 1.5x, 2x, 5x" plus "Temporary speed boost: 50x (5 mins), 100x
    (5 mins) ... temporary income boost: 15x (10 mins), 25x (5 mins)," all
    "stackable" and permanent through a Level Up (unlike every Gold/Gem
    upgrade — see `GameEngine.performLevelUp`).
    - **Permanent tiers** (`PermanentBoostTier`/`PermanentBoostCategory`,
      catalogs `PERMANENT_SPEED_TIERS`/`PERMANENT_PROFIT_TIERS`/`PERMANENT_GEM_TIERS`) —
      nine named, **repeatedly repurchasable** tiers (three per category),
      each compounding on its own purchase count: buying "5x Speed" three
      times contributes `5^3 = 125x` from that tier alone
      (`GameState.permanentSpeedMultiplier()` folds `tier.multiplier^level`
      across all three Speed tiers, same shape for Profit/Gem %) — the
      explicit "3 5x speeds... stack" requirement. Cost per repeat purchase
      (`costForPermanentBoostPurchase(tier, currentLevel)` =
      `basePp * costGrowthRate^currentLevel`, same closed form the
      original Speed/Profit Boost used) grows steeply per tier
      (`costGrowthRate` 1.5–1.8) specifically because a tier can be bought
      unboundedly many times — there's no max-level cap, only cost, unlike
      every Gold/Gem line. `GameState.permanentBoostLevel(tier)`/
      `withPermanentBoostLevel(tier, level)` read/write the matching one of
      nine flat `Int` fields (`permanentSpeedBoost2xLevel` and its eight
      siblings) via a `when` on `(category, tier.multiplier)` — flat fields
      rather than a map, consistent with this project's existing
      "no Room `TypeConverter`, plain scalar columns" convention.
    - **Temporary tiers** (`TemporaryBoostCategory`/`TemporaryBoostOption`,
      catalog `TEMPORARY_BOOST_OPTIONS`) — a fixed-price, instant-activation
      consumable: buying one immediately appends a
      `GameState.activeTemporaryBoosts` entry (`ActiveTemporaryBoost(category,
      multiplier, expiresAt)`) running for its own duration. **Buying a
      second one of the same category before the first expires stacks
      multiplicatively for their overlap** — the explicit confirmed answer
      ("stack multiplicatively while both run") — which is *why* this is a
      list of independent instances rather than one "level + one expiry"
      pair per category: `List<ActiveTemporaryBoost>.multiplierFor(category,
      now)` folds every not-yet-expired entry's multiplier together.
      `GameEngine.advance` prunes expired entries once per tick (using the
      tick's own `now`, threaded through `tick`/`advance`/`computeLairProgress`/
      `grantInstantProduction`, all of which previously read
      `Instant.now()` implicitly via default params) so the persisted list
      never grows unbounded; `GameState.activeTemporaryBoostsRemaining(now)`
      (drops expired, sorts soonest-first) is what the Shop's live
      countdown and the Upgrades Platinum tab both render — computed by
      the caller (`WyrmWhelpApp`), same "view doesn't touch the clock"
      convention as `platinumAdCooldownRemaining`.
    - **Replacing the old multiplier plumbing** — `GameState.platinumSpeedMultiplier(now)`
      / `platinumProfitMultiplier(now)` (`permanentXMultiplier() * activeTemporaryBoosts.multiplierFor(category,
      now)`) now feed exactly where `speedBoostMultiplier(state.speedBoostLevel)`/
      `profitBoostMultiplier(state.profitBoostLevel)` used to —
      `CreatureLair.incomePerCycle`'s `profitBoostMultiplier` param and
      `effectiveProductionSeconds`'s `speedBoostMultiplier` param are
      unchanged, only what value now computes them changed, so no call-site
      signature changes were needed in `CreatureLair.kt` itself. The
      permanent Gem % tiers are similar but feed a *new* third param on
      `gemIncomeMultiplier(gems, gemEfficiencyLevel, platinumGemPercentMultiplier)`
      — `GameState.permanentGemPercentMultiplier()` multiplies the whole
      per-Gem rate (`GEM_INCOME_BONUS_PER_GEM + GemUpgrades.bonusPerGem(...)`),
      so it keeps applying to whatever the *next* run's fresh Gem batch is
      worth too, unlike the Gem-funded Gem Efficiency upgrade it stacks
      with.
    - **Time Skips expanded** — `TIME_SKIP_OPTIONS` grew from two tiers
      (10 min/1 hour) to six (5 min/2 pp, 30 min/8 pp, 1 hour/15 pp,
      12 hours/100 pp, 24 hours/180 pp, 7 days/1,000 pp) per explicit
      request; no logic change, `purchaseTimeSkip`/`grantInstantProduction`
      already worked generically off the list.
    - **Persistence** — Room bumped to **version 9**: the old
      `speedBoostLevel`/`profitBoostLevel` `GameStateEntity` columns are
      gone, replaced by the same nine flat `Int` columns plus one
      `activeTemporaryBoostsJson: String` column (a small JSON-encoded
      list of `{category, multiplier, expiresAtEpochMillis}` records,
      hand-rolled via `kotlinx.serialization` inside `GameStateMappers.kt`
      rather than a Room `TypeConverter` — consistent with this file's
      existing "no converter machinery" style). The Supabase
      `GameStateDto` mirrors the same nine fields plus a genuinely nested
      `List<ActiveTemporaryBoostDto>` (trivial for a jsonb blob, no
      encoding trick needed there).
    - **Verified live on-device**: seeded 5,000 pp directly into the Room
      DB (via `adb run-as` + a local `sqlite3` checkpoint/edit/push-back,
      since this emulator image has no on-device `sqlite3`) to get past
      the "no way to earn Platinum without a live ad" testing gap. Buying
      the 5x Speed tier deducted the exact 20 pp shown, moved
      "5x — owned 1" with a "contributing 5x" sub-label, and immediately
      shortened Kobold Warren's live cycle time from 150ms to 30ms on the
      main game screen. Buying the 50x Speed temporary boost showed a
      live "50x Speed — 4m left" countdown in both the Shop and the
      Upgrades screen's Platinum tab simultaneously.
  - **Level Up** (`domain/model/LevelUp.kt`) — the prestige mechanic
    described under Core game design below, finally implemented in
    v0.23.0 and reworked twice since. **Gems are deliberately temporary,
    not accumulated (v0.24.0)** — despite the formula being AdVenture
    Capitalist's real Angel Investor formula ported 1:1 (same convention
    as the tier-0–9 lair balance in `CreatureLairCatalog`), this game's
    Gems don't persist the way AdCap's Angels do. Per explicit user
    design intent: a bigger Gem batch means a bigger income *head start*
    for the run right after a Level Up, not a stockpile that grows
    forever — that head start is what will matter once a leaderboard
    exists to compare how fast players ramp up, not how many Gems
    anyone's banked. Concretely, `GameState.gemsEarnedFromLevelUp()` is
    `floor(150 * sqrt(lifetimeGoldEarned / 10^15))`, and
    `GameEngine.performLevelUp()` *replaces* `GameState.gems` with that
    result rather than adding to it. `GameState.lifetimeGoldEarned`
    (every Gold Piece ever earned from production, incremented alongside
    `goldPieces` in `advance`/`grantInstantProduction`/`grantGold` but
    *not* when `goldPieces` is spent, and never reset by a Level Up) is
    what the formula scales off — since it only ever grows, a Level Up
    can never hand back *fewer* Gems than a previous one; leveling up
    twice with no new lifetime earnings in between simply regrants the
    identical batch, replacing itself, rather than compounding into
    something bigger. (An earlier version of this formula, live for
    about a day across v0.23.0–v0.23.4, subtracted a `totalGemsEarned`
    running ledger so repeat Level Ups without new progress would grant
    0 — a deliberate "reach further each time" gate the user later
    decided wasn't wanted once Gems stopped being something worth
    gate-keeping: since the new batch always replaces the old one and
    can never be smaller, an instant repeat Level Up costs the player
    nothing to attempt and gains them nothing either, so the ledger and
    its gate were removed entirely rather than kept unused.)
    **Minimum batch sizes** — a Level Up is still blocked outright
    whenever the batch would be too small to be worth resetting for:
    `MIN_GEMS_PER_FIRST_LEVEL_UP` (50) when `GameState.totalLevelUps ==
    0` (an explicit user request — a brand-new save could otherwise
    Level Up for just 1 Gem at ~44 billion lifetime Gold), and the
    smaller `MIN_GEMS_PER_RECURRING_LEVEL_UP` (25) for every Level Up
    after that (also explicit — recurring Level Ups shouldn't be held to
    as high a bar as the first, but also shouldn't trigger for a trickle
    of a few Gems). In practice the recurring minimum rarely binds once
    the first has been cleared, since the batch size is monotonic
    non-decreasing from there — but it stays in place as a floor
    regardless. Whichever minimum applies, clearing it grants the entire
    batch, never capped at the minimum itself.
    `gemIncomeMultiplier(gems: Long)` is a flat +2% income bonus per Gem
    *currently held*, additive rather than compounding (unlike the
    Platinum-bought Profit Boost) — since it reads the temporary `gems`
    balance directly, the bonus is automatically just as temporary,
    with no separate reset logic needed. Both formulas are first-pass
    placeholders, not playtested, same as everywhere else in the
    economy. `CreatureLair.incomePerCycle` takes a fourth
    `gemBonusMultiplier` parameter (default 1.0) alongside the existing
    three — threaded through the same call sites as
    `profitBoostMultiplier` (`GameEngine.advance`/`advanceLair`/
    `grantInstantProduction`, `GameScreen`'s gold-per-second sum,
    `LairRow`/`LairCard`'s income display) rather than folded into one of
    the existing multiplier slots, keeping each bonus source's own name
    intact through the whole call chain.
    `GameEngine.performLevelUp()` does the actual reset: computes
    `gemsEarnedFromLevelUp()` first and does nothing (returns 0) if that's
    0 — a save that hasn't earned enough new lifetime Gold since its last
    Level Up can't reset for nothing — otherwise builds a fresh
    `GameState()` (the same starting shape, Kobold Warren included,
    `GameState()`'s own defaults) with Platinum Pieces, every permanent
    boost tier and active temporary boost (`domain/model/Boosts.kt` —
    v0.26.0 replaced the original `speedBoostLevel`/`profitBoostLevel`
    fields these carried over), `offlineCapHours`, the ad-watch
    cooldown (`lastPlatinumAdWatchedAt`), and — critically —
    `lifetimeGoldEarned` itself explicitly carried over from the pre-reset
    state; `gems` is set directly to the new batch (not `current.gems +
    gemsEarned`). Only the *current run's* gold (Gold Pieces, every owned
    lair, ownership milestones implicitly via the lair reset) and the old
    Gem batch actually reset; the lifetime tally must not. `totalLevelUps`
    increments. The affordability check and the reset happen inside the
    same `_state.update` call (not a separate read-then-write), so two
    rapid taps can't both reset off a stale "can afford" read — same
    pattern as `grantPlatinumAdReward`'s cooldown check.
    `GameViewModel.performLevelUp()` calls it and, only if a batch was
    actually granted, sets `levelUpReward: StateFlow<Long?>` so
    `GameScreen` can pop up `LevelUpRewardDialog` — same one-shot-then-null
    shape as `milestoneAnnouncement`, reusing the `WelcomeBackDialog`/
    `MilestoneReachedDialog` chrome (parchment card, `open_chest` art,
    `GlowingGoldText`) but with the glow recolored to amethyst via two new
    optional `glowBright`/`glowDeep` params on `GlowingGoldText` (default
    to the existing gold tones, so every other call site is unaffected).
    `ui/levelup/LevelUpContent.kt` is the Level Up menu section's real
    content (`MainActivity`'s `"Level Up" ->` branch, same
    pure-display-plus-callback pattern as `ShopContent`/`StewardsContent`):
    an intro card (explicitly describing Gems as temporary, not
    permanent, as of v0.24.0), a Gems-balance card (current Gems plus the
    resulting income-bonus percentage, live-computed, labeled "until your
    next Level Up"), and a "Level Up now" card showing the exact Gem
    batch a Level Up would grant right now
    (`gameState.gemsEarnedFromLevelUp()`, computed live by `WyrmWhelpApp`
    the same way `ShopContent`'s ad-cooldown label is — no polling, it
    just updates as `gameState` ticks) with a `WoodenButton` that disables
    itself once that payout is 0. Tapping the button doesn't reset
    immediately — it opens `LevelUpConfirmDialog`, a private composable
    with its own local `remember`ed show/hide state (not
    `GameViewModel`-driven, unlike `LevelUpRewardDialog`, since it's a
    plain "are you sure" step with no state that needs to survive the
    section closing) showing the exact Gem batch and warning that Gold,
    every lair, and any Gems currently held will all reset, with
    side-by-side Cancel/"Level Up!" `WoodenButton`s — only confirming
    there calls `onLevelUp`.
    **Progress bar toward the next Level Up (v0.35.0)** — added per
    explicit feedback: a player with 0 Gems earnable saw only a disabled
    button and vague "keep earning" text, with no sense of how close they
    actually were — risking that reading as the feature being broken or
    stalled rather than just early. `domain/model/LevelUp.kt` split what
    was inline logic in `gemsEarnedFromLevelUp()` into two new public
    functions the UI also consumes: `rawGemsFromLevelUpFormula()` (the
    formula result *before* the minimum-batch gate — unlike
    `gemsEarnedFromLevelUp()`, this keeps climbing below the minimum
    instead of reporting a flat 0, which is the whole point — a bare 0
    can't drive a progress bar) and `minGemsForLevelUp()` (whichever of
    the two minimums currently applies, extracted so both the gating
    logic and the UI read it from one place). `LevelUpContent` gained
    `rawGemsProgress`/`minGemsRequired` params (`MainActivity` passes
    `gameState.rawGemsFromLevelUpFormula()`/`minGemsForLevelUp()`,
    computed live the same way `gemsEarnable` already was) feeding a new
    `LevelUpProgressBar` inside `LevelUpCard` — a plain
    `animateFloatAsState`-driven fill bar (no rarity gradients or gloss
    the way `LairCard`'s progress bar has; this one's static most of the
    time, so that polish wasn't worth the duplication) showing "X / Y
    Gems to Level Up" — **hidden entirely once `canLevelUp` is true**,
    since the enabled button and "Get a fresh batch of N Gems" text
    already communicate "you're done" at that point; showing a
    permanently-full bar alongside that would be redundant. **Deliberately
    linear against the raw Gem count, not the underlying Gold**, since
    the Gold-to-Gems formula is square-root-scaled
    (`floor(150 * sqrt(lifetimeGoldEarned / 10^15))`) — a Gold-denominated
    bar would visually crawl near the end in the same discouraging way
    even while real progress keeps being made, since each additional Gem
    costs more Gold than the last. Verified live on-device via a direct
    Room DB edit to `lifetimeGoldEarned` (organically holding a save at a
    precise pre-threshold value wasn't practical given how fast Steward-managed
    production compounds): at a value computed to land exactly on 25 raw
    Gems (half of the 50-Gem first-Level-Up minimum), the screen correctly
    showed a half-filled bar and "25 / 50 Gems to Level Up" with the
    button disabled; raising `lifetimeGoldEarned` to comfortably clear the
    minimum (150 raw Gems) correctly hid the bar entirely and switched to
    "Get a fresh batch of 150 Gems, replacing any you're holding now."
    with the button enabled.
    **Real "eligible" gate + live earning counter + auto-return to the
    main screen (v0.36.0)**, three explicit follow-up requests after
    v0.35.0 shipped:
    - **`GameState.canLevelUp()`** (`domain/model/LevelUp.kt`) —
      `gemsEarnedFromLevelUp() > gems`, not just `> 0`. Without this, a
      player who'd already cleared the minimum once could tap Level Up
      over and over: `lifetimeGoldEarned` doesn't move between rapid
      taps, so `gemsEarnedFromLevelUp()` kept returning the exact same
      already-cleared batch forever, and every repeat tap happily wiped
      the fresh run's Gold/lairs for a Gems batch no bigger than what was
      already banked — a real bug the user caught by testing, not a
      hypothetical. `GameEngine.performLevelUp()`'s gate switched from
      `gemsEarned <= 0L` to `!current.canLevelUp()`; `LevelUpContent`'s
      local `canLevelUp` does the same (`gemsEarnable > gems`). A repeat
      Level Up now stays blocked until genuinely new lifetime earnings
      push the formula's result past what's already held.
      `LevelUpCard`'s progress-bar target follows suit — once at least
      one Gem is already banked, the bar tracks toward `gems + 1`
      (`maxOf(minGemsRequired, gems + 1)`) instead of the flat minimum,
      so it doesn't render full while the button correctly stays
      disabled.
    - **`gems.png` art** (see Assets) replaces the icon-less Gems balance
      display and `LevelUpRewardDialog`'s `open_chest` art.
    - **A live "Currently earning" counter** (`EarningCounterCard`) —
      shows `rawGemsFromLevelUpFormula()` on its own, separate from the
      Gems-balance card (what's currently *held*) and the progress bar
      (which only shows while blocked) — per the explicit ask for
      something that "won't do much in the beginning, but after a
      handful of level ups, you should see it climbing steadily."
      Recomposes every tick alongside the rest of the screen's
      `GameState`-derived params, no polling needed.
    - **Auto-return to the main screen** — `MainActivity`'s `WyrmWhelpApp`
      now collects `GameViewModel.levelUpReward` and clears `openSection`
      the moment it goes non-null (`LaunchedEffect(levelUpReward)`), so a
      successful Level Up drops the player back onto the main game screen
      (where `LevelUpRewardDialog` then pops up) instead of leaving them
      parked on the Level Up section showing its own just-reset state.
    Verified live on-device via a direct Room DB edit (with the emulator's
    wifi/data radios disabled first — cloud sync was otherwise overwriting
    the local edit with the still-unmodified cloud save on launch, the
    same merge-picks-the-whole-state gotcha noted elsewhere in this file):
    starting from a real dev save already sitting at exactly the blocked
    case (`lifetimeGoldEarned` giving 150 raw Gems, `gems` already at 150
    from an earlier Level Up) correctly showed a disabled button, "150 /
    151 Gems to Level Up," and "Currently earning: 150 gems" — confirming
    the bug this was fixed for. Raising `lifetimeGoldEarned` to yield 212
    raw Gems correctly re-enabled the button ("Get a fresh batch of 212
    Gems…") and updated the live counter to "212 gems" with the bar
    hidden. Confirming the Level Up landed on the main game screen (Gold
    Pieces and every lair reset, Gems at 212) with `LevelUpRewardDialog`
    showing the new `gems.png` art and "212 Gems" — not left sitting on
    the Level Up section.
    **Gems currency plumbing**: `GameState.gems: Long` and
    `GameState.totalLevelUps: Int` are a rename of two fields that existed
    since early in the project as an unused prestige scaffold
    (`scaleShards`/`totalMolts` — see CHANGELOG 0.23.0) but had no actual
    mechanic wired up to them until this version; the rename reflects the
    "scrap Scale Shards, Gems is the only prestige currency" decision made
    when this was built, not a currency being added on top of an existing
    one. `GameHeader` shows Gems in its `ParchmentStrip` alongside gp/sec
    and pp (`FantasyPalette` gained `gemBright`/`gemDeep` amethyst tones
    for this, the same "named color pair per concept" pattern as
    `goldBright`/`goldDeep`). Persistence: `WyrmWhelpDatabase` is at
    version 7 after three bumps in quick succession — version 5 (v0.23.0)
    for the `GameStateEntity` column rename itself, version 6 (v0.23.1)
    for a `totalGemsEarned` column the since-abandoned subtraction-based
    gate needed (alongside `lifetimeGoldEarned`, which stayed), version 7
    (v0.24.0) dropping that same `totalGemsEarned` column once Gems
    stopped accumulating and the column had nothing left to do — no
    formal migration for any of the three, same destructive-fallback
    policy as every prior bump. `GameStateDto`'s renamed `gems`/
    `total_level_ups` JSON keys keep a `= 0` default (unlike most other
    *required* DTO fields) specifically because that was a wire-format
    rename, not a newly-added field — an old cloud save's
    `scale_shards`/`total_molts` keys simply go unused rather than being
    read; `lifetime_gold_earned` is a genuinely new field and gets the
    same kind of default for the usual reason (an older cloud save's JSON
    blob won't have that key yet). The app has no real installs to
    preserve regardless (same trade-off as the Room migration policy).
- **Data:**
  - **Room** — local persistence, implemented (`data/local/`): `GameStateEntity`
    (single-row table for currencies/meta) + `OwnedLairEntity` (one row per
    claimed lair), `GameStateDao`, `WyrmWhelpDatabase`. `RoomGameRepository`
    implements the domain-layer `GameRepository` interface
    (`domain/repository/GameRepository.kt`) — `GameViewModel` loads the save
    on creation (before applying offline earnings/starting the tick loop) and
    autosaves every 30s. Upgrades/milestones tables don't exist yet (those
    systems aren't built). **No real migrations exist** — `WyrmWhelpDatabase`
    has no `Migration` objects, so `DatabaseModule`'s `Room.databaseBuilder(...)`
    call adds `.fallbackToDestructiveMigration(dropAllTables = true)` and
    the database version is bumped by 1 (currently 9, most recently for
    v0.26.0's Platinum Upgrades — see that bullet under Tech stack; full
    version-by-version history lives in `WyrmWhelpDatabase.kt`'s own doc
    comment, not duplicated here) any time a persisted field
    is added or changed. This wipes existing local saves on that version
    bump rather than crashing at DB-open time — a deliberate pre-release
    trade-off (no real installs to preserve yet, and a full migration is out
    of scope for now), not an oversight. Revisit before a real release.
    Supabase's side of the same kind of change is lighter: since cloud saves
    are one jsonb blob (see below), a new `GameStateDto` field only needs a
    default value for old cloud saves to keep decoding — no schema/SQL
    change needed there.
  - **Supabase** — auth + cloud saves implemented (`data/remote/`):
    `SupabaseAuthRepository` (anonymous sign-in, implements domain
    `AuthRepository`) and `SupabaseCloudSaveRepository` (upload/download the
    `cloud_saves` row as jsonb via `GameStateDto`, implements domain
    `CloudSaveRepository`). `SupabaseModule` (`di/`) provides the
    `SupabaseClient` (Auth + Postgrest plugins) from `BuildConfig.SUPABASE_URL`
    / `SUPABASE_ANON_KEY`, which are read from `local.properties` (gitignored —
    see below). Merge-on-launch logic lives in `domain/model/GameStateExtensions.kt`
    (`mergeGameStates`, `estimatedNetWorth`). Leaderboard not started.
  - **DataStore** — user preferences, consent state, ad-watch tracking
  - Repositories wrap each data source; ViewModels never touch Room/Supabase directly.
- **DI bindings:** `@Singleton` for `GameEngine` and `AdManager` (both real);
  `ConsentManager` (privacy/ad consent — GDPR/UMP) is still aspirational,
  not built — see Open Questions; `@HiltViewModel` for all ViewModels;
  `@ApplicationContext` injected where needed; `@Inject constructor`
  throughout, no explicit `@Provides` needed for either singleton.

### Save data & cloud sync

- **Local:** Room. `GameEngine` autosaves every 30s (toggleable in Settings —
  Settings screen not built yet, so not actually toggleable in-app).
- **Cloud:** Supabase `cloud_saves` table, entire game state as one `jsonb` blob
  (not normalized relational tables — simplest for idle-game state + offline math).
  Table/RLS policies defined in `SQL/001_create_cloud_saves_table.sql`.
- **Sync triggers — implemented:** once per app launch, right after anonymous
  sign-in resolves (`GameViewModel` init). **Not yet implemented:** manual "Save
  to Cloud" button in Settings, on-Level-Up sync, any periodic/backgrounding
  push — none of those exist yet (no Settings screen, no Level Up). See open
  questions.
- **Merge logic:** `domain/model/GameStateExtensions.kt#mergeGameStates` —
  compares local vs. cloud `GameState`, higher `totalLevelUps` (Level Up count)
  wins outright,
  `estimatedNetWorth()` (liquid currency + what owned lairs cost to claim from
  scratch) breaks ties within the same prestige count. The winner is loaded
  into `GameEngine`, then re-saved to both Room and Supabase after offline
  earnings settle.
- **Offline earnings:** `GameState.lastSavedAt` (whichever save — local or
  cloud — won the merge) feeds `GameEngine.applyOfflineEarnings()`, capped by
  `offlineCapHours` (default 4h, upgradeable via game progression, not
  implemented yet).
- **Resilience:** every network step (sign-in, download, upload) in
  `GameViewModel` is wrapped individually so a failure degrades to local-only
  play instead of blocking or crashing — see `Log.w(TAG, ...)` call sites.
- **Guest mode:** doesn't really exist as a separate mode anymore — anonymous
  auth means every install gets cloud sync from the first launch (falling back
  to local-only only if the network/Supabase call actually fails).

### Auth

Supabase anonymous auth first — players start instantly with cloud sync/leaderboard
eligibility from the first session. They can now link email/password to carry
progress across devices and reinstalls without losing anonymous-session
progress — see `GameViewModel.signUp`/`signIn`/`signOut` and
`ui/settings/SettingsContent.kt` above. Google/OAuth linking specifically is
still not implemented, only email/password (no extra SDK/manifest setup
needed for that, unlike OAuth's redirect handling).

**Project config:** `SUPABASE_URL` / `SUPABASE_ANON_KEY` live in `local.properties`
(gitignored, not committed) and are exposed to the app via `BuildConfig` fields
(`app/build.gradle.kts` reads `local.properties` at build time). A fresh clone
needs these two lines added to its own `local.properties` before cloud sync will
work — ask whoever has the Supabase project for the values, or create a new
Supabase project and run the scripts in `/SQL` against it.

**Dashboard-only settings this feature depends on** (not covered by `/SQL`,
same as the existing "Enable Anonymous Sign-Ins" toggle):
- Authentication > Sign In / Providers > Email must be enabled for
  `signUp`/`signIn` to work at all.
- **Authentication > Emails > "Confirm email changes" must be ON for the
  0.16.0 sign-up verification-code gate to mean anything at all.** With it
  on, `signUp` sends a code and `currentUserEmail()` stays null until
  `verifySignUpCode` confirms it — the actual anti-bot gate. With it off,
  Supabase applies the new email immediately with no code ever sent, and
  `signUp` detects that (`currentUserEmail()` already non-null right after)
  and skips the code step entirely — the account creation flow still
  *works*, it just no longer blocks bots, defeating the point of the
  feature. Check this setting if sign-up ever stops asking for a code.
- **Authentication > Emails > "Change Email Address" template must
  actually include `{{ .Token }}`** for the code shown to the player to
  exist at all — this is the template that fires here (see the
  `EMAIL_CHANGE` OTP type note above), and Supabase's default uses
  `{{ .ConfirmationURL }}` (a magic link) instead, which would send a real
  email with no visible code, leaving `VerificationCodeForm` waiting on
  something that never arrives. Edit the template in the dashboard to
  include the numeric token. The **code's length** is also a project
  setting there (Supabase defaults to 6 digits; configurable) — the app
  doesn't hardcode or validate a specific length, so any length works
  without an app change.
- Supabase enforces a low default **email rate limit** on its free tier
  (a handful of confirmation/recovery emails per hour) — hit this during
  this feature's own testing (see CHANGELOG 0.15.0) after a few `signUp`
  attempts in a row. Shows up as `AuthRestException: email rate limit
  exceeded`, surfaced correctly as an `authMessage` — not an app bug, but
  worth knowing before assuming repeated sign-up testing is broken.

### Monetization

Free-to-play: rewarded ads (boosts, offline-earnings multipliers) + optional IAP
(gems, time-skips, cosmetics). No forced interstitials.

**Rewarded ads — all four placements live, AdMob app id
`ca-app-pub-1913393601233746~8060140149`** (in the manifest — see the
`AdManager` bullet under Tech stack for the full picture, including the
test-device safeguard that must stay in debug builds):
- **Welcome Back "Watch Ad to Double"** (live, 0.17.0) — doubles the
  offline-earnings amount shown in the "While You Were Away…" dialog. Ad
  unit id `ca-app-pub-1913393601233746/1494731799`.
- **Shop "Watch an Ad" (Platinum)** (live, 0.18.0; open to guests since
  0.18.1) — earns 2 Platinum Pieces, once every 24 hours (cooldown tracked
  on the save itself, not ad-network- or device-side — see the "Shop's
  Watch an Ad" bullet under Tech stack). Ad unit id
  `ca-app-pub-1913393601233746/9425192707`. The only one of the four still
  actually offered from the Shop.
- **Main screen "Watch an Ad" popup (Speed boost)** (live, 0.29.0; open
  to guests, no real money involved) — grants a free 2x Speed boost for
  4 hours, up to 4 independent watches at a time, each on its own
  24-hour cooldown. Lived briefly in the Shop's Temporary tab
  (v0.29.0–v0.33.x) before moving, alongside the new Income option below,
  exclusively into `QuickAdBoostButton`'s popup on the main game screen
  in v0.34.0 (see that bullet under Tech stack). Ad unit id
  `ca-app-pub-1913393601233746/7941856119`.
- **Main screen "Watch an Ad" popup (Income boost)** (live, 0.34.0; open
  to guests, same reasoning as Speed) — grants a free 2x Income boost for
  2 hours, same four-independent-daily-watches shape as Speed but its
  own separate ledger/cooldowns (see the "Second ad-watch reward" bullet
  under Tech stack). Never lived in the Shop at all — added straight
  into the popup alongside Speed. Ad unit id
  `ca-app-pub-1913393601233746/9610995150`.

The premium currency is `GameState.platinumPieces` (labeled "pp" in the
UI) — no separate "Jewels" or other premium currency was added; platinum
was already designed for exactly this (IAP-sourced, ad-earnable) per its
own doc comment, it just didn't have a UI home yet. The Shop section
(`ui/shop/ShopContent.kt`, reachable from `FloatingMenu`) is that home now
— a balance display, the real spend path (nine permanent boost tiers,
four temporary boost tiers, and six Time Skip sizes as of v0.26.0 — see
the Platinum Upgrades bullet under Tech stack above), the real ad-earn
path described above, and — as of v0.27.0 — **real "Buy Platinum Pieces"
IAP**, five Google Play Billing packs from $0.99 to $9.99 (see the
`BillingManager` bullet under Tech stack for the pricing/PP-amount
reasoning and what's still unverified).
**Only "Buy Platinum Pieces" (real money) is hidden for guests** — as of
0.18.1, "Watch an Ad" is open to everyone, guests included: it earns no
real money, so a guest losing that Platinum on reinstall isn't the kind of
loss the sign-in gate exists to prevent. "Buy Platinum Pieces" stays
behind `ShopContent`'s `isSignedIn` param (wired from
`GameViewModel.userEmail != null` in `MainActivity`) — a guest sees an
explanatory note there instead, since *that* purchase is real money and
should stay tied to a recoverable account. The permanent/temporary boost
and Time Skip sections are unaffected either way since spending Platinum
already owned isn't a real-money transaction. **Requires the five product
ids in `PLATINUM_PURCHASE_OPTIONS` to exist as consumable in-app products
in the Google Play Console** before any of it actually charges anything —
see the `BillingManager` bullet's dashboard-dependency note, same
category as the AdMob ad units and Supabase toggles documented elsewhere
in this file. See Open Questions for what's still missing.

## Core game design

- **Generators — Creature Lairs/Dens:** direct analog to Adventure Capitalist's
  businesses. Each lair is a themed monster den, using real D&D 5E SRD
  creatures/Challenge Ratings for flavor and tuning anchor, from Kobold Warren
  (CR 1/8) up through the Ancient Dragon's Hoard (CR 24) — see
  `domain/catalog/CreatureLairCatalog.kt` for the full 14-tier list and
  `domain/model/CreatureLair.kt` / `OwnedLair.kt` / `GameState.kt` for the data
  model. `domain/engine/GameEngine.kt` is the app-scoped singleton tick loop:
  each lair produces gold on a cycle timer; without a hired Steward, a lair
  sits idle earning nothing until tapped — the tap *starts* its production
  cycle (`startLairLoad`), which then fills up on its own and auto-collects
  the instant it completes, firing a coin-burst effect (see the gold
  collection redesign under `LairRow`/`LairCard` above). A Steward
  auto-collects every completed cycle continuously, online or offline, with
  no tap needed and no confetti. Whelps/Wyrms are a
  separate collectible/pet system layered on top of the lair economy (details
  TBD as we build it out — not yet started).
- **Balance:** tiers 0–9 (Kobold Warren through Troll Warren) use AdVenture
  Capitalist's real Earth Business numbers 1:1 (gold pieces standing in for
  dollars) — see `CreatureLairCatalog`'s class doc for exactly which AdCap
  business maps to which lair and how tiers 10–13 (D&D has no Earth
  equivalent past Oil Company) extend the same cost/income patterns with a
  tempered cycle-time curve. A new save starts owning one Kobold Warren
  already (matching AdCap's free starting Lemonade Stand) with 0 gold.
- **Prestige — Level Up** (renamed from "Molt" — "Molt" only fit
  dragon-flavored lairs, not the goblins/orcs/etc. earlier in the catalog;
  "Level Up" is generic TTRPG language that fits every tier), implemented
  in v0.23.0: resets the current hoard (Gold Pieces and every owned lair,
  back to the exact starting shape a brand-new save begins with) for a
  fresh batch of **Gems** — a *temporary* income-multiplier head start
  for the next run (v0.24.0), not an accumulating currency the way a
  typical idle game's prestige points are. Modeled on AdVenture
  Capitalist's real Angel Investors (same reward formula), but
  deliberately not on their permanence: a bigger Gem batch means a
  bigger head start on *that* run, which matters once a leaderboard
  exists to compare how fast players ramp up, not how many Gems anyone's
  banked. Gems replaced an earlier planned currency name, "Scale
  Shards," which had a dormant `GameState` field
  (`scaleShards`/`totalMolts`) but no actual mechanic wired up yet when the
  rename happened — that field is `GameState.gems`/`totalLevelUps` now,
  the same rename `GameState.totalMolts`'s own doc comment had been asking
  for since before this was built. See `domain/model/LevelUp.kt`,
  `GameEngine.performLevelUp`, and `ui/levelup/LevelUpContent.kt` under
  Tech stack below for the full implementation.
- **Art style:** vector/flat illustration, built with Compose (custom vector
  drawables + Compose Canvas for animation). No external sprite/asset-pack
  dependency.

Currency names, lair tiers/costs, whelp/wyrm collectible mechanics, leaderboard
scope, and exact number-formatting (large-number suffixes) are still open —
we'll pin these down as we build each system.

## Open questions / not yet decided

- Whelp/Wyrm collectible system mechanics (how it interacts with lairs)
- Full currency list — now three, all wired into `GameState`: Gold Pieces
  (primary), Platinum Pieces (premium — the naming question is settled,
  it's Platinum, not a separate "Jewels," see Monetization above), and
  Gems (Level Up's *temporary* per-run currency, v0.23.0, redesigned in
  v0.24.0 — see `domain/model/LevelUp.kt`). Platinum has a real spend
  path (permanent/temporary boost tiers, Time Skips — see the Platinum
  Upgrades bullet under Tech stack) and a real *earn* path (the Shop's
  "Watch an Ad," 2 pp, 24h
  cooldown, 0.18.0), and — as of v0.27.0 — a real *buy* path (five Google
  Play Billing packs, see the `BillingManager` bullet under Tech stack;
  unverified end-to-end since no product exists yet in a real Play
  Console listing). Gems are earned solely via
  Level Up (replacing whatever batch was already held, not accumulating)
  and spent nowhere yet — their only effect is `gemIncomeMultiplier`'s
  flat income bonus, itself just as temporary since it reads the live
  `gems` balance; a real Gem shop (cosmetics, one-run-only boosts, etc.)
  is still open — see the Level Up bullet under Tech stack for why any
  such shop's purchases would need to reset alongside Gems themselves,
  unlike Platinum-bought Boosts.
- `ConsentManager` (GDPR/UMP ad-consent flow) isn't built — the rewarded
  placements live so far ship without any consent gating in front of them.
  Needed before ads run for real EU/UK traffic; revisit before
  removing the debug-only test-device forcing in `AdManager`.
- Large-number formatting convention — first-pass answer landed in
  `ui/format/GoldFormat.kt`: K/M/B/T/Qa/.../Dc named short-scale suffixes,
  then A/B/.../Z/AA/AB/... (bijective base-26, same scheme as spreadsheet
  columns) indefinitely beyond that, so display never falls back to a raw
  digit string no matter how large the economy grows. `GameState.goldPieces`
  is still a raw `Double` underneath, though, which will need revisiting once
  the economy grows past what a `Double` represents precisely — the suffix
  scheme fixes the *display* problem, not the underlying precision one
- Avatar system — **built as of v0.40.0**: `GameHeader`'s `MedallionEmblem`
  is tappable and shows the player's chosen portrait (real art, cropped to
  a circle inside the same gold ring that used to frame the engraved-shield
  placeholder), which now only shows before a choice has been made. See
  the `AvatarPickerDialog`/`domain/model/Avatar.kt` bullet under Tech
  stack. Not yet done: any cosmetic tie-in beyond the header (e.g. a
  future leaderboard showing other players' avatars).
- Creature portrait art — in progress, one lair at a time (see `LairRow`'s
  `lairPortraitRes` above): Kobold Warren, Giant Rat Burrow, Goblin Camp,
  Orc Encampment, Gnoll Den, Bugbear Warcamp, Hobgoblin Barracks, Ogre's
  Cave, Owlbear Roost, Troll Warren, Wyvern Aerie, and Young Dragon's
  Lair have real art as of v0.43.1 — 12 of the 14 tiers. Still showing
  `CreatureAvatar`'s rarity-tinted placeholder disc with the monster's
  first initial: Adult Dragon's Lair and Ancient Dragon's Hoard (the top
  2 tiers).
- Lair cost/income/timing for tiers 0–9 is sourced directly from AdVenture
  Capitalist's Earth Businesses (see `CreatureLairCatalog`); tiers 10–13 are
  our own extrapolation of the same patterns, still not playtested
- Manual upgrade shop — **fully built as of v0.26.0** across all three
  currencies. v0.25.0 shipped Gold (475 tiers) and Gems (200 tiers), both
  resetting on Level Up (see the Upgrades bullet under Tech stack);
  v0.26.0 filled in Platinum — nine repurchasable permanent boost tiers,
  four instant temporary boost tiers, and six Time Skip sizes (see the
  Platinum Upgrades bullet under Tech stack), all bought in the Shop and
  the only upgrade-like state in the game that's genuinely permanent
  through a Level Up, confirming the design intent recorded here since
  v0.25.0. This isn't the earlier-mentioned "100 tiers, phase-jump shape"
  idea — the user's actual follow-up request was nine named discrete
  multiplier tiers plus flat-rate consumables instead, deliberately not
  mirroring the Gold/Gem phase/tier-jump curve.
- **Leaderboard — built as of v0.41.0**: three boards (All-Time / Weekly /
  Monthly gold earned), reachable from the menu. See the Leaderboard
  bullet under Tech stack for the full design; `SQL/004_create_leaderboards.sql`
  must be run once (and needs the pg_cron extension) before any of it
  actually populates. Not yet decided: any *other* leaderboard metric
  (net worth, fastest Level Up, per-lair records) — this only covers
  gold earned.
- Target device scope (phone-only vs. tablet/landscape support)
- Cloud sync now happens on launch, every 5 minutes, on sign-up/sign-in, and
  via a manual "Sync Now" button (see the Account/sync bullet under Tech
  stack) — no Level Up or app-backgrounding trigger yet (Level Up isn't
  built; a backgrounding push would need a lifecycle observer that doesn't
  exist yet either).
- Anonymous identity is now recoverable via email/password linking
  (`GameViewModel.signUp`/`signIn` — see the Auth section) — Google/OAuth
  linking specifically is still not implemented. A guest who never creates
  an account still loses their identity on reinstall, unchanged from
  before.
- `lastSyncedAt`/`isSyncing` (Settings' sync card) reset on every app
  relaunch — not persisted, same category of simplification as
  `BuyQuantity` resetting to `X1` each launch. The *actual* synced data is
  obviously still saved for real; only the UI's "when did this last happen"
  display forgets between sessions.
