-- Wyrm & Whelp: Idle Hoard — reset cloud saves back to a brand-new game.
--
-- Run manually in the Supabase project's SQL Editor (Dashboard > SQL Editor
-- > New query) whenever you want every synced player's cloud save reset to
-- the exact starting state a fresh install begins with (0 gold, one owned
-- Kobold Warren, no upgrades/boosts/gems) — e.g. after a build whose local
-- save reset (see WyrmWhelpDatabase's version bump) should also apply to
-- accounts that sync to the cloud, since those would otherwise just
-- re-download their old progress on next launch.
--
-- ⚠️ THIS UPDATES EVERY ROW IN cloud_saves — every player who has ever
-- synced, not just a test account. Double-check this is really what you
-- want before running it; there is no undo. If you only want to reset your
-- own dev/test account, use the commented-out single-user version at the
-- bottom instead (swap in that account's real user_id first).
--
-- Local saves (each device's own Room database) are untouched by this —
-- it only resets the cloud row. A device that's still offline, or whose
-- local save is newer/further along than this reset, will simply
-- overwrite this reset again on its next sync (same merge-picks-the-
-- more-advanced-save logic described in GameStateExtensions.mergeGameStates).

update public.cloud_saves
set
  state = jsonb_build_object(
    'gold_pieces', 0,
    'platinum_pieces', 0,
    'gems', 0,
    'lifetime_gold_earned', 0,
    'everything_profit_upgrade_level', 0,
    'everything_speed_upgrade_level', 0,
    'gem_efficiency_level', 0,
    'lairs', jsonb_build_object(
      'kobold_warren', jsonb_build_object(
        'count', 1,
        'has_steward', false,
        'cycle_progress_seconds', 0,
        'is_loading', false,
        'completed_loads', 0,
        'profit_upgrade_level', 0,
        'speed_upgrade_level', 0
      )
    ),
    'offline_cap_hours', 4,
    'last_saved_at_epoch_millis', (extract(epoch from now()) * 1000)::bigint,
    'total_level_ups', 0,
    'permanent_speed_boost_2x_level', 0,
    'permanent_speed_boost_5x_level', 0,
    'permanent_speed_boost_10x_level', 0,
    'permanent_profit_boost_15x_level', 0,
    'permanent_profit_boost_2x_level', 0,
    'permanent_profit_boost_5x_level', 0,
    'permanent_gem_boost_15x_level', 0,
    'permanent_gem_boost_2x_level', 0,
    'permanent_gem_boost_5x_level', 0,
    'active_temporary_boosts', jsonb_build_array(),
    'last_platinum_ad_watched_at_epoch_millis', null,
    'speed_boost_ad_watch_timestamps_epoch_millis', jsonb_build_array(),
    'income_boost_ad_watch_timestamps_epoch_millis', jsonb_build_array(),
    'seen_steward_opportunities', jsonb_build_array(),
    'seen_upgrade_opportunities', jsonb_build_array()
  ),
  updated_at = now();

-- Single-user version — resets only one player's row. Find their user_id
-- via Authentication > Users in the dashboard (or `select user_id from
-- public.cloud_saves;` to list everyone who has ever synced), then:
--
-- update public.cloud_saves
-- set
--   state = jsonb_build_object( ... same jsonb_build_object(...) as above ... ),
--   updated_at = now()
-- where user_id = '00000000-0000-0000-0000-000000000000';
