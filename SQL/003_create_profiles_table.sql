-- Wyrm & Whelp: Idle Hoard — leaderboard usernames.
--
-- Run this once in the Supabase project's SQL editor (Dashboard > SQL Editor
-- > New query). Safe to re-run (uses IF NOT EXISTS / OR REPLACE).
--
-- One row per player who has set a username — created the first time
-- GameViewModel.submitUsername() succeeds, after a guest finishes
-- registering a real account (see AuthRepository.setUsername). Usernames
-- are unique case-insensitively ("Bob" and "bob" collide) via the
-- functional index below rather than a plain UNIQUE column constraint.
--
-- Deliberately a separate table from cloud_saves rather than a new column
-- on it: a username is public (anyone can read it, for a future
-- leaderboard join), while cloud_saves.state is private to its own user —
-- mixing the two would mean relaxing RLS on the whole save blob just to
-- expose a name.

create table if not exists public.profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  username text not null,
  created_at timestamptz not null default now()
);

create unique index if not exists profiles_username_lower_idx
  on public.profiles (lower(username));

alter table public.profiles enable row level security;

-- Public read: a future leaderboard needs to display everyone's username,
-- not just the current player's own.
drop policy if exists "Anyone can read usernames" on public.profiles;
create policy "Anyone can read usernames"
  on public.profiles for select
  using (true);

drop policy if exists "Users can set their own username" on public.profiles;
create policy "Users can set their own username"
  on public.profiles for insert
  with check (auth.uid() = user_id);

drop policy if exists "Users can change their own username" on public.profiles;
create policy "Users can change their own username"
  on public.profiles for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
