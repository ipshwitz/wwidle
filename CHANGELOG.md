# Changelog

All notable changes to Wyrm & Whelp: Idle Hoard, newest first. Dates/times are US
Eastern (EST/EDT). See [CLAUDE.md](CLAUDE.md) for the living architecture doc.

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
