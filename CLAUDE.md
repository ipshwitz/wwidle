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
  and read via `AssetManager`); nothing lives there yet. Files land in
  `/assets` before being turned into Compose-usable drawables/resources.

## Workflow rules

These apply to every change made in this repo, however small:

1. **Bump the version every change**, using `A.B.C` (Major.Minor.Patch) semantic
   versioning in [app/build.gradle.kts](app/build.gradle.kts) (`versionName`, and
   increment `versionCode` by 1 each bump):
   - **Patch (A.B.C → A.B.C+1):** bug fixes, tuning, small tweaks, refactors.
   - **Minor (A.B.C → A.(B+1).0):** new features/systems added, backward-compatible.
   - **Major ((A+1).0.0):** breaking save-data changes, ground-up reworks, or the
     jump from pre-release (0.x.x) to first stable release (1.0.0).
   - Current version: **0.3.1** (added `/assets` convention — see [CHANGELOG.md](CHANGELOG.md)).
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
  which needs AGP 9.1.0+.
- **Emulator screencap/screenrecord returns a blank frame on this host:** the
  local Pixel_8 AVD (headless, `-no-window`) produces a byte-identical blank
  white capture from both `adb shell screencap` and `screenrecord` regardless
  of GPU mode (`swiftshader_indirect`, `angle_indirect`) or HWUI renderer
  backend (`skiagl`) — a host/driver-level compositor bug, not an app bug.
  The view hierarchy is still trustworthy: `adb shell uiautomator dump` (pull
  with `adb pull //sdcard/window_dump.xml <path>`, double leading slash to
  dodge Git Bash path mangling) reports real widget text/state and is the
  reliable way to verify UI content from this environment until real
  screenshots work again (try a different AVD system image, or verify on a
  physical device/Android Studio instead).

## Tech stack & architecture

**Pattern:** MVVM + Clean Architecture, DI via Hilt.

- **Presentation (UI):** Jetpack Compose screens/composables. ViewModels expose
  `StateFlow` to the UI. `@HiltViewModel` throughout.
  - `GameViewModel` — implemented (`ui/game/GameViewModel.kt`): wraps
    `GameEngine`, starts its tick loop and settles offline earnings once on
    creation, exposes claim/hire-Steward/plunder actions. `GameScreen`/
    `LairCard`/`WelcomeBackDialog` (`ui/game/`) are the first real screen,
    wired up in `MainActivity` via `by viewModels()`.
  - `AuthViewModel` — Supabase auth
  - `SettingsViewModel` — user preferences
  - `ConsentViewModel` — privacy/ad consent
- **Domain:** `GameEngine` — core tick loop, income calculation, offline-earnings
  math. `@Singleton` via Hilt.
- **Data:**
  - **Room** — local persistence (`GameState`, lairs/generators, upgrades, milestones)
  - **Supabase** — auth, cloud saves, leaderboard
  - **DataStore** — user preferences, consent state, ad-watch tracking
  - Repositories wrap each data source; ViewModels never touch Room/Supabase directly.
- **DI bindings:** `@Singleton` for `GameEngine`, `AdManager`, `ConsentManager`;
  `@HiltViewModel` for all ViewModels; `@ApplicationContext` injected where needed;
  `@Inject constructor` throughout.

### Save data & cloud sync

- **Local:** Room. `GameEngine` autosaves every 30s (toggleable in Settings).
- **Cloud:** Supabase `cloud_saves` table, entire game state as one `jsonb` blob
  (not normalized relational tables — simplest for idle-game state + offline math).
- **Sync triggers:** manual "Save to Cloud" button in Settings, on Molt (prestige),
  on login/register.
- **Merge logic (sign-in mid-session):** compare local vs. cloud progress, keep
  whichever is higher, upload the winner to Supabase and overwrite local Room with it.
- **Offline earnings:** `lastSavedTimestamp` stored in `GameState`. On launch,
  `GameEngine.initialize()` computes `elapsed = now - lastSavedTimestamp` and
  calls `calculateOfflineEarnings(elapsed, applyOfflineCap = true)`. Cap is
  `offlineCapHours`, default 4h, upgradeable via game progression.
- **Guest mode:** plays entirely from local Room, no Supabase sync. On sign-in:
  local save uploads if no cloud save exists yet, otherwise merge logic applies.

### Auth

Supabase anonymous auth first — players start instantly with cloud sync/leaderboard
eligibility from the first session. They can later link email/Google to carry
progress across devices and reinstalls without losing anonymous-session progress.

### Monetization

Free-to-play: rewarded ads (boosts, offline-earnings multipliers) + optional IAP
(gems, time-skips, cosmetics). No forced interstitials.

## Core game design

- **Generators — Creature Lairs/Dens:** direct analog to Adventure Capitalist's
  businesses. Each lair is a themed monster den, using real D&D 5E SRD
  creatures/Challenge Ratings for flavor and tuning anchor, from Kobold Warren
  (CR 1/8) up through the Ancient Dragon's Hoard (CR 24) — see
  `domain/catalog/CreatureLairCatalog.kt` for the full 14-tier list and
  `domain/model/CreatureLair.kt` / `OwnedLair.kt` / `GameState.kt` for the data
  model. `domain/engine/GameEngine.kt` is the app-scoped singleton tick loop:
  each lair produces gold on a cycle timer; without a hired Steward, a
  finished cycle sits full and waits for the player to tap it ("plunder") —
  it never silently completes a second cycle underneath them. A Steward
  auto-collects every completed cycle, online or offline. Whelps/Wyrms are a
  separate collectible/pet system layered on top of the lair economy (details
  TBD as we build it out — not yet started).
- **Prestige — Molt:** resets the current hoard/lairs in exchange for **Scale
  Shards**, a permanent-bonus currency that boosts all future runs.
- **Art style:** vector/flat illustration, built with Compose (custom vector
  drawables + Compose Canvas for animation). No external sprite/asset-pack
  dependency.

Currency names, lair tiers/costs, whelp/wyrm collectible mechanics, leaderboard
scope, and exact number-formatting (large-number suffixes) are still open —
we'll pin these down as we build each system.

## Open questions / not yet decided

- Whelp/Wyrm collectible system mechanics (how it interacts with lairs)
- Full currency list (gold + premium currency name, any specialty currencies) —
  Gold Pieces and Platinum Pieces are wired into `GameState`, but Platinum has
  no spend sink yet (IAP/shop not built)
- Large-number formatting convention — first-pass answer landed in
  `ui/format/GoldFormat.kt` (K/M/B/T/Qa/... suffixes); `GameState.goldPieces`
  is still a raw `Double` underneath, which will need revisiting once the
  economy grows past what a `Double` represents precisely
- Lair cost/income/timing balance in `CreatureLairCatalog` is a first-pass
  guess, not playtested
- Upgrade system (AdCap-style income multipliers per lair at ownership
  milestones) — not implemented; each lair's income currently scales linearly
  with units owned only
- Leaderboard scope (global hoard value? fastest Molt? per-lair records?)
- Target device scope (phone-only vs. tablet/landscape support)
