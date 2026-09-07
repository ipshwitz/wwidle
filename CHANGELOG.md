# Changelog

All notable changes to Wyrm & Whelp: Idle Hoard, newest first. Dates/times are US
Eastern (EST/EDT). See [CLAUDE.md](CLAUDE.md) for the living architecture doc.

## [0.31.2] - 2026-09-06 9:40 PM EDT

- Redesigned the "While You Were Away" pop-up: the bottom half now shows
  a hand-illustrated magical TV with the "Watch Ad to Double" prompt
  underneath it, instead of just a plain button.

## [0.31.1] - 2026-09-06 9:25 PM EDT

- Added real portrait art for Goblin Camp, Orc Encampment, and Gnoll Den —
  they now show their actual creature art on the main screen instead of
  the plain letter placeholder.

## [0.31.0] - 2026-09-06 9:15 PM EDT

- Built out the "Help & Social" menu section (it just said "Coming soon"
  before): links to the game's real Facebook, Instagram, X, TikTok, and
  Whatnot accounts, the website, and a support email address — each with
  its own brand-styled icon. Tapping a row opens it in your browser (or
  your email app for the support address).

## [0.30.0] - 2026-09-06 6:40 PM EDT

- Added a quick-access button in the bottom-right corner of the main game
  screen for the same free Speed-boost ad watch that's in the Shop's
  Temporary tab — no need to open the Shop menu to find it. Shows a small
  badge with how many of the 4 daily watches are still available, and
  fades when none are left. Watching from here counts against the same
  daily watches as watching from the Shop.

## [0.29.0] - 2026-09-06 6:05 PM EDT

- Added a free way to get a Speed boost in the Shop's Temporary tab: watch
  an ad for a 2x Speed boost lasting 4 hours. It stacks with itself and
  with any paid Speed boost — up to 4 watches can be active at once (16x
  total), and each individual watch has its own 24-hour cooldown before it
  can be used again (so all 4 don't come back at the same time if watched
  back-to-back).

## [0.28.2] - 2026-09-06 10:45 PM EDT

- Added the game's version number to the bottom of the Settings screen.

## [0.28.1] - 2026-09-06 10:30 PM EDT

- Changed the two longest Time Skip labels to plain language — "1 day"
  instead of "24h" and "1 week" instead of "168h." The other four sizes
  (5m/30m/1h/12h) are unchanged.

## [0.28.0] - 2026-09-06 10:15 PM EDT

- Reorganized the Shop into four tabs instead of one long scroll:
  - **Get PP** (first/default) — "Watch an Ad" and "Buy Platinum Pieces"
    together, since both are about getting more Platinum.
  - **Permanent**, **Temporary**, and **Time Skips** — each of the three
    spend categories that used to just be sections in the long scroll now
    gets its own tab.
  - The Platinum balance stays visible above the tabs no matter which one
    you're on.

## [0.27.2] - 2026-09-06 9:50 PM EDT

- Devalued the Platinum Pieces IAP packs further — 100 pp for $0.99 made
  the currency feel worthless, so the $0.99 pack is now a deliberately
  stingy "teaser" instead of a real purchase:
  - $0.99 for 4 pp, $2.99 for 15 pp, $4.99 for 30 pp, $6.99 for 55 pp,
    $9.99 for 100 pp.
  - The cheapest pack can't even afford the cheapest permanent boost's
    first purchase on its own — it's meant to give a taste, not a real
    dent. The $9.99 pack is now the meaningful purchase.

## [0.27.1] - 2026-09-06 9:35 PM EDT

- Tuned down the "Buy Platinum Pieces" price range — $0.99 to $9.99
  instead of $0.99 to $49.99:
  - $0.99 for 100 pp, $2.99 for 330 pp, $4.99 for 600 pp, $6.99 for
    920 pp, $9.99 for 1,400 pp.
  - The old $49.99 pack's 7,000 pp was too much once checked against
    what things actually cost — enough to buy the single most expensive
    permanent boost tier seven times over in one sitting. The new top
    pack's 1,400 pp buys about five instead, a much more reasonable
    biggest-purchase-of-the-day size.

## [0.27.0] - 2026-09-06 9:10 PM EDT

- Added real "Buy Platinum Pieces" — the Shop's disabled "Soon" placeholder
  is now five actual Google Play Billing purchases:
  - $0.99 for 100 pp, $4.99 for 550 pp, $9.99 for 1,200 pp, $19.99 for
    2,600 pp, $49.99 for 7,000 pp. Bigger packs give a slightly better
    rate, but only mildly — the $49.99 pack is capped well short of
    "enough Platinum to never need more," on purpose.
  - Still gated to signed-in players, same as before — a guest sees the
    same explanatory note.
  - Each purchase is a one-time consumable — buy it, get the Platinum,
    buy it again later.
  - Fixed a cold-start slowdown this introduced during testing: connecting
    to Google Play Billing at app launch (the same way ads pre-load) added
    25-60+ extra seconds on this test device. The connection now only
    starts once the Shop is actually opened, which fixed it.
  - Not fully testable end-to-end yet — actually charging money requires
    setting these products up in the Google Play Console first, which
    hasn't been done.

## [0.26.0] - 2026-09-06 6:20 PM EDT

- Added Platinum Upgrades, filling in the Upgrades screen's Platinum tab
  and replacing the old Speed Boost/Profit Boost with a bigger system,
  all bought in the Shop:
  - **Permanent boosts:** nine named tiers you can buy over and over —
    2x/5x/10x Speed, 1.5x/2x/5x Profit, and 1.5x/2x/5x Gem % (raises how
    much income bonus each Gem is worth). Buying the same tier multiple
    times stacks — three purchases of "5x Speed" gives 125x from that
    tier alone — with each repeat purchase costing more Platinum than
    the last.
  - **Temporary boosts:** four instant-activation tiers — 50x/100x Speed
    for 5 minutes, 15x Profit for 10 minutes, 25x Profit for 5 minutes.
    Buying a second one of the same type while the first is still
    running stacks both together for as long as they overlap.
  - **Time Skips** grew from two sizes to six: 5 minutes, 30 minutes,
    1 hour, 12 hours, 24 hours, and 7 days.
  - Everything here is permanent — none of it resets when you Level Up,
    unlike Gold and Gem upgrades.
  - The Upgrades screen's Platinum tab now shows what you've bought
    (owned counts, combined multipliers, and a live countdown on any
    active temporary boost) — buying still only happens in the Shop.
  - Verified live on-device: a real permanent-boost purchase deducted
    the exact cost shown and immediately sped up production on the main
    screen, and a real temporary-boost purchase showed a live countdown
    in both the Shop and the Upgrades screen at the same time.

## [0.25.0] - 2026-09-06 2:50 PM EDT

