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

## Workflow rules

These apply to every change made in this repo, however small:

1. **Bump the version every change**, using `A.B.C` (Major.Minor.Patch) semantic
   versioning in [app/build.gradle.kts](app/build.gradle.kts) (`versionName`, and
   increment `versionCode` by 1 each bump):
   - **Patch (A.B.C → A.B.C+1):** bug fixes, tuning, small tweaks, refactors.
   - **Minor (A.B.C → A.(B+1).0):** new features/systems added, backward-compatible.
   - **Major ((A+1).0.0):** breaking save-data changes, ground-up reworks, or the
     jump from pre-release (0.x.x) to first stable release (1.0.0).
   - Current version: **0.1.0** (initial scaffold — see [CHANGELOG.md](CHANGELOG.md)).
2. **Log every change in [CHANGELOG.md](CHANGELOG.md)**, newest entry on top, in
   plain simplified language (what changed, not a diff dump), with a date and
   time in US Eastern (EST/EDT) for each entry.

## Tech stack & architecture

**Pattern:** MVVM + Clean Architecture, DI via Hilt.

- **Presentation (UI):** Jetpack Compose screens/composables. ViewModels expose
  `StateFlow` to the UI. `@HiltViewModel` throughout.
  - `GameViewModel` — main game state, lair purchases, upgrades
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
  businesses. Each lair is a themed monster den (e.g. Goblin Den, Troll Cave,
  Dragon Roost) that passively produces gold over time. Tapping a lair gives a
  manual production boost. Whelps/Wyrms are a separate collectible/pet system
  layered on top of the lair economy (details TBD as we build it out).
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
- Full currency list (gold + premium currency name, any specialty currencies)
- Large-number formatting convention (e.g. suffixes vs. scientific notation)
- Leaderboard scope (global hoard value? fastest Molt? per-lair records?)
- Target device scope (phone-only vs. tablet/landscape support)
