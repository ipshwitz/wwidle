-- Wyrm & Whelp: Idle Hoard — leaderboards.
--
-- Run this once in the Supabase project's SQL editor (Dashboard > SQL Editor
-- > New query). Safe to re-run (uses IF NOT EXISTS / OR REPLACE and
-- unschedules its own cron job before rescheduling it).
--
-- Three ranked boards, all keyed on GameState.lifetimeGoldEarned (never
-- resets, even across a Level Up — see CLAUDE.md's Level Up section):
--   all_time — lifetime_gold_earned itself, no baseline needed.
--   weekly   — earned since the most recent Sunday 11:59pm America/New_York.
--   monthly  — earned since the last day of the previous month, 11:59pm
--              America/New_York.
-- Only players with a `profiles` row (a real account with a username set —
-- see 003_create_profiles_table.sql) appear at all; guests are excluded
-- entirely, by design (the join below is what does the excluding).
--
-- Rankings are NOT computed live on every read — a `refresh_leaderboards()`
-- job recomputes all three boards once an hour (via pg_cron) into
-- `leaderboard_rankings`, which the app just reads straight off
-- (`order by rank limit 50`). This needs the pg_cron extension — the
-- `create extension` below enables it automatically on most Supabase
-- projects; if it errors, enable "pg_cron" first via Dashboard > Database >
-- Extensions, then re-run this whole script.

create extension if not exists pg_cron with schema extensions;

-- Each player's lifetime_gold_earned as of the start of the *current*
-- week/month, so "earned this period" = current value - this baseline. A
-- row's baseline only rolls over (see refresh_leaderboards below) once
-- we've actually crossed into a new period since it was last recorded — a
-- player who creates their account mid-period simply starts counting from
-- whenever they were first observed, not retroactively to the period's
-- real start. No client-facing RLS policies at all: only
-- refresh_leaderboards (SECURITY DEFINER, below) ever touches this table.
create table if not exists public.leaderboard_baselines (
  user_id uuid not null references auth.users (id) on delete cascade,
  period text not null check (period in ('weekly', 'monthly')),
  period_start timestamptz not null,
  baseline_gold double precision not null,
  primary key (user_id, period)
);

alter table public.leaderboard_baselines enable row level security;

-- What the app actually reads — pre-computed rank + gold-earned per player
-- per board, refreshed hourly (see the cron job at the bottom). Keeping the
-- app's own query trivial (`order by rank limit 50`) is the whole point of
-- precomputing this instead of ranking live on every leaderboard open.
create table if not exists public.leaderboard_rankings (
  period text not null check (period in ('all_time', 'weekly', 'monthly')),
  user_id uuid not null references auth.users (id) on delete cascade,
  username text not null,
  gold_earned double precision not null,
  rank int not null,
  computed_at timestamptz not null default now(),
  primary key (period, user_id)
);

create index if not exists leaderboard_rankings_period_rank_idx
  on public.leaderboard_rankings (period, rank);

alter table public.leaderboard_rankings enable row level security;

drop policy if exists "Leaderboard rankings are publicly readable" on public.leaderboard_rankings;
create policy "Leaderboard rankings are publicly readable"
  on public.leaderboard_rankings for select
  using (true);

-- Recomputes all three boards from scratch. SECURITY DEFINER so it can read
-- every player's cloud_saves row despite its own-row-only RLS (see
-- 001_create_cloud_saves_table.sql) — only this function, owned by
-- postgres and never exposed to clients directly, needs that access.
create or replace function public.refresh_leaderboards()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  now_est timestamp := now() at time zone 'America/New_York';
  -- The most recent Sunday 11:59pm America/New_York at or before now.
  week_start timestamptz := (date_trunc('week', now_est) - interval '1 day' + interval '23 hours 59 minutes') at time zone 'America/New_York';
  -- The last day of the previous month, 11:59pm America/New_York.
  month_start timestamptz := (date_trunc('month', now_est) - interval '1 minute') at time zone 'America/New_York';
begin
  insert into public.leaderboard_baselines (user_id, period, period_start, baseline_gold)
  select cs.user_id, 'weekly', week_start, coalesce((cs.state ->> 'lifetime_gold_earned')::double precision, 0)
  from public.cloud_saves cs
  join public.profiles p on p.user_id = cs.user_id
  on conflict (user_id, period) do update
    set period_start = excluded.period_start,
        baseline_gold = excluded.baseline_gold
    where public.leaderboard_baselines.period_start < excluded.period_start;

  insert into public.leaderboard_baselines (user_id, period, period_start, baseline_gold)
  select cs.user_id, 'monthly', month_start, coalesce((cs.state ->> 'lifetime_gold_earned')::double precision, 0)
  from public.cloud_saves cs
  join public.profiles p on p.user_id = cs.user_id
  on conflict (user_id, period) do update
    set period_start = excluded.period_start,
        baseline_gold = excluded.baseline_gold
    where public.leaderboard_baselines.period_start < excluded.period_start;

  delete from public.leaderboard_rankings where period = 'all_time';
  insert into public.leaderboard_rankings (period, user_id, username, gold_earned, rank, computed_at)
  select 'all_time', cs.user_id, p.username,
         coalesce((cs.state ->> 'lifetime_gold_earned')::double precision, 0),
         row_number() over (order by coalesce((cs.state ->> 'lifetime_gold_earned')::double precision, 0) desc),
         now()
  from public.cloud_saves cs
  join public.profiles p on p.user_id = cs.user_id;

  delete from public.leaderboard_rankings where period = 'weekly';
  insert into public.leaderboard_rankings (period, user_id, username, gold_earned, rank, computed_at)
  select 'weekly', cs.user_id, p.username,
         greatest(coalesce((cs.state ->> 'lifetime_gold_earned')::double precision, 0) - b.baseline_gold, 0),
         row_number() over (order by greatest(coalesce((cs.state ->> 'lifetime_gold_earned')::double precision, 0) - b.baseline_gold, 0) desc),
         now()
  from public.cloud_saves cs
  join public.profiles p on p.user_id = cs.user_id
  join public.leaderboard_baselines b on b.user_id = cs.user_id and b.period = 'weekly';

  delete from public.leaderboard_rankings where period = 'monthly';
  insert into public.leaderboard_rankings (period, user_id, username, gold_earned, rank, computed_at)
  select 'monthly', cs.user_id, p.username,
         greatest(coalesce((cs.state ->> 'lifetime_gold_earned')::double precision, 0) - b.baseline_gold, 0),
         row_number() over (order by greatest(coalesce((cs.state ->> 'lifetime_gold_earned')::double precision, 0) - b.baseline_gold, 0) desc),
         now()
  from public.cloud_saves cs
  join public.profiles p on p.user_id = cs.user_id
  join public.leaderboard_baselines b on b.user_id = cs.user_id and b.period = 'monthly';
end;
$$;

-- Run once immediately so the boards aren't empty until the first
-- scheduled tick fires.
select public.refresh_leaderboards();

-- Schedule the hourly refresh — unschedule any existing job of the same
-- name first so re-running this script doesn't stack duplicates.
do $$
begin
  if exists (select 1 from cron.job where jobname = 'refresh-leaderboards') then
    perform cron.unschedule('refresh-leaderboards');
  end if;
end $$;

select cron.schedule('refresh-leaderboards', '0 * * * *', 'select public.refresh_leaderboards();');
