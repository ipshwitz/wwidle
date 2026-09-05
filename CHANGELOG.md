# Changelog

All notable changes to Wyrm & Whelp: Idle Hoard, newest first. Dates/times are US
Eastern (EST/EDT). See [CLAUDE.md](CLAUDE.md) for the living architecture doc.

## [0.7.7] - 2026-09-04 11:17 PM EDT

- Widened the gap between stacked `FloatingMenu` items by 25% (4dp → 5dp).
- On an open `SectionOverlayCard`, moved the header sign from centered to
  top-start (left), keeping the close button at top-end (right) — both still
  straddle the card's top edge the same way, just on opposite corners instead
  of sharing the middle.
- Verified visually on the emulator: menu items have a touch more breathing
  room, and the section-card header sits at the left with the close button
  at the right, no overlap.

## [0.7.6] - 2026-09-04 11:12 PM EDT

- Fixed the real root cause of the header/close-button size mismatch from the
  previous release: the five `menu-*.png` sign images had a transparent
  margin baked into their canvas (1672x941) around the actual sign shape, so
  `SIGN_ASPECT_RATIO` (and everything derived from it — the header height,
  the close button's matching size) was computing against blank space
  instead of the visible art. This is also why menu items looked oddly far
  apart in the expanded menu despite a 4dp `spacedBy` — the invisible padding
  inside each image was adding real visual gap on top of it.
- All five sign images recropped tight to the art itself (1626x536, still
  verified transparent at the corners) and re-copied into `drawable-nodpi`;
  `SIGN_ASPECT_RATIO` updated to match.
- `SIGN_HEADER_HEIGHT` in `SectionOverlayCard` is now derived
  (`SIGN_HEADER_WIDTH / SIGN_ASPECT_RATIO`) instead of a hardcoded `Dp`, so
  it can't silently go stale like this again.
- Removed the manual left-offset on the header sign added last release to
  dodge the oversized close button — with both elements now sized correctly
  off the real art, they sit side by side with no overlap and no offset
  needed.
- Verified visually on the emulator: menu items now sit close together with
  normal spacing, and the header/close button in an open section card are
  properly sized, side by side, with no clipping.

## [0.7.5] - 2026-09-04 11:01 PM EDT

- Resized the `CloseButton` in `SectionOverlayCard` to match the header sign's
  height, and positioned it to straddle the card's top edge the same way as
  the sign (half over the scrim, half over the card) instead of sitting as a
  small icon fully inside the card.
- `CloseButton` now renders as a plain clickable `Image` instead of an
  `IconButton` — `IconButton` clips content to its own fixed 40dp
  touch-target box, which was silently cropping the button at the larger
  size. Added a `size` parameter (defaults to 32dp) so other call sites can
  keep the old small size.
- The header sign is now shifted left off dead-center so the enlarged close
  button doesn't overlap and clip the section title — caught from a
  screenshot showing "Help & Social" cut off behind the swords icon.
- Verified visually and functionally on the emulator: both elements now
  match heights and straddle the edge identically, the label is fully
  readable, and the close button still dismisses the card correctly.

## [0.7.4] - 2026-09-04 10:54 PM EDT

- Replaced the Material "X" glyph (`Icons.Default.Close`) used to dismiss
  overlay cards with a custom crossed-swords icon (`x.png`) in a shared new
  `CloseButton` composable — this is now the app's standard close control for
  any future "close this overlay/dialog" affordance, not just
  `SectionOverlayCard`.
- Verified `x.png`'s transparency by real corner-pixel alpha (not just file
  type) before wiring it in, per the established habit — corners fully
  transparent, swords opaque.
- Removed the now-unused `material-icons-core` dependency since nothing in
  the app references `Icons.*` anymore.
- Verified visually and functionally on the emulator: the close button
  renders with a genuinely transparent background (no white box) and still
  correctly dismisses an open section card back to the game.

## [0.7.3] - 2026-09-04 10:44 PM EDT

- Fixed "Coming soon…" rendering half-hidden underneath the sign header: the
  content's top padding was a flat 24dp guess, way less than the ~67.5dp the
  sign actually overlaps the card surface by. Now computed from the sign's
  real overlap (`SIGN_HEADER_HEIGHT / 2`) plus its own breathing room.
- Enlarged the overlay card from 85% to 92% of screen height, so it covers
  more of the game behind it.
- Added consistent breathing room on every edge: 20dp start/end (was 16dp),
  24dp bottom (was none), so content never presses against the card or
  screen edges.
- Verified visually on the emulator: content now clears the sign with proper
  spacing, and the card noticeably covers more of the screen.

## [0.7.2] - 2026-09-04 10:36 PM EDT

- Every `SectionOverlayCard` now uses `woodenwall-1.png` (a tavern interior —
  shields, axes, torch, banner, mugs) as its background, behind the same
  50%-opacity white overlay used on the main game screen, instead of a flat
  Material surface color.
