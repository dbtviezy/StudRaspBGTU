-- Chat schema for Supabase (run once in Supabase → SQL Editor).
-- Accounts live in Neon; our API (/api/chat/token) signs Supabase JWTs with SUPABASE_JWT_SECRET,
-- so auth.uid() here is the user id from the Neon `users` table and the display name comes from
-- the signed claim user_metadata.name.

create extension if not exists pgcrypto;

create table if not exists public.chats (
  id          uuid primary key default gen_random_uuid(),
  kind        text not null check (kind in ('group', 'dm')),
  title       text not null default '',
  group_id    text,
  invite_code text unique,
  created_by  uuid not null,
  created_at  timestamptz not null default now()
);

create table if not exists public.chat_members (
  chat_id   uuid not null references public.chats (id) on delete cascade,
  user_id   uuid not null,
  name      text not null,
  role      text not null default 'member' check (role in ('owner', 'member')),
  accepted  boolean not null default true,
  last_read bigint not null default 0,
  joined_at timestamptz not null default now(),
  primary key (chat_id, user_id)
);
create index if not exists chat_members_user_idx on public.chat_members (user_id);

create table if not exists public.messages (
  id         bigserial primary key,
  chat_id    uuid not null references public.chats (id) on delete cascade,
  user_id    uuid not null,
  author     text not null,
  body       varchar(1000) not null check (length(btrim(body)) > 0),
  client_id  uuid not null unique,
  created_at timestamptz not null default now()
);
create index if not exists messages_chat_idx on public.messages (chat_id, id);

create table if not exists public.chat_bans (
  user_id uuid primary key,
  until   timestamptz,
  reason  text not null default ''
);

create or replace function public.my_name() returns text
language sql stable as $$
  select coalesce(nullif(auth.jwt() -> 'user_metadata' ->> 'name', ''), 'Студент')
$$;

create or replace function public.is_member(c uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from chat_members where chat_id = c and user_id = auth.uid() and accepted)
$$;

create or replace function public.is_banned() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from chat_bans where user_id = auth.uid() and (until is null or until > now()))
$$;

alter table public.chats enable row level security;
alter table public.chat_members enable row level security;
alter table public.messages enable row level security;
alter table public.chat_bans enable row level security;

drop policy if exists chats_read on public.chats;
create policy chats_read on public.chats for select to authenticated
  using (exists (select 1 from public.chat_members m where m.chat_id = chats.id and m.user_id = auth.uid()));

drop policy if exists members_read on public.chat_members;
create policy members_read on public.chat_members for select to authenticated
  using (user_id = auth.uid() or public.is_member(chat_id));

drop policy if exists messages_read on public.messages;
create policy messages_read on public.messages for select to authenticated
  using (public.is_member(chat_id));

drop policy if exists messages_write on public.messages;
create policy messages_write on public.messages for insert to authenticated
  with check (user_id = auth.uid() and public.is_member(chat_id) and not public.is_banned());

drop policy if exists messages_delete_own on public.messages;
create policy messages_delete_own on public.messages for delete to authenticated
  using (user_id = auth.uid());

-- Author fields are filled from the JWT, and each account may send at most 30 messages a minute.
create or replace function public.messages_before_insert() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  new.user_id := auth.uid();
  new.author := public.my_name();
  new.created_at := now();
  if (select count(*) from messages where user_id = new.user_id and created_at > now() - interval '1 minute') >= 30 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  return new;
end $$;

drop trigger if exists messages_before_insert on public.messages;
create trigger messages_before_insert before insert on public.messages
  for each row execute function public.messages_before_insert();

