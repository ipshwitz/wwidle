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
  (1254x1254), genuinely transparent corners, no re-export needed. See the
  Open Questions entry on creature portrait art for which other
  already-generated candidates are being held back pending a style redo.
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
   - Current version: **0.21.3** (progress-fill bar now snaps instantly on
     cycle reset instead of tweening backward into the next cycle — see
     [CHANGELOG.md](CHANGELOG.md)).
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

## Tech stack & architecture

**Pattern:** MVVM + Clean Architecture, DI via Hilt.

- **Presentation (UI):** Jetpack Compose screens/composables. ViewModels expose
  `StateFlow` to the UI. `@HiltViewModel` throughout.
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
  - **`WelcomeBackDialog`** (`ui/game/WelcomeBackDialog.kt`) — the offline-
    earnings pop-up, restyled from a plain Material `AlertDialog` to match
    the cozy-fantasy chrome: a plain `Dialog` (not `AlertDialog` — none of
    its title/text/button slots would let this look like anything other than
    a Material dialog) with `usePlatformDefaultWidth = false`, containing a
    parchment-gradient `Column` with a carved wood border, the existing
    `open_chest` art (no new asset needed), `GlowingGoldText` for the amount
    earned, and a `WoodenButton` ("Claim") instead of a Material
    `TextButton`.
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
    content-driven height — no magic numbers kept in sync between the two.
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
    > 0 && !owned.hasSteward && !owned.isLoading`) gates both tap targets and
    dims the avatar identically to the card's own disabled state. Owns the
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
  - `LairCard` styling: no Material `Card` — a custom `Box` sized via
    `Modifier.height(IntrinsicSize.Min)` so a second, fractionally-widthed Box
    can render *behind* the text/buttons as a left-to-right fill representing
    [progress] (100% = the cycle a tap started is about to complete and
    auto-collect). Restyled to match `GameHeader`'s cozy-fantasy chrome (was
    flat rarity-colored blocks with a Material `Button`/`OutlinedButton`
    pair, which read as "boring" against the rest of the game): a
    translucent parchment gradient base (`FantasyPalette.parchmentShade`/`parchment`,
    alpha 0.55 — sheer enough that `GameScreen`'s background art still shows
    through, same as before) plus a faint per-tier "rarity" tint
    (`rarityColor(tier)`, the same 5-band green→blue→purple→orange→gold ramp
    as always) over the whole card, with a stronger rarity gradient for the
    claimed-fraction fill and a bright 2px line at the fill's own trailing
    edge (drawn via `drawBehind` *on the fill `Box` itself*, at its own right
    edge — since that edge already sits exactly at the animated fraction, no
    extra position math needed; the line is only drawn while the fraction is
    strictly between 0 and 1, so a fully solid card reads as a clean
    undivided tint with no seam). `FontFamily.Serif` for the name (matching
    `GameHeader`), italic muted text for monster/CR, bold `goldDeep`-colored
    text for the income line. The Claim button is now the shared
    `WoodenButton` instead of a Material `Button`. **The Steward button is
    gone** — hiring a Steward now lives solely in the Stewards menu section
    (see `StewardsContent` below), not on every card; `LairCard` no longer
    takes `onHireSteward`. The card's `clickable` (`enabled = owned.count >
    0 && !owned.hasSteward && !owned.isLoading`, `onClick = onStartLoad`) is
    what actually starts the cycle now, not a collection.
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
      `GameEngine.PROGRESS_SOLID_THRESHOLD_SECONDS` (3× `TICK_INTERVAL_MS`,
      ~99ms — the point past which a lair's own `effectiveProductionSeconds`
      can complete inside a single tick, so the raw ratio stops being
      meaningful) it reports a flat `1f` instead of the raw ratio — a
      continuously solid bar, which is the *truthful* picture once cycles
      complete far faster than a human can watch one fill, not an aliased,
      jittery one. `GameViewModel.lairProgress` exposes it straight through;
      `GameScreen` collects it and passes `lairProgress[lair.id] ?: 0f` down
      through `LairRow` to `LairCard`'s new `progress: Float` param (`LairCard`
      no longer takes `speedBoostMultiplier`/`globalSpeedMultiplier` — it
      doesn't need to derive anything itself anymore).
    - `LairCard`'s `animateFloatAsState` now uses a **fixed 150ms tween**
      (`PROGRESS_ANIMATION_DURATION_MS`), deliberately *not* tied to
      `TICK_INTERVAL_MS` the way it used to be — tracking the tick rate
      exactly meant the animation re-synced to a fresh target almost
      immediately every tick, so a fast-resetting target produced visible
      bounce instead of being smoothed over several ticks.
    - **Snap-on-reset (v0.21.3)** — the 150ms tween fixed the bounce but
      introduced a new, subtler issue: since [progress] only ever increases
      within a cycle and drops exactly once on completion, tweening *that*
      drop the same smooth way made the bar visibly slide backward into its
      next cycle instead of resetting cleanly — and since that backward
      slide ate into the 150ms window, it also cut the next forward tween
      short enough that a moderately fast lair's bar rarely looked like it
      actually reached full before resetting again. `LairCard` now
      `remember`s the previous recomposition's raw `progress` value per
      card; any *decrease* (never a natural part of the ramp — there's no
      other way `progress` goes down) swaps the `animationSpec` to `snap()`
      for that one frame instead of `tween(...)`, so the bar jumps instantly
      to empty and the next fill starts clean. Confirmed via a burst of
      screenshots on a Steward-managed lair: fill visibly climbed
      65%→90%→95% across consecutive frames, then the very next sample
      showed it already restarted at a fresh low value with no
      intermediate slide-back frames — a snap, not a tween.
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
    section's real content: an Account card (sign up/in/out) and a Cloud
    Sync card (automatic-every-5-minutes note, last-synced time, manual
    "Sync Now"). No separate `AuthViewModel`/`SettingsViewModel` exists —
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
  - **`AdManager`** (`ads/AdManager.kt`) — the app's ad integration, via the
    Google Mobile Ads SDK (`play-services-ads`). `@Singleton`, same
    app-scoped pattern as `GameEngine`: constructed once by Hilt,
    initializes `MobileAds` and starts loading a rewarded ad for every
    `RewardedPlacement` immediately in its `init` block rather than waiting
    for a screen to ask for one — so an ad is normally already loaded by
    the time a player reaches a rewarded placement instead of loading on
    demand. `RewardedPlacement` is a small enum, one entry per real AdMob
    ad unit id (`OFFLINE_EARNINGS_DOUBLE`, `SHOP_PLATINUM` as of 0.18.0) —
    adding a new rewarded spot means adding an entry there; `AdManager`
    itself tracks a loaded-ad slot per placement (`Map<RewardedPlacement,
    RewardedAd>`) generically rather than one hardcoded field per
    placement, so it doesn't need touching again for a third. Two entry
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
    against a race between two rapid taps). `ShopContent`'s `WatchAdRow`
    computes its own "Available in Xh Ym" label reactively from
    `gameState.platinumAdCooldownRemaining()` passed in by `MainActivity` —
    no separate countdown timer or polling, it just updates naturally as
    `gameState` ticks. Shared `ui/format/DurationFormat.kt` formats that
    countdown for both `WatchAdRow`'s button label and
    `GameViewModel.platinumAdMessage`'s cooldown text, extracted once it
    was clear both needed the identical "3h 12m" logic.
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
    "gp/cycle" text and progress-bar fill, `GameScreen`'s gold-per-second
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
  - **`ShopContent`** (`ui/shop/ShopContent.kt`) — the Shop section's real
    content: a balance card, then a "Boosts" section (the actual spend path
    for Platinum Pieces — see the Boosts bullet below and Monetization) with
    one row each for Speed Boost, Profit Boost, and every entry in
    `TIME_SKIP_OPTIONS` (0.19.0 added a second, cheap tier — see that
    bullet), then the "Earn Platinum" section covered under Monetization
    below (the real "Watch an Ad" plus the still-disabled "buy outright"
    IAP). `FloatingMenu`'s `"Shop"` entry (its own wooden-sign art as of
    v0.20.1) reaches it. Takes
    `platinumPieces`/`speedBoostLevel`/`profitBoostLevel` plus
    `onBuySpeedBoost`/`onBuyProfitBoost`/`onBuyTimeSkip` callbacks —
    `WyrmWhelpApp` wires the callbacks straight to the matching
    `GameViewModel` methods, same pattern as `StewardsContent`'s
    `onHireSteward`.
  - **Boosts** (`domain/model/Boosts.kt`) — permanent, account-wide bonuses
    bought with Platinum Pieces, *not* tied to any one lair (unlike the
    ownership milestones): Speed Boost (10 pp base, ×1.5 cost growth/level,
    +5% production speed/level, compounding) and Profit Boost (same cost
    curve, +10% income/level, compounding), plus repeatable Time Skips —
    `TIME_SKIP_OPTIONS: List<TimeSkipOption>` (`costPp`/`seconds` pairs,
    cheapest first: 2 pp for 10 minutes, 5 pp for 1 hour as of 0.19.0),
    each instantly granting that much production via the same
    `GameEngine.advance()` logic offline earnings use. Deliberately a list
    rather than a single fixed size/cost pair — more tiers are expected
    here over time; the 10-minute one exists specifically so the whole
    Platinum loop (earn 2 pp from one ad watch, spend it immediately) is
    cheaply testable end to end. Levels live on
    `GameState.speedBoostLevel`/`profitBoostLevel`.
    `CreatureLair.incomePerCycle` takes a third `profitBoostMultiplier`
    parameter (default 1.0) alongside the existing global-milestone one, and
    a new `CreatureLair.effectiveProductionSeconds(speedBoostMultiplier)`
    (default 1.0, divides `baseProductionSeconds`) replaces raw
    `baseProductionSeconds` everywhere a lair's actual cycle time matters —
    `GameEngine.advance`/`advanceLair` (both the unmanaged-lair readiness
    check and the Steward auto-collect loop), `LairCard`'s progress-bar
    fraction, and `GameScreen`'s gold-per-second sum. `GameEngine` exposes
    `purchaseSpeedBoost()`/`purchaseProfitBoost()`/
    `purchaseTimeSkip(option: TimeSkipOption)` (each: check affordability,
    deduct Platinum, apply); `GameViewModel` has matching thin wrappers,
    same shape as `claimLair`/`hireSteward`.
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
    the database version is bumped by 1 (currently 4, most recently for the
    v0.20.0 gold-collection redesign's `OwnedLairEntity.isReadyToCollect` →
    `isLoading`+`completedLoads` column swap) any time a persisted field
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
  compares local vs. cloud `GameState`, higher `totalMolts` (Level Up count)
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

**Rewarded ads — both placements live, AdMob app id
`ca-app-pub-1913393601233746~8060140149`** (in the manifest — see the
`AdManager` bullet under Tech stack for the full picture, including the
test-device safeguard that must stay in debug builds):
- **Welcome Back "Watch Ad to Double"** (live, 0.17.0) — doubles the
  offline-earnings amount shown in the "While You Were Away…" dialog. Ad
  unit id `ca-app-pub-1913393601233746/1494731799`.
- **Shop "Watch an Ad"** (live, 0.18.0; open to guests since 0.18.1) —
  earns 2 Platinum Pieces, once every 24 hours (cooldown tracked on the
  save itself, not ad-network- or device-side — see the "Shop's Watch an
  Ad" bullet under Tech stack). Ad unit id
  `ca-app-pub-1913393601233746/9425192707`.

The premium currency is `GameState.platinumPieces` (labeled "pp" in the
UI) — no separate "Jewels" or other premium currency was added; platinum
was already designed for exactly this (IAP-sourced, ad-earnable) per its
own doc comment, it just didn't have a UI home yet. The Shop section
(`ui/shop/ShopContent.kt`, reachable from `FloatingMenu`) is that home now
— a balance display, the real spend path (permanent Speed/Profit boosts
and repeatable Time Skips — see the Boosts bullet under Tech stack above),
the real ad-earn path described above, and "buy outright" (IAP), still a
disabled "Soon" placeholder since billing isn't integrated yet.
**Only "Buy Platinum Pieces" (real money) is hidden for guests** — as of
0.18.1, "Watch an Ad" is open to everyone, guests included: it earns no
real money, so a guest losing that Platinum on reinstall isn't the kind of
loss the sign-in gate exists to prevent. "Buy Platinum Pieces" stays
behind `ShopContent`'s `isSignedIn` param (wired from
`GameViewModel.userEmail != null` in `MainActivity`) — a guest sees an
explanatory note there instead, since *that* purchase is real money and
should stay tied to a recoverable account. The Boosts section is
unaffected either way since spending Platinum already owned isn't a
real-money transaction. See Open Questions for what's still missing.

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
- **Prestige — Level Up** (renamed from "Molt"): resets the current
  hoard/lairs in exchange for **Scale Shards**, a permanent-bonus currency
  that boosts all future runs. Renamed because "Molt" only fit dragon-flavored
  lairs, not the goblins/orcs/etc. earlier in the catalog — "Level Up" is
  generic TTRPG language that fits every tier. Mechanic still not implemented
  (see open questions); the persisted field is still named
  `GameState.totalMolts` internally — rename it to match once Level Up is
  actually built, so the old name doesn't linger as a mismatch.
- **Art style:** vector/flat illustration, built with Compose (custom vector
  drawables + Compose Canvas for animation). No external sprite/asset-pack
  dependency.

Currency names, lair tiers/costs, whelp/wyrm collectible mechanics, leaderboard
scope, and exact number-formatting (large-number suffixes) are still open —
we'll pin these down as we build each system.

## Open questions / not yet decided

- Whelp/Wyrm collectible system mechanics (how it interacts with lairs)
- Full currency list — Gold Pieces and Platinum Pieces are wired into
  `GameState`; the premium-currency naming question is settled (it's
  Platinum, not a separate "Jewels" — see Monetization above), Platinum has
  a real spend path (Speed/Profit boosts, Time Skip — see the Boosts bullet
  under Tech stack), and now a real *earn* path too — the Shop's "Watch an
  Ad" (2 pp, 24h cooldown, 0.18.0). Still missing: "buy outright" (IAP) is
  still a disabled placeholder since billing integration doesn't exist yet.
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
- Avatar system — `GameHeader` has a `MedallionEmblem` slot (a carved
  gold-ringed medallion with an engraved shield silhouette, not yet an actual
  avatar) but no real avatar images or selection UI exist yet
- Creature portrait art — in progress, one lair at a time (see `LairRow`'s
  `lairPortraitRes` above): Kobold Warren, Giant Rat Burrow, and Bugbear
  Warcamp have real art as of v0.20.2; every other lair still shows
  `CreatureAvatar`'s rarity-tinted placeholder disc with the monster's first
  initial. Several already-generated candidates (`lair-goblin.png`,
  `lair-orc.png`, `lair-gnoll.png`, `lair-hobgoblin.png` in `/assets`,
  untracked) are sitting out because they were generated in a different,
  more painterly/realistic style that doesn't match the kobold/rat/bugbear
  art (which itself matches the established `coin.png`/`closed-chest.png`
  look: bold black outlines, semi-flat cel-shading, soft painted
  highlights) — regenerate those against that style before wiring them in,
  don't just drop them in as-is.
- Lair cost/income/timing for tiers 0–9 is sourced directly from AdVenture
  Capitalist's Earth Businesses (see `CreatureLairCatalog`); tiers 10–13 are
  our own extrapolation of the same patterns, still not playtested
- Manual upgrade shop (spending gold on a chosen boost) — not implemented.
  The *automatic* ownership-milestone multipliers this was originally
  tracking are done (see `domain/model/Milestone.kt`, above) — this is only
  about a separate, player-chosen upgrade purchase on top of those
- Leaderboard scope (global hoard value? fastest Level Up? per-lair records?)
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
