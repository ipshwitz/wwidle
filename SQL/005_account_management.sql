-- Wyrm & Whelp: Idle Hoard — Settings' "Delete Account" and "Reset Account".
--
-- Run this once in the Supabase project's SQL editor (Dashboard > SQL Editor
-- > New query). Safe to re-run (uses CREATE OR REPLACE / DROP POLICY IF EXISTS).

-- ---------------------------------------------------------------------------
-- Self-service account deletion ("Delete Account")
-- ---------------------------------------------------------------------------
-- The client SDK has no "delete my own account" call — actually removing a
-- row from auth.users needs privileges no authenticated client role has, and
-- the real admin API needs a service-role key, which must never ship inside
-- the app. This SECURITY DEFINER function is the standard Supabase pattern
-- for self-service deletion instead: it runs as its owner (postgres, which
-- CAN write to auth.users) but only ever deletes auth.uid()'s OWN row, never
-- a caller-supplied id, so granting it to every authenticated session
-- (anonymous guests included — an anonymous sign-in is still a real
-- auth.users row with a real auth.uid()) is safe.
--
-- cloud_saves, profiles, leaderboard_baselines, and leaderboard_rankings all
-- already declare "references auth.users (id) on delete cascade" (see
-- 001/003/004), so this one delete quietly cleans up every other table too —
-- nothing else to add here.
create or replace function public.delete_own_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from auth.users where id = auth.uid();
end;
$$;

revoke all on function public.delete_own_account() from public;
grant execute on function public.delete_own_account() to authenticated;

-- ---------------------------------------------------------------------------
-- Let a player clear their own username ("Reset Account")
-- ---------------------------------------------------------------------------
-- profiles (003) had no delete policy before this — Account Reset removes a
-- player's chosen username along with their game progress, and since
-- profiles.username is `not null`, removing the row entirely (rather than
-- blanking the column) is what makes a later AuthRepository.currentUsername()
-- read go back to null.
drop policy if exists "Users can delete their own username" on public.profiles;
create policy "Users can delete their own username"
  on public.profiles for delete
  using (auth.uid() = user_id);
