# Changelog

All notable changes to Wyrm & Whelp: Idle Hoard, newest first. Dates/times are US
Eastern (EST/EDT). See [CLAUDE.md](CLAUDE.md) for the living architecture doc.

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