- Added the Upgrades menu section — a permanent Gold/Gem sink on top of
  the existing ownership milestones, reachable from the floating menu
  between Unlocks and Stewards:
  - **Gold tab:** 475 tiers total across 30 lines — every lair gets its
    own Profit and Speed line (15 tiers each, 28 lairs), plus two
    "Everything" lines (Profit and Speed, 28/27 tiers) that boost every
    owned lair at once. Each line runs through three internal
    beginning/mid/end phases with a deliberate cost *and* effect jump at
    each phase boundary, not just smooth compounding — mirroring the
    lair catalog's own tier jumps.
  - **Gems tab:** a single 200-tier "Gem Efficiency" line that raises how
    much income bonus each currently-held Gem is worth (stacking on top
    of the flat 2%/Gem baseline). Gems spent here stop counting toward
    that per-Gem bonus, same as spending them any other way.
  - **Platinum tab:** left as a "Coming soon…" placeholder for now —
    intentionally not built yet.
  - Every Gold- and Gem-funded upgrade level resets on your next Level
    Up, same as the currency that bought it — only Platinum-funded
    upgrades (not built yet) will carry forward. Room bumped to database
    version 8 for the five new persisted fields (a lair's own
    Profit/Speed upgrade level, plus the two Everything levels and Gem
    Efficiency level).
  - Verified live on-device: opening each tab renders correctly, a real
    Gold purchase (Kobold Warren's Profit line) deducted the exact
    373.8 gp shown and moved the line to Lv 1/15 with an updated
    next-tier cost, and a real Gem purchase moved Gem Efficiency to
    Lv 1/200 for 5 Gems with the per-Gem bonus display updating from
    +2.0% to +2.1% income.

## [0.24.0] - 2026-09-06 12:31 PM EDT

- Redesigned Gems from an accumulating prestige currency into a
  temporary one, matching AdVenture Capitalist's real Angel Investors
  (they boost how fast a run ramps up, they don't stack forever):
  - Every Level Up now *replaces* your Gem balance with a fresh batch
    sized off your lifetime earnings, instead of adding to whatever you
    already had. A bigger batch means a faster-ramping next run, not a
    bigger permanent stockpile — that will matter once there's a
    leaderboard comparing how fast players build up, not how many Gems
    they've banked.
  - Removed the internal "Gems ever earned" ledger this replaces — it
    existed to stop a Level Up from re-granting Gems you'd already
    earned, but that's no longer meaningful once Gems don't accumulate.
  - Every other part of Level Up is unchanged: the 50/25-Gem minimums,
    Platinum Pieces and PP-bought Boosts staying permanent, and lairs/
    Gold resetting each time.
  - Updated the Level Up screen's copy throughout to describe Gems as
    temporary rather than permanent.

## [0.23.4] - 2026-09-06 12:02 PM EDT

- Added a recurring 25-Gem minimum for every Level Up after the first —
  previously any positive Gem gap unlocked it again, so a trickle of just
  a few Gems could technically trigger a reset. Every Level Up now needs
  to be worth at least 25 Gems (still less than the first one's 50, since
  recurring resets shouldn't be held to as high a bar), and once that's
  cleared the full amount is still granted, not capped at 25.

## [0.23.3] - 2026-09-06 11:55 AM EDT

- Narrowed the 50-Gem minimum added in 0.23.2 to only apply to the very
  first Level Up. Every Level Up after that goes back to the plain
  lifetime-earnings gate from 0.23.1 with no minimum batch size — once
  you've cleared that first real milestone, a smaller top-up payout is
  fine.

## [0.23.2] - 2026-09-06 11:23 AM EDT

- Added a real minimum to Level Up: previously the very first Level Up
  unlocked the moment lifetime earnings were worth even 1 Gem (around
  44 billion lifetime Gold) — barely a milestone. Now the payout must be
  worth at least 50 Gems before Level Up is allowed at all (around 111
  trillion lifetime Gold for the first one), so it can't be triggered for
  a token amount. Once that bar is cleared, the full payout is granted —
  the minimum only gates whether the button is enabled, it doesn't cap
  how many Gems you actually get.

## [0.23.1] - 2026-09-06 11:09 AM EDT

- Fixed Level Up having no real cap: the previous formula computed Gems
  from your *current* hoard's net worth, which reset every Level Up — so
  as soon as you'd earned back a small amount of gold, you could Level Up
  again immediately, over and over, without actually progressing further.
- Replaced it with AdVenture Capitalist's real Angel Investor formula: Gems
  earned are based on *lifetime* Gold earned (a new running total that
  never resets, not even on a Level Up), and a Level Up only ever grants
  the gap between what that lifetime total "should" be worth in Gems and
  how many you've already earned. Leveling up twice without earning any
  new lifetime Gold in between now correctly grants 0 the second time —
  you have to actually earn more before it unlocks again.
- Verified live: with 206 Gems already banked from a prior Level Up, the
  button correctly shows disabled ("Keep earning Gold — Level Up unlocks
  again once you've earned enough more") since no new lifetime earnings
  had accrued yet.

## [0.23.0] - 2026-09-05 11:35 PM EDT

- Implemented Level Up, the prestige mechanic that's been an open question
  since the project started: reset your Gold and every owned lair back to
  the start, in exchange for Gems — a new, third currency that permanently
  boosts every future run's income (+2% per Gem, flat, not compounding).
  Platinum Pieces and everything bought with it (Speed/Profit Boost, the
  ad cooldown) carry over across a Level Up untouched.
- Gems earned follows a square-root curve based on your current hoard's
  net worth — cheap early on, needing dramatically more to earn the same
  amount later, the classic prestige-currency shape.
- New "Level Up" section (reachable from the same menu item that's existed
  since early on) shows your current Gems and their income bonus, plus
  exactly how many Gems leveling up right now would earn. Tapping the
  button asks for confirmation first — resetting your hoard isn't
  something to do by accident — then pops up a reward dialog on success,
  matching the game's existing reward-popup look but recolored to
  amethyst for Gems instead of gold.
- Gems now show in the header next to gold/sec and Platinum Pieces.
- This replaces an earlier planned currency name, "Scale Shards" — it had
  a placeholder field in the save data but no actual mechanic attached to
  it yet, so it's simply been renamed to Gems rather than kept as a
  separate, unused currency.
- Verified live: leveled up a 27B-gold hoard, earned 206 Gems, watched the
  reset happen (hoard back to one Kobold Warren, 0 gold) and the new
  +412% income bonus apply immediately to that lair's production.

## [0.22.3] - 2026-09-05 10:15 PM EDT

- Replaced the buy button's flat brick-red "can't afford this" color with a
  dimmed version of the lair's own rarity color instead. Red read as an
  alert/warning and, worse, looked *more* clickable than the normal
  affordable state, not less.
- The unaffordable state now just fades the button (45% alpha, matching
  `WoodenButton`'s own disabled treatment elsewhere in the app) rather than
  switching to a different color — "disabled" reads the same way across
  every button in the game now.
- Verified live across all five rarity tiers: affordable buttons stay
  vivid, unaffordable ones fade to a muted version of the same color.

## [0.22.2] - 2026-09-05 10:08 PM EDT

- Fixed Steward-managed lair avatars showing semi-transparent even though
  they're actively producing on their own. The avatar's dimming was driven
  by the same `enabled` flag that gates tap-ability, and a Steward-managed
  lair is never tappable (there's nothing to tap — the Steward already
  handles it), so it was permanently dimmed regardless of how much it was
  earning.
- Split "tappable" and "visually bright" into two separate signals in
  `LairRow`: a lair is now bright whenever it's owned and either has a
  Steward or isn't mid-cycle — only an owned, Steward-less lair mid-load
  still dims, unchanged from before.
- Verified live: hired Stewards on Kobold Warren, Giant Rat Burrow, and
  Bugbear Warcamp (portrait-art avatars) and confirmed all three now
  render fully opaque instead of dimmed.

## [0.22.1] - 2026-09-05 9:49 PM EDT

- Fixed the new lair card layout being dominated by gold: the buy button's
  affordable state was a gold gradient, which is also this game's
  legendary-tier rarity color — once most lairs were affordable (normal
  mid/late-game), gold covered nearly every card, both overwhelming the
  screen and making "gold = legendary" meaningless.
- The buy button now uses each lair's own rarity color (green, blue,
  purple, orange) instead of a universal gold — gold only appears on an
  actually-legendary lair now. A solid accent stripe down the card's left
  edge plus a faint rarity-colored wash across the whole card background
  reinforce the tier even before looking at the button. The unaffordable
  state is unchanged (still a flat muted red regardless of tier).
- Compared four different fixes as an HTML mockup before building this
  one, which combines a rarity-colored button with the full-card wash.
- Verified on the emulator across green, blue, purple, and orange tiers,
  and confirmed the unaffordable red state still works correctly.

## [0.22.0] - 2026-09-05 9:31 PM EDT

- Redesigned the lair card layout, piloted first as an HTML mockup (based
  on a layout from another of the user's games, reskinned with this
  game's own wood/parchment/gold palette and rarity colors) before being
  built in the app:
  - Each lair's challenge rating now shows right next to its name, e.g.
    "Kobold Warren (1/8 CR)", instead of on a separate line with the
    monster type.
  - The progress fill is now a proper progress-bar track under the name
    (dark inset groove, animated rarity-colored fill, a gloss highlight
    strip) with the income/cycle-time text centered right on the bar,
    instead of the entire card background doubling as the fill.
  - The buy button is now a full-width two-line control (quantity, then
    price) — gold when affordable, a muted brick-red when not — and the
    owned count moved into its own small panel next to it instead of a
    plain "Owned: N" text line.
- The monster type line (e.g. "Kobold") is dropped from the card — there
  wasn't room for it alongside the real progress bar, and the lair name
  already implies it in every case so far.
- Fixed a layout bug hit while building this: the progress label's text
  was overflowing past the bar into the buy button row below it, caused
  by padding that shrank the area `clip()` actually bounded rather than
  adding space above the bar, combined with the label inheriting a
  default text size taller than the bar's own height.
- Verified on the emulator across every rarity tier and both the
  affordable/unaffordable and owned/unclaimed states.

## [0.21.6] - 2026-09-05 8:59 PM EDT

- Fixed a Kobold Warren at a 38ms cycle time (fast, but not fast enough to
  show a solid bar) visibly bouncing instead of climbing smoothly. The
  cause wasn't animation tuning — it was sampling resolution: at the
  previous 33ms tick rate, a 38ms cycle only got about one sample per
  cycle, which can't distinguish "just started" from "about to finish."
- Lowered the engine's tick interval from 33ms to 8ms (so a 38ms cycle
  now gets 4-5 samples) and shortened the fill animation from 60ms to
  20ms to match. This doesn't remove the underlying limit entirely — some
  cycle time will always be fast enough to alias against any fixed tick
  rate — but it pushes the point where that becomes visible much further
  out, well past where ordinary milestone/Speed-Boost stacking currently
  reaches.
- The engine's tick loop and progress calculation now run about 4x as
  often; both are cheap arithmetic over the ~14-lair catalog, so this is
  a deliberate small CPU/battery trade for smoother animation.
- Verified on the emulator: the previously-bouncing 38ms lair now holds a
  stable, consistently near-full fill across consecutive frames instead
  of jumping to random low values.

## [0.21.5] - 2026-09-05 8:48 PM EDT

- Each lair card now shows its actual current cycle time next to its
  income — "218 gp / 38ms" instead of just "218 gp/cycle" — so it's
  visible exactly how fast a lair is collecting, from milliseconds (a
  heavily Speed-boosted lair) up through minutes and hours for a slow,
  unboosted one.
- New `ui/format/CycleTimeFormat.kt` handles the full range: whole
  milliseconds under 1 second, "12s"/"1.5s" under a minute, then
  minutes/hours/days for anything longer.
- Verified on the emulator across the full owned range: "218 gp / 38ms",
  "3.60K gp / 750ms", "27.00K gp / 1.5s", "15.55M gp / 48s", "22.39M gp /
  6m 24s", "89.58M gp / 25m 36s".

## [0.21.4] - 2026-09-05 8:25 PM EDT

- Tuned the progress-fill bar further based on feedback that it still
  snapped back around 80–90% instead of visibly reaching the end, and that
  the "solid" cutoff for very fast lairs kicked in too early (around the
  200-owned Speed milestone).
- Shortened the fill animation from 150ms to 60ms — a tween always lags its
  target by roughly its own duration, and 150ms was long enough that a
  cycle only a few hundred milliseconds long would reset again before the
  bar ever visually caught up to full.
- Moved the "just show it solid" cutoff from ~99ms down to 10ms (the same
  threshold the coin-burst effect already uses) — a lair that's maxed its
  own individual Speed milestones (400 owned, 64x) now sits right at that
  line instead of well past it, so most lairs keep showing a real, very
  fast animation instead of freezing solid as early as the 200 rung.
- Verified on the emulator: a lair previously solid at ~200 owned now
  shows a real fill climbing past 90% before resetting, and moderately
  fast lairs that used to snap back around 80–90% now visually read as
  consistently near-full between samples.

## [0.21.3] - 2026-09-05 8:11 PM EDT

- Fixed the progress-fill bar not starting cleanly from empty or reliably
  reaching full: after the previous fix's 150ms tween, a cycle resetting
  to 0% still animated smoothly *backward* into the next cycle instead of
  snapping — which also ate into the next cycle's own 150ms fill window,
  so a moderately fast lair's bar rarely looked like it actually finished
  before resetting again.
- Since a lair's fill fraction only ever increases within a cycle and
  drops exactly once on completion, any decrease is unambiguously a reset
  — the bar now snaps instantly to empty in that case instead of tweening,
  and only tweens smoothly while actually filling forward.
- Verified on the emulator: a Steward-managed lair's bar climbed smoothly
  across several consecutive frames (65% → 90% → 95%) and then reset to a
  fresh low value with no in-between slide-back frames.

## [0.21.2] - 2026-09-05 8:00 PM EDT

- Fixed the lair progress-fill bar bouncing/glitching at high Speed
  multipliers instead of animating smoothly. Cause: the bar's fraction was
  derived per-card from raw cycle-progress data, animated with a tween
  duration tied to the engine's 33ms tick rate — once a lair's cycle got
  fast enough to complete inside a single tick, that value stopped meaning
  anything sampled at that rate, and no amount of easing could smooth out
  a signal that had already lost information at the source.
- Rearchitected using a pattern proven in another project: the engine now
  computes each lair's fill fraction once per tick into its own
  `lairProgress` map (a separate `StateFlow`), and reports a flat "100%
  full" once a lair's cycle drops below about 99ms rather than a jittery
  partial value — a heavily Speed-boosted lair now reads as a clean solid
  bar, which is the honest picture once it's completing cycles far faster
  than anyone could watch one fill. The UI's fill animation also now uses
  a fixed 150ms tween, deliberately decoupled from the tick rate, so quick
  resets get smoothed rather than tracked instantly.
- Verified on the emulator: a lair pushed to an extreme Speed multiplier
  (sub-40ms cycles) now renders as a clean, undivided solid-tinted card
  with no seam or flicker, while moderate-speed lairs still show a
  smoothly, monotonically advancing partial fill across consecutive
  frames rather than jumping around.
- New unit tests cover `lairProgress` for an idle lair, a partially loaded
  one, one that just completed, a Steward-managed lair mid-cycle, and the
  solid-at-extreme-speed clamp.

## [0.21.1] - 2026-09-05 7:30 PM EDT

- Milestone rungs now split into two real bonus types instead of all
  boosting income: the first six rungs (25, 50, 100, 200, 300, 400 owned)
  now shrink that lair's cycle time (a Speed bonus), while 500 and beyond
  boost gold earned per cycle instead (an Income bonus). The two compound
  independently — a lair's Speed bonus no longer inflates its income the
  way the old single combined multiplier did.
- The milestone pop-up and the Unlocks screen both now correctly label
  each rung "Speed" or "Income" based on which it actually is, instead of
  always saying "Speed" regardless of the real effect.
- This is a real balance change, not just a label fix: any lair that had
  only crossed Speed-type rungs (25 through 400) will show lower gp/cycle
  than before, since that bonus no longer double-counts toward income.
  Verified on the emulator — Kobold Warren's income correctly dropped from
  848 gp/cycle to 106 gp/cycle after this change (same 106 owned, no
  Income rung crossed yet), and a fresh "x200" milestone pop-up correctly
  showed "2x Speed."
- New/updated unit tests cover the Speed/Income split precisely at the
  400/500 rung boundary, for both the per-lair and "Everything" global
  multipliers.

## [0.21.0] - 2026-09-05 7:04 PM EDT

- Reaching an ownership milestone (25, 50, 100 owned of a lair, or the
  "Everything" bonus once every lair has caught up) now pops up a themed
  announcement naming which milestone was reached and its reward — e.g.
  "Kobold Warren — x100, 2x Speed, for this lair" — instead of the bonus
  only being visible later in the Unlocks screen. Styled to match the
  game's existing cozy-fantasy pop-ups (parchment card, carved wood
  border, treasure-chest art, glowing reward text, a wooden "Nice!"
  button to dismiss).
- A single big purchase that jumps past several rungs at once (e.g.
  buying up from 10 to 100 owned crosses 25, 50, and 100 in one go) queues
  each announcement and shows them one at a time rather than bundling
  them into one pop-up.
- Verified on the emulator: buying up Kobold Warren from 50 to 106 owned
  correctly announced only the newly-crossed "x100" rung (not "x50" again,
  already announced earlier), the dialog dismissed cleanly on "Nice!",
  and gold/production continued unaffected afterward. The domain logic
  for multi-rung purchases and the global "Everything" rung is covered by
  new unit tests in `GameStateExtensionsTest.kt`.

## [0.20.2] - 2026-09-05 6:39 PM EDT

- Kobold Warren, Giant Rat Burrow, and Bugbear Warcamp now show real
  creature portrait art in their lair-list avatar instead of the
  lettered placeholder disc. Every other lair still uses the placeholder
  until it gets matching art — several already-generated candidates exist
  but are being held back because they were drawn in a different, more
  painterly style that doesn't match this batch.
- Fixed a Compose layout bug hit while wiring the new art in: the portrait
  image was blowing up to fill almost the entire screen instead of sitting
  in its small circular avatar. Cause: the avatar's `Row` measures its
  height via `IntrinsicSize.Min`, and unlike the placeholder's `Canvas`
  (which has no opinion on its own size), an `Image` reports its source
  art's real pixel size during that measurement pass — sizing it with
  `fillMaxHeight()/fillMaxWidth()` let that leak through. Switched to
  `Modifier.matchParentSize()`, which sizes strictly off the
  already-resolved avatar circle instead.
- Verified on the emulator: all three avatars render at the correct small
  circular size, cropped and bordered like the placeholders, and every
  other lair is unaffected.

## [0.20.1] - 2026-09-05 6:25 PM EDT

- Shop and Settings now show their own wooden-sign art in the floating menu
  (and as the section header when opened), matching every other menu item
  instead of falling back to a plain white surface/title. New art:
  `menu-shop.png` and `menu-settings.png` (already cropped to match the
  other signs, no re-export needed).
- Verified on the emulator: both signs render at the correct size in the
  menu stack, and opening either section shows the sign straddling the
  card's top edge exactly like Unlocks/Stewards/etc., with no content
  hidden underneath it.

## [0.20.0] - 2026-09-05 5:47 PM EDT

- Redesigned gold collection for lairs without a hired Steward: tapping a
  lair (its card or its avatar) no longer collects gold directly — it
  starts that lair's production cycle instead. The cycle fills up on its
  own over the lair's normal production time with no further input, then
  automatically credits the gold and fires the gold-coin burst effect the
  instant it finishes. Tapping again while a cycle is already running does
  nothing. This is a deliberate, confirmed change to how unmanaged lairs
  earn: they now sit completely idle (0% progress, no income) until
  tapped, including while the app is closed — an idle lair earns nothing
  offline unless it happened to be mid-cycle when the app was backgrounded.
  Steward-hired lairs are completely unaffected: they keep collecting
  silently and continuously, online or offline, with no confetti, exactly
  as before.
- The gold-coin burst is now driven by the cycle actually completing, not
  by the tap itself — so a completed cycle always shows the burst
  regardless of how long the load took. Below a 10ms production time
  (reachable only after dozens of stacked Speed Boost levels) the burst is
  skipped entirely since it can't read as anything but a flicker at that
  speed — the gold is still credited either way.
- Time Skip (the Shop's Platinum-Pieces spend) still instantly grants
  production from every owned lair regardless of whether it's currently
  mid-cycle, matching its existing "every owned lair" description — it's a
  bonus layered on top of the tap cycle, not a substitute for tapping.
- The header's gold-per-second stat now only counts Steward-managed lairs,
  since an unmanaged lair no longer runs continuously on its own and
  including it would overstate real passive income.
- Domain/data changes: `OwnedLair.isReadyToCollect` was replaced by
  `isLoading` (true while a tapped cycle is running) and `completedLoads`
  (a counter bumped each time a tapped cycle finishes, used to detect
  completions for the burst effect). Local save format bumped (Room DB
  version 3→4, wiping local saves via the existing destructive-migration
  fallback — no real installs to preserve yet); old cloud saves still
  decode fine since Supabase's JSON decoding ignores unknown/missing keys.
- Verified on the emulator: a freshly claimed lair sits at 0% and earns
  nothing until tapped; tapping starts a visible fill that completes on
  its own and credits gold automatically; tapping again mid-fill is a
  no-op (confirmed via a visible partial-fill screenshot followed by an
  immediate second tap that changed nothing extra). Also added new unit
  tests for `startLairLoad` (no-op on a Steward-managed or already-loading
  lair), the confetti-skip threshold, and Time Skip crediting an idle,
  untapped lair.

## [0.19.1] - 2026-09-05 4:38 PM EDT

- Redesigned the Unlocks screen: milestones are now grouped by lair (a
  "Kobold Warren" header, then every rung it's reached below it — plus an
  "Everything" group for the global ladder), shown as a 4-cards-per-row
  grid instead of one row per rung. Each card is reduced to its two
  load-bearing numbers — "x25" (the ownership count) and "2x Speed" (the
  bonus) — instead of a full sentence. A short final row (not a multiple
  of 4) pads with invisible spacers rather than stretching its real cards,
  so every card stays the same size regardless of row length.
- Verified on the emulator: the empty state ("No milestones unlocked
  yet…") still renders correctly, and a save with Kobold Warren at 600
  owned (7 rungs reached) plus Goblin Camp at 30 (1 rung) correctly showed
  two separate groups — Kobold Warren's 7 cards wrapping into a 4-then-3
  grid with the second row left-aligned (not stretched), and Goblin
  Camp's single card in its own group below.

## [0.19.0] - 2026-09-05 3:12 PM EDT

- Added a second Time Skip tier to the Shop: 10 minutes of production for
  2 Platinum Pieces, alongside the existing 1 hour for 5 pp. Cheap enough
  to buy from a single "Watch an Ad" reward, specifically so the whole
  Platinum economy (earn via ad, spend on a Boost) is easy to test
  end to end without needing to grind or edit a save.
- `domain/model/Boosts.kt`'s flat `TIME_SKIP_COST_PP`/`TIME_SKIP_SECONDS`
  constants became `TIME_SKIP_OPTIONS: List<TimeSkipOption>` (a
  `costPp`/`seconds` pair per tier) — deliberately a list, not another flat
  pair, since more tiers are expected here later. `GameEngine.purchaseTimeSkip`
  and `GameViewModel.purchaseTimeSkip` now take a `TimeSkipOption` parameter
  instead of always buying the one hardcoded size. `ShopContent` renders one
  `BoostRow` per entry in `TIME_SKIP_OPTIONS` ("Time Skip — 10m", "Time Skip
  — 1h", ...) instead of a single fixed row.
- Verified end to end on the emulator: granted Platinum via a direct save
  edit (safer than repeated ad interactions — see the note below), bought
  the new 10-minute tier for 2 pp (balance correctly went 20 → 18 pp), and
  confirmed the underlying `advance()` call actually ran — the unmanaged
  Kobold Warren lair filled to its one allowed pending cycle exactly as
  offline earnings would, and plundering it credited 1 gp.
- Also attempted to test via a real "Watch an Ad" → spend flow, which hit
  the same automated-testing hazard noted for 0.17.0: an accidental
  click-through on the test ad's own "Learn More"/"Install" UI (most likely
  a queued/delayed tap landing on the wrong screen, the `adb input tap`
  quirk already documented in CLAUDE.md) navigated to the Play Store before
  the ad finished, correctly earning no reward — a real confirmation of the
  reward-gating logic, just not the happy path. Switched to the direct
  save-edit approach for the actual Time Skip verification instead of
  retrying the ad repeatedly.

## [0.18.1] - 2026-09-05 2:45 PM EDT

- The Shop's "Watch an Ad" is now open to guests too, not just signed-in
  players — it earns no real money, so a guest losing that Platinum on
  reinstall isn't the kind of loss the sign-in gate exists to prevent
  (unlike the still-gated "Buy Platinum Pieces" IAP, which is real money
  and should stay tied to a recoverable account). No code changes were
  needed below the UI layer — `GameViewModel.watchAdForPlatinum` never
  checked sign-in status to begin with, so this was purely a `ShopContent`
  visibility change: "Watch an Ad" moved outside the `isSignedIn` gate,
  "Buy Platinum Pieces" stayed inside it, and the guest explanatory note
  now only mentions the real-money purchase.
- Verified end to end on the emulator as a guest: tapped "Watch", the test
  ad played, and after it finished the balance updated to "2 pp", the
  button correctly switched to "In 23h 59m", and an "Earned 2 pp!" banner
  appeared — the first time this session a rewarded-ad *reward grant* (not
  just the negative/early-exit case) was captured live, since testing as a
  guest sidesteps the confirmed-account/real-inbox limitation that blocked
  this in 0.17.0/0.18.0.

## [0.18.0] - 2026-09-05 2:25 PM EDT

- The Shop's "Watch an Ad" is now real: earns 2 Platinum Pieces, watchable
  once every 24 hours. The cooldown is tracked on the save itself
  (`GameState.lastPlatinumAdWatchedAt`) rather than anything ad-network- or
  device-side, so it persists across sessions and syncs with the rest of
  the save. The button shows "Available in Xh Ym" (updating live off the
  ticking game state, no separate countdown timer) while on cooldown, and
  a message banner reports the outcome ("Earned 2 pp!", a cooldown notice,
  or "ad isn't ready yet").
- `AdManager` now supports more than one rewarded placement — refactored
  around a `RewardedPlacement` enum (`OFFLINE_EARNINGS_DOUBLE`,
  `SHOP_PLATINUM`, one real ad unit id each) with a loaded-ad slot per
  placement, instead of the single hardcoded slot 0.17.0 shipped with. The
  debug-build test-device safeguard (forces Google's test creative through
  both real ad units) applies to every placement automatically.
- New `domain/model/AdRewards.kt`: `PLATINUM_AD_REWARD_PP`,
  `PLATINUM_AD_COOLDOWN` (24h), and `GameState.canWatchPlatinumAd`/
  `platinumAdCooldownRemaining` — the cooldown math, fully unit-tested.
  `GameEngine.grantPlatinumAdReward` grants the Platinum and stamps the
  watch time atomically, re-checking the cooldown itself rather than
  trusting the caller already did.
- New shared `ui/format/DurationFormat.kt` ("3h 12m" / "12m") so the
  Shop's live button label and the ViewModel's cooldown message can't
  drift apart — extracted after almost duplicating the same formatting
  logic in both places.
- Bumped Room to database version 3 for the new persisted column
  (`lastPlatinumAdWatchedAtEpochMillis`); Supabase's `GameStateDto` gained
  the matching nullable field with a default, so older cloud saves still
  decode.
- Verified on the emulator: the Shop's guest view still correctly hides
  the whole Earn Platinum section (regression check after the `ShopContent`
  signature change). Couldn't verify the signed-in "Watch an Ad" tap-through
  live — same limitation as 0.15.0/0.16.0's sign-up testing: reaching a
  confirmed (non-guest) account in this environment needs a real inbox to
  receive the sign-up verification code, which isn't available here. The
  domain logic (cooldown math, reward grant, persistence round-trip) is
  fully unit-tested instead, and the ad-showing code path is the same
  `AdManager.showAd` contract already verified end-to-end for the Welcome
  Back placement in 0.17.0.

## [0.17.0] - 2026-09-05 2:06 PM EDT

- Wired in the first real Google AdMob rewarded ad: the Welcome Back
  ("While You Were Away…") dialog now has a "Watch Ad to Double" button
  that doubles the offline earnings just shown, once the player watches
  the ad to completion. Only one watch is allowed per pop-up.
- Added the Google Mobile Ads SDK (`play-services-ads`), the app's real
  AdMob App ID in the manifest, and a new `AdManager` (`ads/AdManager.kt`)
  — an app-scoped singleton, same pattern as `GameEngine`, that loads one
  rewarded ad ahead of time and shows it on request. `GameEngine` gained a
  matching `grantGold(amount)` for crediting the doubled amount outside the
  normal income pipeline.
- **Real ad unit id, test-mode-forced for all debug builds.** The Welcome
  Back placement's ad unit id is the actual production one from AdMob —
  there is no separate test ad unit for this placement. To avoid ever
  loading/serving a real ad (and the invalid-traffic policy risk that
  comes with automated or dev-device impressions on a live unit),
  `AdManager` registers the device as a Google test device
  (`AdRequest.DEVICE_ID_EMULATOR`) whenever `BuildConfig.DEBUG` is true,
  which makes Google serve its test creative through the same real ad unit
  id instead. This must stay in place for every debug build; a release
  build (`BuildConfig.DEBUG == false`) skips it and serves real ads
  normally.
- Verified on the emulator: the Welcome Back dialog correctly showed the
  new button, tapping it opened a real ad activity clearly labeled "Test
  Ad" (confirming the test-device safeguard worked), and — after an
  accidental early exit via the test ad's own click-through UI — the
  double was correctly *not* granted, confirming the reward is gated on
  actually finishing the ad rather than just opening it. Didn't capture a
  full watch-to-completion reward grant live in this session (scripting a
  clean, no-accidental-taps run through a real video ad's own UI via adb
  is fragile), but the negative case — no early-exit reward — is the
  harder one to get right and is confirmed; the grant path itself
  (`GameEngine.grantGold` doubling `earnings.goldEarned`) is a two-line
  arithmetic operation reviewed directly rather than exercised live.

## [0.16.0] - 2026-09-05 1:30 PM EDT

- Sign-up now requires entering an emailed verification code before it
  takes effect — a deliberate anti-bot/anti-spam gate on account creation,
  not just an email-ownership nicety. After submitting the sign-up form,
  Settings shows a code-entry step ("We emailed a verification code to
  ... — enter it below to finish creating your account") with Verify,
  Cancel, and a "Resend code" link, instead of the account being live
  immediately.
- `AuthRepository` gained `verifySignUpCode(email, code)` (calls Supabase's
  `verifyEmailOtp` with the `EMAIL_CHANGE` OTP type — that's the correct
  type here, not `SIGNUP`, since from Supabase's point of view the account
  already exists as our anonymous user and we're just setting its
  previously-empty email) and `resendSignUpCode(email)`. `GameViewModel`
  gained matching `verifySignUpCode`/`resendSignUpCode`/
  `cancelSignUpVerification` methods and a `pendingVerificationEmail`
  state that drives Settings into the code-entry step.
- Still gracefully handles a Supabase project with "Confirm email changes"
  turned off: `signUp` checks whether the account is already fully
  upgraded right after the initial call and skips the code step entirely
  if so, exactly like before this change. The code step only appears when
  Supabase actually held the upgrade pending a confirmation, which is the
  setup you need for this to function as an anti-bot gate at all — noted
  as a dashboard requirement in CLAUDE.md's Auth section, alongside the
  existing rate-limit note from 0.15.0.
- The code field doesn't hardcode a digit count (accepts any non-blank
  input) since Supabase's OTP length is a project setting, not something
  the app controls.
- Verified the surrounding flow still works on the emulator (guest state,
  Sign In form, error banner) after this refactor. Couldn't verify the
  actual code-entry step end to end in this session — Supabase's own
  email rate limit (hit during 0.15.0's testing) was still in effect, and
  receiving/typing back a real emailed code isn't possible in this
  environment anyway. The `verifyEmailOtp`/`EMAIL_CHANGE` type choice
  matches Supabase's own documented anonymous-user-upgrade pattern, but
  flagging that the exact server round trip is unverified rather than
  claiming otherwise.

## [0.15.0] - 2026-09-05 1:19 PM EDT

- Built the Settings screen's real content: an Account card (sign up, sign
  in, sign out) and a Cloud Sync card (automatic every 5 minutes, plus a
  manual "Sync Now" button).
- `AuthRepository` gained `signUp`/`signIn`/`signOut`/`currentUserEmail`
  alongside the existing `ensureSignedIn`. `signUp` upgrades the current
  guest (anonymous) session to a permanent email/password account in place
  via Supabase's `updateUser` — same user id, same cloud save, no merge
  needed. `signIn` switches to a different, already-existing account (a
  different user id), so `GameViewModel` reconciles local vs. that
  account's cloud save with the existing `mergeGameStates` logic (the same
  merge used on launch). `signOut` drops the session and immediately
  re-establishes a fresh guest one, syncing the outgoing account's cloud
  row one last time first so nothing played under it is lost — local play
  is never interrupted either way.
- `GameViewModel` now owns this account/sync state directly (no separate
  `AuthViewModel` — the two are tightly coupled) — `userEmail` (null means
  guest), `authMessage` (surfaces both errors like "Invalid login
  credentials" and neutral notices like "check your email to confirm"),
  `lastSyncedAt`/`isSyncing` for the sync card. Cloud sync now also runs on
  a repeating 5-minute timer, not just once per launch.
- Fixed a real bug found while testing this: Supabase returns `""` (not
  null) for a guest's email, which was tricking the "is this a guest"
  check into treating every guest as signed in. `AuthRepository.currentUserEmail()`
  now normalizes blank to null.
- IAP visibility gated on sign-in per design: the Shop's "Earn Platinum"
  section (watch an ad / buy outright) now only shows for signed-in
  players — guests see an explanatory note instead ("keeps real-money
  purchases tied to an account you can recover, not a guest identity
  that's lost on reinstall"). The Boosts section (spending Platinum
  already owned) is unaffected — that's not a real-money purchase.
- Verified on the emulator: fresh guest correctly shows the guest copy and
  hides Earn Platinum in the Shop; the sign-up/sign-in forms validate
  input, submit, and correctly show the collapsed-optimistic-submit +
  banner-message pattern; a bad Sign In attempt correctly surfaces
  "Invalid login credentials" from Supabase; Sync Now correctly updates
  "Last synced". A full Create Account attempt hit Supabase's own email
  rate limit (expected — this project's test account had already sent a
  few confirmation emails during this same testing session) and a full
  sign-in-after-email-confirmation round trip couldn't be verified end to
  end since that requires a real inbox this environment doesn't have —
  noted here rather than claimed as verified.

## [0.14.0] - 2026-09-05 12:17 PM EDT

- Platinum Pieces now have a real spend path: permanent, account-wide
  Speed Boost and Profit Boost levels (each costs more Platinum than the
  last, compounding — +5%/level for Speed, +10%/level for Profit) plus a
  repeatable Time Skip (flat 5 pp, instantly grants an hour of production
  using the same math as offline earnings). All three are new rows in the
  Shop screen's "Boosts" section, above the existing "Earn Platinum" rows.
- New `domain/model/Boosts.kt` holds the cost/multiplier formulas
  (closed-form, same shape as `CreatureLair.costForNextUnit`). `GameState`
  gained `speedBoostLevel`/`profitBoostLevel`. `CreatureLair.incomePerCycle`
  takes a third `profitBoostMultiplier` parameter and a new
  `effectiveProductionSeconds(speedBoostMultiplier)` replaces raw
  `baseProductionSeconds` everywhere a lair's actual cycle time is used
  (`GameEngine`'s tick loop, `LairCard`'s progress bar, `GameScreen`'s
  gold/sec sum) — all default to no-bonus (1.0) so every existing call site
  and test keeps working unchanged at boost level 0.
- Bumped Room's database version to 2 for the two new `GameStateEntity`
  columns, with `DatabaseModule` now falling back to destructively
  recreating the database on a schema mismatch — no formal migration exists
  yet (still pre-1.0, no real installs to preserve), documented as a
  deliberate trade-off in CLAUDE.md rather than building real migrations.
  The Supabase side needed no schema change (jsonb blob) — just default
  values on the two new `GameStateDto` fields so old cloud saves still
  decode.
- Verified on the emulator (via a save edited directly through the Room
  database to grant Platinum for testing, then cleared afterward): buying
  Speed Boost went Level 0→1, 500→490 pp, cost updated 10→15 pp for the
  next level, and the description correctly showed "5.0% faster"; Profit
  Boost behaved identically for its own 10%/level; Time Skip deducted its
  flat 5 pp. Back on the game screen, Kobold Warren's income line updated
  from "1 gp/cycle" to "1.1 gp/cycle" (the 10% profit boost) and the
  header's gp/sec rose from 1.7 to 1.9 (reflecting the 5% speed boost too),
  confirming both boosts actually apply to live production, not just the
  Shop's own preview text.

## [0.13.0] - 2026-09-05 11:56 AM EDT

- Added a Shop menu section. Considered adding a new "Jewels" premium
  currency for this, but `GameState.platinumPieces` already existed for
  exactly this purpose (IAP-sourced, ad-earnable) with no UI home yet — the
  Shop is that home now, not a second currency.
- The Shop screen shows the current Platinum Pieces balance, then two ways
  to earn more: watching a rewarded ad, or buying outright. Both are shown
  as disabled "Soon" buttons since neither an ad network nor billing is
  integrated yet — no shop items (what Platinum will eventually buy) exist
  either, since none have been designed.
- Styled with the same parchment-card look as Stewards/Unlocks. `Shop` was
  added to `FloatingMenu`'s item list (no sign art yet, so it falls back to
  the same plain-`Surface` look Settings uses).
- Verified on the emulator: the Shop sign opens the section correctly, shows
  "0 pp" with the coin icon and both disabled earn-methods, and the close
  button dismisses it back to the game.

## [0.12.2] - 2026-09-05 11:37 AM EDT

- Restyled the offline-earnings "While You Were Away…" pop-up to match the
  cozy-fantasy chrome instead of a plain Material `AlertDialog`: a parchment
  scroll with a carved wood border, the existing `open_chest` art, glowing
  gold text for the amount earned, and a wooden "Claim" button.
- Promoted `GlowingGoldText` out of `GameHeader` into `ui/common/` so this
  dialog could reuse the same glowing/embossed gold-text treatment as the
  header's total.
- Verified on the emulator: the dialog renders correctly (chest art, glowing
  gold amount, italic "earned over the last N minutes" line, wooden Claim
  button) and still correctly dismisses and credits the gold on tap.

## [0.12.1] - 2026-09-05 11:28 AM EDT

- Redesigned the Unlocks screen to stop compressing milestones: it now shows
  one row per rung actually reached, instead of one row per lair summarizing
  its current bonus. Owning 50 Kobold Warrens now shows two rows — "Kobold
  Warren — 25 owned — Profit Speed Doubled" and "Kobold Warren — 50 owned —
  Profit Speed Doubled" — instead of a single "Bonus: x4, Next at 100" line.
  The "Everything" ladder gets the same treatment: one row per global rung
  reached instead of a single status card.
- Restyled `UnlocksContent` with the same parchment-card look as
  `StewardsContent`/`LairCard`, replacing its older plain Material `Surface`
  look from before the cozy-fantasy restyle.
- Verified on the emulator, live: with Kobold Warren pushed to 25 then 50
  owned in front of the running game, confirmed gp/cycle jumped 24→50→200
  (exactly the 2x then compounding 4x the milestone math predicts) and that
  the Unlocks screen now lists both the 25-owned and 50-owned rungs as
  separate entries rather than one merged row. Also confirmed the
  "Everything" ladder correctly stays empty since other lairs haven't
  caught up — it doesn't fire just because one lair is far ahead.

## [0.12.0] - 2026-09-05 11:09 AM EDT

- Implemented the Stewards screen for real (was "Coming soon…" since the
  Steward-hire button was removed from `LairCard`) — this is the only way to
  hire a Steward now. Shows an intro card explaining what a Steward does,
  then one row per *owned* lair: a "Steward Hired" badge if it already has
  one, or a button to hire one for that lair's own cost. Lairs with zero
  units owned don't get a row.
- Styled with the same cozy-fantasy chrome as the redesigned `LairCard` —
  translucent parchment cards and the shared `WoodenButton` — rather than
  the plainer Material look `UnlocksContent` still has (a candidate for the
  same treatment later).
- No domain changes: `GameEngine.hireSteward`/`GameViewModel.hireSteward`
  already existed and were already unit-tested from when the button lived
  on `LairCard`; this was purely wiring a real screen up to them.
- Verified on the emulator: hiring a Steward correctly deducts its cost and
  flips the row to "Steward Hired"; afterward the lair's card no longer
  needs manual taps — its fill animates continuously and gold keeps
  climbing on its own, exactly like the existing auto-collect behavior
  already covered by `GameEngineTest`.
- Documented a real environment quirk hit repeatedly while testing this:
  `adb shell input tap` can queue up and deliver taps well after the issuing
  command returns, landing on whatever the UI shows *at delivery time* — see
  CLAUDE.md's Build Environment Notes for how to tell that apart from an
  actual bug next time.

## [0.11.0] - 2026-09-05 10:26 AM EDT

- Added a circular creature avatar next to each lair card, as its own
  container sharing a row with the card rather than living inside it.
  Tapping the avatar plunders the lair exactly like tapping the card.
- No monster portrait art exists yet (nothing's been dropped into `/assets`
  for this), so the avatar is a placeholder: a rarity-tinted radial-gradient
  disc with a carved border and the monster's first letter — the color band
  and the full name in the card next to it still make each lair identifiable
  even though a few tiers share an initial.
- The avatar is sized to exactly match its card's height automatically
  (`IntrinsicSize.Min` on the row + `aspectRatio(1f)` on the avatar), so
  there's no fixed size to keep in sync as card content changes.
- The coin-burst-on-plunder counter moved from `LairCard`'s local state up to
  the new `LairRow` that owns both the avatar and the card, so tapping
  either one fires the same burst.
- Verified on the emulator: avatars render correctly sized and colored per
  tier, dim when their lair isn't ready to collect, and tapping one plunders
  the lair exactly like tapping the card (confirmed in an isolated test —
  gold increases by exactly one cycle's income, ownership count unchanged).

## [0.10.2] - 2026-09-05 10:15 AM EDT

- Removed the Steward button from every `LairCard`. Hiring a Steward will
  live solely in the Stewards menu section going forward — but that screen
  is still just "Coming soon…", so there's currently no way to hire one at
  all until it's built. `GameViewModel.hireSteward`/`GameEngine.hireSteward`
  are untouched, just unreachable from the UI for now (see CLAUDE.md's open
  questions).
- Restyled `LairCard` to match the rest of the game's cozy-fantasy chrome
  instead of flat Material colors — it was reading as "boring" next to the
  restyled header:
  - A translucent parchment gradient base instead of a flat rarity wash
    (still sheer enough to show the game's background art through), with a
    per-tier rarity tint over the whole card and a stronger rarity gradient
    for the claimed-progress fill.
  - A bright line now marks exactly where the fill has reached, drawn at the
    fill bar's own trailing edge so it always tracks the animation with no
    extra position math.
  - The lair name uses the same serif lettering as the header; monster/CR is
    italic and muted; the income line is bold and gold-colored.
  - The Claim button is now a `WoodenButton` (carved wood, cut corners)
    instead of a Material `Button` — the same button now shared with the
    header's buy-quantity selector, promoted out of `GameHeader` into
    `ui/common/` along with its `FantasyPalette` (renamed from the header's
    private `GameHeaderColors`) so both screens paint with the same
    material.
- Verified on the emulator: cards read far richer against the background art
  while staying translucent, the fill/leading-edge line animates correctly,
  and Claim still enables/disables and purchases correctly with the new
  button.

## [0.10.1] - 2026-09-05 08:23 AM EDT

- The Unlocks screen now only shows milestones that have actually been
  reached, instead of previewing every lair's progress toward its first one.
  A lair's row appears once its own bonus clears 1x; the "Everything" card
  appears once every lair has caught up to at least the first rung. A save
  with nothing unlocked yet shows a short placeholder instead of a wall of
  untouched rows.
- Verified on the emulator: a fresh save shows the "No milestones unlocked
  yet" placeholder; after pushing one lair past its first milestone, only
  that lair's row appears (Everything stays hidden, since the rest haven't
  caught up).

## [0.10.0] - 2026-09-05 12:45 AM EDT

- Wired the header's buy-quantity selector into actual purchases: `x1`/`x10`/
  `x100` buy exactly that many units at once (atomically — either the whole
  bulk cost is affordable and all of them are bought, or none are), `Max`
  buys the most a lair can afford right now, and `Next` buys up to that
  lair's next ownership milestone.
- Added ownership milestones: every lair doubles its own production at 25,
  50, 100, 200, 300, and 400 owned, then gets a flat x4/x5/x6/x7 boost at
  500/1,000/5,000/10,000 — all compounding with every earlier rung already
  reached.
- Added an "Everything" milestone: the same ladder again, but keyed on
  whichever lair owns the *fewest* units — every lair has to catch up before
  this bonus applies, and it applies to every lair's production at once.
- Built the Unlocks section for real (was a "Coming soon…" placeholder): an
  "Everything" status card (current bonus, which lair is holding it back)
  followed by every lair's own bonus and how many units to its next
  milestone.
- Swapped the small coin icon next to the header's gold total from the
  placeholder chest art to `coin.png`, a proper ornate gold coin.
- Verified on the emulator: bulk purchases deduct the right bulk cost and
  add all the units at once; a lair's `gp/cycle` and the header's `gp/sec`
  update correctly the moment it crosses a milestone; the Unlocks screen
  scrolls through all 14 lairs and reflects live milestone progress; `Next`
  correctly targets each lair's own next milestone.

## [0.9.1] - 2026-09-05 12:00 AM EDT

- Restyled `GameHeader` to match the rest of the game's cozy-fantasy look
  (wooden signs, parchment, carved edges) instead of plain Material colors —
  no new image assets, all done with Compose gradients, shapes, and drawing:
  - The avatar placeholder is now a `MedallionEmblem`: a gold sweep-gradient
    ring around an embossed wood disc with an engraved shield silhouette
    (drawn via `Path`, not an image).
  - The total gold amount uses a serif, extra-bold `GlowingGoldText` — a
    dark "engraved" copy layered under a bright gold copy with a soft
    colored glow — next to the existing chest art for a touch of flavor.
  - Gold-per-second and Platinum Pieces now sit on a cream `ParchmentStrip`
    beneath the gold total instead of plain small text.
  - The buy-quantity button is now a `WoodenQuantityButton`: `CutCornerShape`
    (matching the angled corners on `FloatingMenu`'s wooden signs), a wood
    gradient, and a gold bevel highlight, instead of a plain white box.
  - The whole header sits on a `woodenBanner` — a wood-tone gradient with
    faint grain streaks and a carved shadow-and-gold-highlight line along
    the bottom edge — spanning the full width including behind the status
    bar, so it reads as one continuous plank across the top of the screen.
  - Skipped the optional dragon/wyrm silhouette flavor — no dragon art asset
    exists, and a hand-drawn `Path` silhouette wouldn't read as one
    convincingly the way the shield does for a simpler heraldic shape.
- Introduced `GameHeaderState` (bundles the four display values) and
  `GameHeaderColors` (the wood/gold/parchment palette, swappable via a
  `colors` param) so the header stays themeable and its parameter list
  doesn't sprawl. `GameHeader` itself still takes no `GameViewModel`
  reference — `GameScreen` collects the flows and assembles the state.
- Verified visually and functionally on the emulator: the new look renders
  correctly, the quantity button still cycles through all five states, and
  gold/coin-burst/plunder all still update live in the restyled header.

## [0.9.0] - 2026-09-04 11:48 PM EDT

- Replaced the plain "X gp" title bar with a new `GameHeader`: an avatar
  placeholder on the left (a plain circle for now — the real pre-created
  avatar picker isn't built yet), total Gold Pieces / gold-per-second /
  Platinum Pieces stacked in the middle, and a bulk-purchase quantity
  selector on the right.
- Added the quantity selector as a small tappable box cycling
  x1 → x10 → x100 → Next → Max → back to x1 (`BuyQuantity` enum, `ui/game/
  BuyQuantity.kt`). It's wired up and cycles correctly, but purely as a UI
  selection for now — nothing yet uses it to actually buy more than one unit
  at a time (needs bulk-purchase cost math, a separate task).
- Extended `GoldFormat`'s large-number suffixes: after the named short-scale
  list (K/M/B/T/Qa/Qi/Sx/Sp/Oc/No/Dc) runs out, it now continues indefinitely
  with letter suffixes (A, B, ... Z, AA, AB, ...), so the display never falls
  back to a raw digit string no matter how large the economy grows.
- Added gold-per-second display: sum of each owned lair's income rate
  (`incomePerCycle / baseProductionSeconds`), independent of Steward status.
- Fixed the header rendering underneath the status bar icons on first pass —
  a plain `Row` doesn't get the automatic inset handling `TopAppBar` provided
  for free, so it needs its own `Modifier.statusBarsPadding()`.
- Verified visually and functionally on the emulator: header no longer
  overlaps the status bar, the quantity selector cycles correctly through
  all five states, and gold/plunder still update live in the header.

## [0.8.0] - 2026-09-04 11:27 PM EDT

- Added a gold coin burst effect: tapping a ready lair card to manually
  plunder it now bursts a dozen small gold coins radially outward from the
  card's center, with a bit of gravity arc and a quick fade — pure `Canvas`
  drawing, no sprite asset, matching the project's stated vector/flat art
  style.
- Fires only on a manual tap, never on a Steward's automatic collection —
  the burst is triggered from inside the card's own click handler, which a
  Steward's auto-collect (running in `GameEngine`'s tick loop) never touches.
- New `CoinBurstOverlay` composable (`ui/game/CoinBurst.kt`), layered over
  `LairCard`'s content with no pointer input so it never blocks taps.
- Verified visually on the emulator across several frames of the animation:
  coins spread out and fade smoothly, and tapping still correctly plunders
  the lair.

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
