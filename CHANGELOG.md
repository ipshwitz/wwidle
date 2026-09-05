# Changelog

All notable changes to Wyrm & Whelp: Idle Hoard, newest first. Dates/times are US
Eastern (EST/EDT). See [CLAUDE.md](CLAUDE.md) for the living architecture doc.

## [0.6.0] - 2026-09-04 09:21 PM EDT

- Added the app's first navigation menu: a floating hamburger FAB fixed at
  the bottom of the screen that expands upward into a vertical stack of
  tappable sections — Help & Social, Unlocks, Upgrades, Stewards, Level Up,
  and Settings — each its own labeled container for now (real per-item art
  to come later). Evokes the wooden trail signpost in the background art.
  Tapping a section navigates to a shared "Coming Soon" placeholder screen,
  since none of those sections have a real screen yet.
- Wired up real navigation for the first time: added Navigation Compose with
  type-safe routes (`GameRoute`, `ComingSoonRoute`), one `NavController`/
  `NavHost` owned by `MainActivity`, with the floating menu overlaid above
  it so it persists across every screen.
- Renamed the prestige mechanic from "Molt" to "Level Up" — "Molt" only made
  sense for dragon-flavored lairs, not the goblins/orcs/etc. earlier in the
  catalog. Still not implemented; the persisted field is still named
  `totalMolts` internally until Level Up actually gets built.
- Factored `GameScreen`'s background-art treatment out into a shared
  `AppBackground` composable so the new placeholder screens look visually
  consistent instead of defaulting to a plain white background.
- Hit the same compileSdk-36/AGP-8.13.2 dependency ceiling as before, this
  time via `navigation-compose` (2.10.0 pulls in `lifecycle-*:2.11.0`,
  requiring compileSdk 37) — used 2.9.4 instead, which pairs with our
  existing lifecycle 2.9.3 pin.
- Verified visually on the emulator: collapsed and expanded menu states, and
  navigating from the expanded menu to a placeholder screen and back.

## [0.5.5] - 2026-09-04 06:58 PM EDT

- Fixed the real bug behind two related reports ("the fill doesn't restart
  from 0%, more like 25%" and "repeated tapping throws off the cycle
  animation"): `GameEngine`'s tick loop measures `deltaSeconds` from the
  *previous* tick regardless of when a plunder reset a cycle in between, so
  every reset was immediately overshot by up to one full tick interval on the
  very next tick. At the old 200ms interval that was a third of Kobold
  Warren's 0.6s cycle — both symptoms were this same overshoot, just more
  obvious the faster you cycle through resets. Shrunk `TICK_INTERVAL_MS` to
  33ms, cutting the worst-case overshoot to ~5.5% (and under 2% for every
  lair with a longer cycle) — small enough to read as a clean start.
- Verified with two screen recordings sent for direct comparison: normal
  cycling, and rapid repeated tapping on Kobold Warren (the fastest, most
  exposed case). Confirmed via UI dump that rapid tapping doesn't cause
  double-collection — gold accumulated exactly matches legitimate plunders
  for the elapsed time.

## [0.5.4] - 2026-09-04 05:14 PM EDT

- Fixed choppy lair-card fill animation: `GameEngine` only updates state
  every 200ms (`TICK_INTERVAL_MS`), so the fill was visibly stepping instead
  of flowing. It now animates linearly across that same 200ms window
  (`animateFloatAsState`), turning the discrete updates back into continuous
  motion. Made `TICK_INTERVAL_MS` public so the UI reads it directly instead
  of duplicating the number.
- Confirmed each lair has its own production time that scales up with tier
  (0.6s for Kobold Warren through multi-hour for the endgame lairs, per the
  AdCap-derived tuning from v0.5.1) — that part was already working as
  intended, the choppiness was purely an animation issue.
- Sent a short screen recording (repeated Kobold Warren cycles) for visual
  confirmation, since a static screenshot can't show animation smoothness.

## [0.5.3] - 2026-09-04 05:06 PM EDT

- Redesigned the lair cards: more compact (tighter padding, smaller buttons,
  trimmed the redundant status line), colored by a 5-band "rarity" ramp
  across the catalog's tiers (green → blue → purple → orange → gold), and
  translucent so the background art shows through every card, not just the
  gaps between them.
- The whole card is now the production-cycle timer: instead of a thin
  progress strip, a same-colored fill grows left-to-right behind the text
  from 0% to 100% of the cycle. At 100% (ready to plunder) the card is fully
  "loaded" and tappable; tapping collects and the fill drains back to 0% to
  start the next cycle, exactly mirroring the existing tap-gating logic (no
  engine/ViewModel changes — this was a pure `LairCard` rewrite).
- Verified visually on the emulator — screenshot tooling is working again
  this session.

## [0.5.2] - 2026-09-04 04:59 PM EDT

- Added the lair screen's background art: `assets/main-bg.png` (a fantasy
  village/castle landscape, signposted "Goblin Cave" and "Dragon Peak" —
  thematically on the nose) now renders behind the lair list, copied into
  `app/src/main/res/drawable-nodpi/main_bg.png` for actual use from Compose.
  A 50%-opacity white overlay sits between the art and the UI so it stays
  atmospheric without competing with the lair cards for attention. Both the
  top bar and Scaffold background are transparent so the art shows through
  everywhere, not just around the cards.