create or replace function public.new_invite_code() returns text
language sql volatile as $$
  select string_agg(substr('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + floor(random() * 32)::int, 1), '')
  from generate_series(1, 8)
$$;

create or replace function public.create_group_chat(p_group text, p_title text) returns public.chats
language plpgsql security definer set search_path = public as $$
declare
  chat chats;
begin
  if auth.uid() is null or public.is_banned() then raise exception 'forbidden' using errcode = 'P0001'; end if;
  if (select count(*) from chats where created_by = auth.uid() and created_at > now() - interval '1 day') >= 5 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  insert into chats (kind, title, group_id, invite_code, created_by)
    values ('group', left(btrim(coalesce(p_title, '')), 60), left(p_group, 40), public.new_invite_code(), auth.uid())
    returning * into chat;
  insert into chat_members (chat_id, user_id, name, role) values (chat.id, auth.uid(), public.my_name(), 'owner');
  return chat;
end $$;

create or replace function public.join_chat(p_code text) returns public.chats
language plpgsql security definer set search_path = public as $$
declare
  chat chats;
begin
  if auth.uid() is null or public.is_banned() then raise exception 'forbidden' using errcode = 'P0001'; end if;
  select * into chat from chats where invite_code = upper(btrim(p_code)) and kind = 'group';
  if chat.id is null then raise exception 'invalid_code' using errcode = 'P0001'; end if;
  insert into chat_members (chat_id, user_id, name) values (chat.id, auth.uid(), public.my_name())
    on conflict (chat_id, user_id) do update set accepted = true, name = excluded.name;
  return chat;
end $$;

create or replace function public.regenerate_invite(p_chat uuid) returns text
language plpgsql security definer set search_path = public as $$
declare
  code text := public.new_invite_code();
begin
  if not exists (select 1 from chat_members where chat_id = p_chat and user_id = auth.uid() and role = 'owner') then
    raise exception 'forbidden' using errcode = 'P0001';
  end if;
  update chats set invite_code = code where id = p_chat;
  return code;
end $$;

-- The peer sees the chat as a request until they accept it; their name is taken from their own token then.
create or replace function public.start_dm(p_peer uuid, p_peer_name text) returns public.chats
language plpgsql security definer set search_path = public as $$
declare
  chat chats;
begin
  if auth.uid() is null or public.is_banned() then raise exception 'forbidden' using errcode = 'P0001'; end if;
  if p_peer = auth.uid() then raise exception 'bad_request' using errcode = 'P0001'; end if;
  select c.* into chat from chats c
    where c.kind = 'dm'
      and exists (select 1 from chat_members a where a.chat_id = c.id and a.user_id = auth.uid())
      and exists (select 1 from chat_members b where b.chat_id = c.id and b.user_id = p_peer)
    limit 1;
  if chat.id is not null then return chat; end if;
  if (select count(*) from chats where created_by = auth.uid() and kind = 'dm' and created_at > now() - interval '1 day') >= 20 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  insert into chats (kind, created_by) values ('dm', auth.uid()) returning * into chat;
  insert into chat_members (chat_id, user_id, name, role) values (chat.id, auth.uid(), public.my_name(), 'owner');
  insert into chat_members (chat_id, user_id, name, accepted)
    values (chat.id, p_peer, left(coalesce(nullif(btrim(p_peer_name), ''), 'Студент'), 40), false);
  return chat;
end $$;

create or replace function public.accept_chat(p_chat uuid) returns void
language sql security definer set search_path = public as $$
  update chat_members set accepted = true, name = public.my_name()
  where chat_id = p_chat and user_id = auth.uid()
$$;

create or replace function public.leave_chat(p_chat uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  delete from chat_members where chat_id = p_chat and user_id = auth.uid();
  delete from chats c where c.id = p_chat
    and (c.kind = 'dm' or not exists (select 1 from chat_members m where m.chat_id = c.id));
end $$;

create or replace function public.mark_read(p_chat uuid, p_id bigint) returns void
language sql security definer set search_path = public as $$
  update chat_members set last_read = greatest(last_read, p_id)
  where chat_id = p_chat and user_id = auth.uid()
$$;

create or replace function public.my_chats()
returns table (
  id uuid, kind text, title text, group_id text, invite_code text, role text, accepted boolean,
  peer_name text, last_id bigint, last_body text, last_author text, last_at timestamptz, unread int
)
language sql stable security definer set search_path = public as $$
  select c.id, c.kind, c.title, c.group_id,
    case when me.role = 'owner' or c.kind = 'group' then c.invite_code end,
    me.role, me.accepted,
    (select p.name from chat_members p where p.chat_id = c.id and p.user_id <> auth.uid() limit 1),
    l.id, l.body, l.author, l.created_at,
    (select count(*)::int from messages u where u.chat_id = c.id and u.id > me.last_read and u.user_id <> auth.uid())
  from chat_members me
  join chats c on c.id = me.chat_id
  left join lateral (select * from messages m where m.chat_id = c.id order by m.id desc limit 1) l on true
  where me.user_id = auth.uid()
  order by coalesce(l.created_at, c.created_at) desc
$$;

revoke all on function public.create_group_chat(text, text) from public, anon;
revoke all on function public.join_chat(text) from public, anon;
revoke all on function public.regenerate_invite(uuid) from public, anon;
revoke all on function public.start_dm(uuid, text) from public, anon;
revoke all on function public.accept_chat(uuid) from public, anon;
revoke all on function public.leave_chat(uuid) from public, anon;
revoke all on function public.mark_read(uuid, bigint) from public, anon;
revoke all on function public.my_chats() from public, anon;
grant execute on function public.create_group_chat(text, text) to authenticated;
grant execute on function public.join_chat(text) to authenticated;
grant execute on function public.regenerate_invite(uuid) to authenticated;
grant execute on function public.start_dm(uuid, text) to authenticated;
grant execute on function public.accept_chat(uuid) to authenticated;
grant execute on function public.leave_chat(uuid) to authenticated;
grant execute on function public.mark_read(uuid, bigint) to authenticated;
grant execute on function public.my_chats() to authenticated;

-- Live updates for open chats.
do $$ begin
  alter publication supabase_realtime add table public.messages;
exception when duplicate_object then null; end $$;

-- Retention: group messages 7 days, direct messages 72 hours (needs the pg_cron extension:
-- Database → Extensions → pg_cron).
create extension if not exists pg_cron;
select cron.schedule('chat-retention', '15 * * * *', $$
  delete from public.messages m using public.chats c
  where m.chat_id = c.id
    and ((c.kind = 'group' and m.created_at < now() - interval '7 days')
      or (c.kind = 'dm' and m.created_at < now() - interval '72 hours'))
$$);
