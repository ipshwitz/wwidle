-- Wyrm & Whelp: Idle Hoard — auto-generated "AnonymousXXXXXX" usernames.
--
-- Run this once in the Supabase project's SQL editor (Dashboard > SQL Editor
-- > New query). Safe to re-run (uses CREATE OR REPLACE / IF NOT EXISTS /
-- ON CONFLICT).
--
-- Every player — guest or signed-in — gets a placeholder "AnonymousNNNNNN"
-- username the instant their auth.users row is created, so everyone shows
-- up on the leaderboard from session one instead of only players who've
-- explicitly set a real name (see 003/004 — refresh_leaderboards()'s INNER
-- JOIN against profiles was the actual mechanism excluding anyone without a
-- row). A signed-in player can overwrite this any time via the existing
-- Settings username field (GameViewModel.submitUsername); a guest can't —
-- AccountCard shows it read-only with a "sign in to change it" hint — until
-- they sign up for a real account.
--
-- The number itself comes from a real sequence, not random(): guarantees no
-- collision against profiles_username_lower_idx's uniqueness, so no retry
-- logic is needed anywhere that reads from it.

create sequence if not exists public.anonymous_username_seq start 100000;

-- Standard Supabase "auto-create a profile row on signup" trigger pattern.
-- Fires once per real auth.users INSERT — which, in this app, only ever
-- happens at AuthRepository.ensureSignedIn()'s anonymous sign-in moment:
-- both signUp (upgrading that session to a real email via updateUser) and
-- signIn (switching to a different, already-existing account) never insert
-- a new auth.users row, so this one trigger covers every player exactly
-- once regardless of whether they ever go on to add an email.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (user_id, username)
  values (new.id, 'Anonymous' || nextval('public.anonymous_username_seq')::text)
  on conflict (user_id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Backfill: every account that predates this script (guest or signed-in)
-- and has no profiles row yet gets one too, so this isn't only a
-- going-forward fix — existing installs show up on the leaderboard the
-- next time refresh_leaderboards() runs, with no app update needed.
insert into public.profiles (user_id, username)
select u.id, 'Anonymous' || nextval('public.anonymous_username_seq')::text
from auth.users u
left join public.profiles p on p.user_id = u.id
where p.user_id is null;

-- Lets a session regenerate its OWN placeholder name — used by Settings'
-- "Reset Account" (a fresh run gets a fresh anonymous identity, same
-- treatment as everything else Reset wipes back to a blank slate) instead
-- of the old delete-the-row approach, which would have left a player with
-- no profiles row at all — breaking the "everyone always has a username"
-- guarantee this script exists to establish. SECURITY DEFINER so it can
-- pull from the sequence, but the WHERE clause pins the update to
-- auth.uid(), so it can never touch anyone else's row even though it runs
-- with elevated privilege — same safety shape as delete_own_account() (005).
create or replace function public.regenerate_own_username()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.profiles
  set username = 'Anonymous' || nextval('public.anonymous_username_seq')::text
  where user_id = auth.uid();
end;
$$;

revoke all on function public.regenerate_own_username() from public;
grant execute on function public.regenerate_own_username() to authenticated;