- Verified visually on the emulator — screenshots that had been reliably
  blank all session (see build environment notes) started working again
  partway through this change, unexplained; confirmed the art and overlay
  render correctly and the lair list/top bar are still fully legible on top.

## [0.5.1] - 2026-09-04 04:52 PM EDT

- Rebalanced the whole lair economy to match AdVenture Capitalist's real
  Earth Businesses: Kobold Warren through Troll Warren (tiers 0-9) now use
  AdCap's actual Lemonade Stand → Oil Company costs, cost-growth rates, cycle
  times, income, and manager prices 1:1 (gold pieces for dollars). Wyvern
  Aerie through the Ancient Dragon's Hoard (tiers 10-13, beyond what Earth
  has) extend the same ~12x-cost-per-tier and ~50%-income-to-cost patterns,
  with gentler cycle-time growth since we don't have AdCap's repeated
  prestige resets yet to claw long cycle times back down.
- Matched AdCap's actual onboarding too: a new save now starts owning one
  Kobold Warren already (instead of 0 lairs + a flat starting-gold grant),
  mirroring AdCap's free starting Lemonade Stand.
- Verified on an emulator: fresh install shows the exact expected numbers
  (Giant Rat Burrow "Claim — 60 gp", Goblin Camp "Claim — 720 gp", etc.,
  matching AdCap's published values), and plundering the starting Kobold
  Warren correctly grants 1 gp.

## [0.5.0] - 2026-09-04 04:38 PM EDT

- Added Supabase cloud sync: anonymous sign-in on launch, then downloads the
  cloud save and merges it against the local one (whichever save is more
  progressed wins — higher Molt count first, then net worth), loads the
  winner, and re-uploads it after offline earnings settle.
- Every network step degrades gracefully to local-only play if it fails
  (network issue, Supabase misconfigured, etc.) instead of crashing or
  blocking — confirmed by testing with anonymous sign-in still disabled
  before flipping it on.
- Added `/SQL` (repo root) for sequentially-numbered database scripts —
  `SQL/001_create_cloud_saves_table.sql` creates the `cloud_saves` table with
  row-level security so each player can only read/write their own save.
  (Supersedes the one-off `supabase/schema.sql` from earlier — same content,
  new home/convention.)
- Config: `SUPABASE_URL`/`SUPABASE_ANON_KEY` go in `local.properties`
  (gitignored) and are exposed to the app via `BuildConfig`.
- Verified for real against the live Supabase project — not just "it builds":
  pulled the app's own session token off the emulator (`run-as` on the debug
  build) and queried the `cloud_saves` table directly over REST, confirming a
  claimed lair and updated gold correctly reached the actual database and
  came back through RLS as expected.

## [0.4.0] - 2026-09-04 04:09 PM EDT

