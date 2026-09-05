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
  dimensions are, not be left as a stale copy-pasted value. Settings has no
  art yet and still falls back to a plain labeled surface. `woodenwall-1.png`
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
  placeholder it launched with.
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
   - Current version: **0.14.0** (permanent Speed/Profit boosts and Time Skip
     — Platinum Pieces' real spend path — see [CHANGELOG.md](CHANGELOG.md)).
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

## Tech stack & architecture

**Pattern:** MVVM + Clean Architecture, DI via Hilt.

- **Presentation (UI):** Jetpack Compose screens/composables. ViewModels expose
  `StateFlow` to the UI. `@HiltViewModel` throughout.
  - `GameViewModel` — implemented (`ui/game/GameViewModel.kt`): wraps
    `GameEngine`, starts its tick loop and settles offline earnings once on
    creation, exposes claim/hire-Steward/plunder actions plus a `buyQuantity:
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
    incomePerCycle(count) / baseProductionSeconds` across owned lairs) — a
    theoretical rate independent of Steward status, matching how idle games
    typically show this stat.
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
    `CreatureAvatar` is a placeholder (no monster portrait art exists yet, no
    files dropped into `/assets` for this): a rarity-tinted radial-gradient
    disc with a carved border and the monster's first letter in serif type —
    not unique per creature (a few tiers share an initial) but the rarity
    color band and the full name in the card right next to it already
    disambiguate, and this is explicitly a stand-in, not a real icon system.
    Tapping the avatar plunders the lair exactly like tapping the card: both
    share one hoisted `plunder` lambda (bumps `coinBurstTrigger`, then calls
    `onPlunder`) owned by `LairRow`, passed into `LairCard` as a plain `Int`
    parameter instead of `LairCard` keeping that counter as local `remember`
    state — needed so both tap targets fire the same `CoinBurstOverlay`
    (which still only renders over the card, not the avatar).
  - `LairCard` styling: no Material `Card` — a custom `Box` sized via
    `Modifier.height(IntrinsicSize.Min)` so a second, fractionally-widthed Box
    can render *behind* the text/buttons as a left-to-right fill representing
    `cycleProgressSeconds / baseProductionSeconds` (100% = ready to plunder).
    Restyled to match `GameHeader`'s cozy-fantasy chrome (was flat rarity-
    colored blocks with a Material `Button`/`OutlinedButton` pair, which read
    as "boring" against the rest of the game): a translucent parchment
    gradient base (`FantasyPalette.parchmentShade`/`parchment`, alpha 0.55 —
    sheer enough that `GameScreen`'s background art still shows through, same
    as before) plus a faint per-tier "rarity" tint (`rarityColor(tier)`, the
    same 5-band green→blue→purple→orange→gold ramp as always) over the whole
    card, with a stronger rarity gradient for the claimed-fraction fill and a
    bright 2px line at the fill's own trailing edge (drawn via `drawBehind`
    *on the fill `Box` itself*, at its own right edge — since that edge
    already sits exactly at the animated fraction, no extra position math
    needed). `FontFamily.Serif` for the name (matching `GameHeader`), italic
    muted text for monster/CR, bold `goldDeep`-colored text for the income
    line. The fill's target value is wrapped in `animateFloatAsState` (linear
    easing, duration = `GameEngine.TICK_INTERVAL_MS`) — `GameEngine` only
    pushes a new value every tick, which reads as visible steps without this;
    animating linearly across that same window turns it back into continuous
    motion. Keep the two in sync if either changes. The Claim button is now
    the shared `WoodenButton` instead of a Material `Button`. **The Steward
    button is gone** — hiring a Steward now lives solely in the Stewards
    menu section (see `StewardsContent` below), not on every card;
    `LairCard` no longer takes `onHireSteward`.
  - **`CoinBurstOverlay`** (`ui/game/CoinBurst.kt`) — a one-shot radial burst
    of small gold coins (plain `Canvas`-drawn circles with a darker rim, per
    the stated art style — no sprite asset) fired only on a manual plunder
    tap (from either the card or its avatar — see `LairRow` above), not a
    Steward's automatic collection, since both tap targets go through the
    same hoisted `coinBurstTrigger` counter (owned by `LairRow`, incremented
    right before calling `onPlunder`) — a Steward's auto-collect runs inside
    `GameEngine`'s tick loop and never touches either click handler. Uses a
    counter rather than a boolean so a
    second plunder mid-animation restarts the effect (`key(trigger)` tears
    down and relaunches the old one) instead of being a no-op. Rendered as
    the last child in `LairCard`'s `Box` (`Modifier.matchParentSize()`, no
    pointer input) so it draws over the card's content without blocking taps
    on it. Clips to the card's own rounded-rect bounds like everything else
    in that `Box` — coins bursting past the edge just get clipped there,
    which reads fine at this card size and duration (~650ms).
  - `AuthViewModel` — Supabase auth
  - `SettingsViewModel` — user preferences
  - `ConsentViewModel` — privacy/ad consent
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
    art already has its label baked in); Settings (no art yet) falls back to
    a plain labeled `Surface`. Tapping any item collapses the menu and calls
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
    competing with it for the same space — header left, close right. Settings
    (no art yet) falls back to a plain bold title with no overlap/inset.
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
  - **Milestones** (`domain/model/Milestone.kt`) — `MILESTONE_STEPS` is the
    shared ownership-count ladder (25/50/100/200/300/400 each ×2, then 500
    ×4, 1,000 ×5, 5,000 ×6, 10,000 ×7), applied two ways, both compounding
    every rung reached into one running multiplier
    (`milestoneMultiplierFor`):
    - **Individual** — `CreatureLair.individualMilestoneMultiplier(unitsOwned)`,
      keyed on that one lair's own owned count (100 Kobold Warrens is its
      own 8x, independent of every other lair).
    - **"Everything"** — `GameState.globalMilestoneMultiplier(catalog)`
      (`domain/model/GameStateExtensions.kt`), keyed on the *lowest* owned
      count across every lair in `catalog` — every lair has to reach a rung
      before the bonus for it applies to all of them, not just whichever
      lair is furthest ahead.
    Both multipliers feed into `CreatureLair.incomePerCycle(unitsOwned,
    globalMultiplier = 1.0)` (`baseIncomeGp * unitsOwned *
    individualMilestoneMultiplier(unitsOwned) * globalMultiplier`) — every
    caller that credits or previews income (`GameEngine.advance`/
    `advanceLair`/`plunderLair`, `LairCard`'s "gp/cycle" text, `GameScreen`'s
    gold-per-second sum) computes the global multiplier once via
    `state.globalMilestoneMultiplier(...)` and threads it through; callers
    that don't pass one (existing tests, mainly) get the no-bonus default of
    1.0. `nextMilestoneThreshold(unitsOwned)` (smallest rung still ahead, or
    null past 10,000) is what `BuyQuantity.NEXT` targets.
  - **`UnlocksContent`** (`ui/unlocks/UnlocksContent.kt`) — the Unlocks
    section's real content (see `SectionOverlayCard` above): one row per
    milestone *rung* actually reached, not a compressed per-lair summary —
    owning 50 Kobold Warrens shows two separate "Kobold Warren — 25 owned"
    and "Kobold Warren — 50 owned" rows (each labeled with what that rung
    grants, e.g. "Profit Speed Doubled"), and the "Everything" ladder gets
    the same one-row-per-rung treatment rather than a single status card.
    Built by flattening `MILESTONE_STEPS` against each lair's owned count
    (and against the global "Everything" minimum) into a flat list, so nothing
    here is a preview of what's ahead — a rung simply doesn't appear in the
    list until it's actually been crossed. A save with nothing unlocked at
    all shows a "No milestones unlocked yet…" placeholder instead of an
    empty screen. Pure display — takes `lairs`/`state` passed in by
    `WyrmWhelpApp` (which already holds the `GameViewModel` reference)
    rather than taking a ViewModel itself. Styled with the same parchment
    `ParchmentCard`s as `StewardsContent` (this file used to have its own
    older plain-Material-`Surface` look from before the cozy-fantasy
    restyle; both screens now share the same look, though each still
    defines its own private `ParchmentCard` copy rather than a shared one —
    worth factoring out if a third section needs it).
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
    one row each for Speed Boost, Profit Boost, and Time Skip, then an "Earn
    Platinum" section with the original two "earn more" rows (watch a
    rewarded ad, or buy outright via IAP), both still a disabled
    `WoodenButton` labeled "Soon" since neither ads nor billing are wired up
    yet. `FloatingMenu`'s `"Shop"` entry (no sign art yet, same plain-`Surface`
    fallback as Settings) reaches it. Takes `platinumPieces`/`speedBoostLevel`/
    `profitBoostLevel` plus `onBuySpeedBoost`/`onBuyProfitBoost`/
    `onBuyTimeSkip` callbacks — `WyrmWhelpApp` wires the callbacks straight to
    the matching `GameViewModel` methods, same pattern as `StewardsContent`'s
    `onHireSteward`.
  - **Boosts** (`domain/model/Boosts.kt`) — permanent, account-wide bonuses
    bought with Platinum Pieces, *not* tied to any one lair (unlike the
    ownership milestones): Speed Boost (10 pp base, ×1.5 cost growth/level,
    +5% production speed/level, compounding) and Profit Boost (same cost
    curve, +10% income/level, compounding), plus a flat-cost repeatable Time
    Skip (5 pp, instantly grants `TIME_SKIP_SECONDS` — 1 hour — of production
    via the same `GameEngine.advance()` logic offline earnings use). Levels
    live on `GameState.speedBoostLevel`/`profitBoostLevel`.
    `CreatureLair.incomePerCycle` takes a third `profitBoostMultiplier`
    parameter (default 1.0) alongside the existing global-milestone one, and
    a new `CreatureLair.effectiveProductionSeconds(speedBoostMultiplier)`
    (default 1.0, divides `baseProductionSeconds`) replaces raw
    `baseProductionSeconds` everywhere a lair's actual cycle time matters —
    `GameEngine.advance`/`advanceLair` (both the unmanaged-lair readiness
    check and the Steward auto-collect loop), `LairCard`'s progress-bar
    fraction, and `GameScreen`'s gold-per-second sum. `GameEngine` exposes
    `purchaseSpeedBoost()`/`purchaseProfitBoost()`/`purchaseTimeSkip()`
    (each: check affordability, deduct Platinum, apply); `GameViewModel` has
    matching thin wrappers, same shape as `claimLair`/`hireSteward`.
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
    the database version is bumped by 1 (currently 2, for the Boosts
    feature's two new `GameStateEntity` columns) any time a persisted field
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
- **DI bindings:** `@Singleton` for `GameEngine`, `AdManager`, `ConsentManager`;
  `@HiltViewModel` for all ViewModels; `@ApplicationContext` injected where needed;
  `@Inject constructor` throughout.

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
eligibility from the first session. They can later link email/Google to carry
progress across devices and reinstalls without losing anonymous-session progress
(linking not implemented yet — currently every install/reinstall is a brand new
anonymous identity with no way to recover a previous one).

**Project config:** `SUPABASE_URL` / `SUPABASE_ANON_KEY` live in `local.properties`
(gitignored, not committed) and are exposed to the app via `BuildConfig` fields
(`app/build.gradle.kts` reads `local.properties` at build time). A fresh clone
needs these two lines added to its own `local.properties` before cloud sync will
work — ask whoever has the Supabase project for the values, or create a new
Supabase project and run the scripts in `/SQL` against it.

### Monetization

Free-to-play: rewarded ads (boosts, offline-earnings multipliers) + optional IAP
(gems, time-skips, cosmetics). No forced interstitials. The premium currency is
`GameState.platinumPieces` (labeled "pp" in the UI) — no separate "Jewels" or
other premium currency was added; platinum was already designed for exactly
this (IAP-sourced, ad-earnable) per its own doc comment, it just didn't have a
UI home yet. The Shop section (`ui/shop/ShopContent.kt`, reachable from
`FloatingMenu`) is that home now — a balance display, the real spend path
(permanent Speed/Profit boosts and repeatable Time Skips — see the Boosts
bullet under Tech stack above), and "watch an ad" / "buy outright" earn
entry points, both still disabled ("Soon") since neither ads nor billing are
integrated yet. See Open Questions for what's still missing.

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
  Platinum, not a separate "Jewels" — see Monetization above), and Platinum
  now has a real spend path (Speed/Profit boosts, Time Skip — see the
  Boosts bullet under Tech stack). Still missing: a real *earn* path — the
  Shop screen's "watch an ad" / "buy outright" buttons are still disabled
  placeholders since no ad network or billing integration exists yet, so
  Platinum can currently only be granted by direct save editing
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
- Creature portrait art — `LairRow`'s `CreatureAvatar` (the circle next to
  each lair card) is a rarity-tinted placeholder disc with the monster's
  first initial, not real art; no monster portraits have been dropped into
  `/assets` yet. Swap `CreatureAvatar`'s drawing for real per-monster images
  once they exist
- Lair cost/income/timing for tiers 0–9 is sourced directly from AdVenture
  Capitalist's Earth Businesses (see `CreatureLairCatalog`); tiers 10–13 are
  our own extrapolation of the same patterns, still not playtested
- Manual upgrade shop (spending gold on a chosen boost) — not implemented.
  The *automatic* ownership-milestone multipliers this was originally
  tracking are done (see `domain/model/Milestone.kt`, above) — this is only
  about a separate, player-chosen upgrade purchase on top of those
- Leaderboard scope (global hoard value? fastest Level Up? per-lair records?)
- Target device scope (phone-only vs. tablet/landscape support)
- Cloud sync only happens once per app launch (right after anonymous sign-in);
  no manual button, Level Up trigger, or backgrounding push yet — a long
  session with no restart won't push its progress to the cloud until it's
  closed and reopened. Add more triggers once Settings/Level Up exist.
- Anonymous identity isn't recoverable — no email/Google linking yet, so a
  reinstall or a cleared app means a brand new (empty) cloud identity, not a
  restored one.