- Generalized `AppBackground` to take an `imageRes` parameter (defaults to
  `main_bg`, `GameScreen`'s landscape) instead of always using the landscape
  art, so `SectionOverlayCard` could reuse the same background-plus-overlay
  composable with a different image rather than duplicating the pattern.
- Verified visually on the emulator: the tavern wall renders correctly at
  50% opacity within the card's rounded shape, and the sign header still
  straddles the top edge on top of it as before.

## [0.7.1] - 2026-09-04 10:26 PM EDT

- Each `SectionOverlayCard` now headers with the same wooden-sign art shown
  on its `FloatingMenu` item (label already baked into the image) instead of
  a plain text title — opening "Level Up" shows the "Level Up" sign, opening
  "Help & Social" shows that sign, etc. Settings (no art yet) still falls
  back to a plain bold title.
- Refined the header placement per feedback: the sign now straddles the
  card's top edge like a hanging plaque rather than sitting inside the card
  — the card surface is inset from the top by half the sign's height, and
  the sign sits at the very top of the surrounding box, so its top half
  reads as outside the card (over the scrim) and its bottom half overlaps
  the card surface.
- Verified via the emulator's accessibility dump (screencap stayed blank
  through two full restarts this session): the sign's element bounds span
  355px, and the card surface begins exactly half that span down from the
  sign's top — confirming the intended 50/50 overlap split precisely.

## [0.7.0] - 2026-09-04 10:07 PM EDT

- Tightened the floating menu's item spacing to half its previous gap
  (`Arrangement.spacedBy(4.dp)`, was `8.dp`).
- Replaced full-screen navigation with slide-up overlay cards: tapping a
  `FloatingMenu` item no longer navigates to a separate "Coming Soon" screen
  — it opens a `SectionOverlayCard` that slides up from the bottom to cover
  85% of the screen (rounded top corners, scrim behind it, the game still
  visibly mounted and dimmed above/underneath), with a close `X` in the
  top-right corner. Tapping the scrim or pressing Back also dismisses it.
  The game screen — and its running `GameEngine` tick loop — is never
  actually left, so there's no more "how do I get back?" moment.
- Removed Navigation Compose entirely (dependency, `ui/navigation/Routes.kt`,
  `ComingSoonScreen.kt`, `NavHost`/`NavController` in `MainActivity`) — with
  every section now an overlay card instead of a real screen, there was
  nothing left for a nav graph to route between. Re-added
  `material-icons-core` (removed two versions ago as unused) for the card's
  close icon.
- Verified visually on the emulator: tighter spacing, the card sliding up
  over the dimmed game screen, and the close button correctly returning to
  the game with the menu collapsed.

## [0.6.4] - 2026-09-04 09:55 PM EDT

- Wired up real art for 5 of the 6 floating-menu items: Help & Social,
  Unlocks, Upgrades, Stewards, and Level Up now render as wooden trail-sign
  images (label already baked into the art) instead of plain text planks.
  Settings has no art yet and still falls back to the plain labeled surface.
  Verified all 5 source PNGs actually had transparent backgrounds (alpha=0 at
  every corner) before copying them in, per the new checklist item in
  CLAUDE.md — `file` reporting "RGBA" only means an alpha channel exists, not
  that it's used, which is exactly how open-chest.png's first export slipped
  through opaque a few versions ago.
- `floatingMenuItems` is now a list of `MenuItem(label, imageRes?)` instead
  of plain strings, so items can mix real art and text-fallback planks.
- Verified visually on the emulator: all 5 signs render with correct
  transparency (irregular wood edges visible against the lair cards behind
  them, no white box), and tapping one still navigates to its placeholder
  screen correctly.

## [0.6.3] - 2026-09-04 09:43 PM EDT

- Fixed the white box behind the chest icons: it wasn't coming from the PNGs
  (both are properly transparent now) — Material3's `FloatingActionButton`
  always draws its own solid container and shadow underneath its content,
  regardless of what's inside it. Swapped it for a plain transparent-
  background `IconButton`, so only the chest art itself is visible, floating
  directly on the background art.
- Verified functionally via `uiautomator` (toggle still flips correctly, menu
  still opens) — the emulator's screencap bug was back this session and
  didn't clear even after a restart, so no fresh screenshot this round.

## [0.6.2] - 2026-09-04 09:37 PM EDT

- Re-synced `drawable-nodpi/open_chest.png` with the now-transparent
  `assets/open-chest.png` (resaved with a real alpha channel). The FAB's
  open-chest state now blends into the button the same way the closed-chest
  state already did, instead of showing a near-white square.
- Verified visually on the emulator — both collapsed and expanded FAB states
  now render consistently.

## [0.6.1] - 2026-09-04 09:31 PM EDT

- Replaced the floating menu's generic hamburger/X icon with real art:
  `assets/closed-chest.png` and `open-chest.png`, copied into
  `drawable-nodpi/` as `closed_chest.png`/`open_chest.png`. The FAB now shows
  a closed treasure chest normally and an open one while the menu is
  expanded. Removed the now-unused `material-icons-core` dependency that
  only existed for the old `Icons.Default.Menu`/`Close` glyphs.
- Noted for later: `closed-chest.png` has a real transparent background,
  but `open-chest.png` is fully opaque with a near-white background (no
  alpha channel) — worth a transparent re-export if that ever needs to
  blend into the FAB rather than show as a near-white square.
- Verified functionally via `uiautomator` (content-desc correctly flips
  "Open menu" → "Close menu" on tap, menu items render) — the emulator's
  screencap bug (documented in the build environment notes) was back this
  session, even after a full emulator restart, so no fresh screenshot this
  round.

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