- Added local save persistence with Room: `GameStateEntity` (currencies/meta)
  + `OwnedLairEntity` (one row per claimed lair), wrapped by
  `RoomGameRepository` behind a new domain-layer `GameRepository` interface.
  `GameViewModel` now loads the save on startup (before settling offline
  earnings or starting the tick loop) and autosaves every 30 seconds.
- Fixed a real bug this surfaced during testing: a brand-new save started
  with 0 gold, but every lair costs gold to claim — there was no way to ever
  get started. New saves now begin with 25 gp, enough to claim the first
  Kobold Warren.
- Verified end-to-end on an emulator: claimed a lair, waited past the
  autosave interval, force-stopped the app, and relaunched — gold and owned
  lair count both survived, confirmed via `uiautomator dump` (screenshots
  still blocked by the environment issue noted below).

## [0.3.1] - 2026-09-04 03:59 PM EDT

- Added a `/assets` folder at the repo root for source game assets (logos,
  background art, etc.), documented in CLAUDE.md.

## [0.3.0] - 2026-09-04 03:40 PM EDT

- Built `GameViewModel` (`@HiltViewModel`): starts `GameEngine`'s tick loop and
  settles offline earnings once on first creation (never stops the engine in
  `onCleared`, since it's an app-scoped singleton that outlives any one
  screen), and exposes actions for claiming a lair, hiring a Steward, and
  plundering a finished cycle.
- Built the first real screen: `GameScreen`/`LairCard`/`WelcomeBackDialog`
  (Compose, Material3) replacing the default template UI in `MainActivity`.
  Shows the full lair list with owned count, claim/Steward-hire buttons,
  production progress, and a "while you were away" dialog for offline
  earnings.
- Added `GoldFormat`, a first-pass large-number display formatter
  (K/M/B/T/Qa/... suffixes) — a placeholder answer to the still-open
  number-formatting question now that there's a UI to display gold in.
- Verified via a full debug build + install on an Android emulator: confirmed
  correct via the live accessibility-tree dump (exact expected lair names/
  costs/owned counts present), though actual pixel screenshots were blocked
  by a screencap/screenrecord bug on this host's emulator setup (see
  CLAUDE.md build environment notes).

## [0.2.0] - 2026-09-03 08:28 PM EDT

- Wired up Hilt + KSP (app-scoped DI) and connected the local repo to
  [github.com/ipshwitz/wwidle](https://github.com/ipshwitz/wwidle).
- Built the Creature Lair/Den data model (`CreatureLair`, `OwnedLair`,
  `GameState`) and a 14-tier `CreatureLairCatalog` spanning D&D 5E SRD
  monsters from Kobold (CR 1/8) up to the Ancient Red Dragon (CR 24), each
  with its own claim cost, gold income, production cycle time, and Steward
  (auto-collector) hire cost.
- Built `GameEngine`: the app-scoped singleton tick loop that advances lair
  production, handles claiming lairs, hiring Stewards, manually plundering a
  finished cycle, and settling offline earnings on load (capped by
  `offlineCapHours`). Unmanaged lairs cap at one completed cycle waiting for a
  tap; Stewarded lairs auto-collect every cycle, online or offline.
- Added unit tests covering claim cost/affordability, unmanaged-vs-Stewarded
  production, plundering, and the offline-earnings cap.
- Fixed two pre-existing scaffold dependency versions (`androidx.core-ktx`,
  `androidx.lifecycle-runtime-ktx`) that required a newer Android Gradle
  Plugin than this project uses, which was blocking any build.

## [0.1.0] - 2026-09-03 08:10 PM EDT

- Set up project tracking: added `CLAUDE.md` (architecture/decisions reference)
  and this changelog.
- Locked in initial architecture: MVVM + Clean Architecture, Hilt DI, Room for
  local save data, Supabase for auth/cloud sync/leaderboards (anonymous-first
  auth), DataStore for preferences.
- Decided core game design direction: gold generators are themed Creature
  Lairs/Dens (Adventure Capitalist–style), prestige mechanic is "Molt" (reset
  for permanent Scale Shards), art style is vector/flat illustration in Compose,
  monetization is free-to-play with rewarded ads + optional IAP.
- Confirmed all naming going forward uses Dungeons & Dragons theming (no reused
  terminology from other projects).
