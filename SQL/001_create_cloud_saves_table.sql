-- Wyrm & Whelp: Idle Hoard — cloud save schema.
--
-- Run this once in the Supabase project's SQL editor (Dashboard > SQL Editor
-- > New query). Safe to re-run (uses IF NOT EXISTS / OR REPLACE).
--
-- Also required, NOT covered by this script (dashboard-only settings):
--   Authentication > Sign In / Providers > Anonymous Sign-Ins — must be
--   enabled, or GameEngine's anonymous sign-in will fail for every player.

create table if not exists public.cloud_saves (
  user_id uuid primary key references auth.users (id) on delete cascade,
  state jsonb not null,
  updated_at timestamptz not null default now()
);

alter table public.cloud_saves enable row level security;

drop policy if exists "Users can read their own save" on public.cloud_saves;
create policy "Users can read their own save"
  on public.cloud_saves for select
  using (auth.uid() = user_id);

drop policy if exists "Users can insert their own save" on public.cloud_saves;
create policy "Users can insert their own save"
  on public.cloud_saves for insert
  with check (auth.uid() = user_id);

drop policy if exists "Users can update their own save" on public.cloud_saves;
create policy "Users can update their own save"
  on public.cloud_saves for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
