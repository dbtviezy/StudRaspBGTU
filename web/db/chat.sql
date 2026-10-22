-- Chat schema for Supabase (run once in Supabase → SQL Editor).
-- Accounts live in Neon; our API (/api/chat/token) mints a real Supabase Auth JWT for the same
-- user id (via service role), so auth.uid() here matches Neon `users.id` and the display name
-- comes from Auth user_metadata.name.

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
  role      text not null default 'member' check (role in ('owner', 'helper', 'member')),
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
-- v1.2: one row per restriction: 'all' (full ban), 'chat' (can't write), 'chat_create' (can't start chats or DMs).
-- Filled by our server (/api/admin) from the bans in Neon.
alter table public.chat_bans add column if not exists scope text not null default 'all';
alter table public.chat_bans drop constraint if exists chat_bans_pkey;
alter table public.chat_bans add primary key (user_id, scope);

-- Class reps confirmed by the moderators; they get helper rights in every chat of their group.
create table if not exists public.class_reps (
  user_id  uuid primary key,
  group_id text not null,
  name     text not null default ''
);
alter table public.class_reps enable row level security;
drop policy if exists class_reps_read on public.class_reps;
create policy class_reps_read on public.class_reps for select to authenticated using (true);

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
  select exists (select 1 from chat_bans where user_id = auth.uid() and scope = 'all' and (until is null or until > now()))
$$;

create or replace function public.is_restricted(p_scope text) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from chat_bans
    where user_id = auth.uid() and scope in ('all', p_scope) and (until is null or until > now())
  )
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
  with check (user_id = auth.uid() and public.is_member(chat_id) and not public.is_restricted('chat'));

drop policy if exists messages_delete_own on public.messages;
create policy messages_delete_own on public.messages for delete to authenticated
  using (user_id = auth.uid());

-- Author fields are filled from the JWT, and each account may send at most 30 messages a minute.
create or replace function public.messages_before_insert() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if public.is_restricted('chat') then raise exception 'muted' using errcode = 'P0001'; end if;
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
  if public.is_restricted('chat_create') then raise exception 'no_create' using errcode = 'P0001'; end if;
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
  if public.is_restricted('chat_create') then raise exception 'no_create' using errcode = 'P0001'; end if;
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

-- v1.2: the chat creator (role 'owner') can make members helpers.
alter table public.chat_members drop constraint if exists chat_members_role_check;
alter table public.chat_members add constraint chat_members_role_check check (role in ('owner', 'helper', 'member'));

-- The pinned text is copied, so it stays after the message itself expires.
alter table public.chats add column if not exists pinned_body text;
alter table public.chats add column if not exists pinned_author text;
alter table public.chats add column if not exists pinned_at timestamptz;

create or replace function public.is_staff(c uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from chat_members m join chats ch on ch.id = m.chat_id
    where m.chat_id = c and m.user_id = auth.uid() and m.accepted and ch.kind = 'group'
      and (m.role in ('owner', 'helper')
        or exists (select 1 from class_reps r where r.user_id = m.user_id and r.group_id = ch.group_id))
  )
$$;

create or replace function public.set_member_role(p_chat uuid, p_user uuid, p_role text) returns void
language plpgsql security definer set search_path = public as $$
begin
  if p_role not in ('helper', 'member') then raise exception 'bad_request' using errcode = 'P0001'; end if;
  if not exists (select 1 from chat_members where chat_id = p_chat and user_id = auth.uid() and role = 'owner') then
    raise exception 'forbidden' using errcode = 'P0001';
  end if;
  update chat_members set role = p_role where chat_id = p_chat and user_id = p_user and role <> 'owner';
end $$;

-- p_message null unpins.
create or replace function public.pin_message(p_chat uuid, p_message bigint) returns void
language plpgsql security definer set search_path = public as $$
declare
  m messages;
begin
  if not public.is_staff(p_chat) then raise exception 'forbidden' using errcode = 'P0001'; end if;
  if p_message is null then
    update chats set pinned_body = null, pinned_author = null, pinned_at = null where id = p_chat;
    return;
  end if;
  select * into m from messages where id = p_message and chat_id = p_chat;
  if m.id is null then raise exception 'bad_request' using errcode = 'P0001'; end if;
  update chats set pinned_body = m.body, pinned_author = m.author, pinned_at = now() where id = p_chat;
end $$;

create table if not exists public.polls (
  id         bigserial primary key,
  chat_id    uuid not null references public.chats (id) on delete cascade,
  question   varchar(200) not null check (length(btrim(question)) > 0),
  options    text[] not null check (array_length(options, 1) between 2 and 8),
  multiple   boolean not null default false,
  closed     boolean not null default false,
  created_by uuid not null,
  author     text not null,
  created_at timestamptz not null default now()
);
create index if not exists polls_chat_idx on public.polls (chat_id, id);

create table if not exists public.poll_votes (
  poll_id bigint not null references public.polls (id) on delete cascade,
  user_id uuid not null,
  choice  smallint not null,
  primary key (poll_id, user_id, choice)
);

-- Read and written only through the functions below.
alter table public.polls enable row level security;
alter table public.poll_votes enable row level security;

create or replace function public.create_poll(p_chat uuid, p_question text, p_options text[], p_multiple boolean) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  opts text[];
  new_id bigint;
begin
  if not public.is_staff(p_chat) then raise exception 'forbidden' using errcode = 'P0001'; end if;
  if public.is_restricted('chat') then raise exception 'muted' using errcode = 'P0001'; end if;
  select array_agg(left(btrim(o), 100) order by n) into opts
    from unnest(p_options) with ordinality as t(o, n) where btrim(coalesce(o, '')) <> '';
  if btrim(coalesce(p_question, '')) = '' or coalesce(array_length(opts, 1), 0) not between 2 and 8 then
    raise exception 'bad_request' using errcode = 'P0001';
  end if;
  if (select count(*) from polls where created_by = auth.uid() and created_at > now() - interval '1 day') >= 20 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  insert into polls (chat_id, question, options, multiple, created_by, author)
    values (p_chat, left(btrim(p_question), 200), opts, coalesce(p_multiple, false), auth.uid(), public.my_name())
    returning id into new_id;
  return new_id;
end $$;

-- An empty array takes the vote back.
create or replace function public.vote_poll(p_poll bigint, p_choices int[]) returns void
language plpgsql security definer set search_path = public as $$
declare
  p polls;
  picked int[] := coalesce(p_choices, '{}');
begin
  select * into p from polls where id = p_poll;
  if public.is_restricted('chat') then raise exception 'muted' using errcode = 'P0001'; end if;
  if p.id is null or not public.is_member(p.chat_id) or p.closed then
    raise exception 'forbidden' using errcode = 'P0001';
  end if;
  if (not p.multiple and coalesce(array_length(picked, 1), 0) > 1)
    or exists (select 1 from unnest(picked) c where c < 0 or c >= array_length(p.options, 1)) then
    raise exception 'bad_request' using errcode = 'P0001';
  end if;
  delete from poll_votes where poll_id = p_poll and user_id = auth.uid();
  insert into poll_votes (poll_id, user_id, choice) select distinct p_poll, auth.uid(), c::smallint from unnest(picked) c;
end $$;

create or replace function public.close_poll(p_poll bigint, p_delete boolean) returns void
language plpgsql security definer set search_path = public as $$
declare
  c uuid;
begin
  select chat_id into c from polls where id = p_poll;
  if c is null or not public.is_staff(c) then raise exception 'forbidden' using errcode = 'P0001'; end if;
  if p_delete then delete from polls where id = p_poll; else update polls set closed = true where id = p_poll; end if;
end $$;

create or replace function public.chat_polls(p_chat uuid)
returns table (
  id bigint, question text, options text[], multiple boolean, closed boolean, author text,
  created_at timestamptz, counts int[], mine int[], voters int
)
language sql stable security definer set search_path = public as $$
  select p.id, p.question::text, p.options, p.multiple, p.closed, p.author, p.created_at,
    array(
      select count(v.user_id)::int from generate_series(0, array_length(p.options, 1) - 1) i
      left join poll_votes v on v.poll_id = p.id and v.choice = i
      group by i order by i
    ),
    array(select v.choice::int from poll_votes v where v.poll_id = p.id and v.user_id = auth.uid() order by v.choice),
    (select count(distinct v.user_id)::int from poll_votes v where v.poll_id = p.id)
  from polls p
  where p.chat_id = p_chat and public.is_member(p_chat)
  order by p.id desc
  limit 20
$$;

-- Deadlines the class rep posts for the whole group chat.
create table if not exists public.chat_tasks (
  id         bigserial primary key,
  chat_id    uuid not null references public.chats (id) on delete cascade,
  title      varchar(200) not null check (length(btrim(title)) > 0),
  note       varchar(500) not null default '',
  due_date   date,
  due_time   text check (due_time is null or due_time ~ '^\d{2}:\d{2}$'),
  created_by uuid not null,
  author     text not null,
  created_at timestamptz not null default now()
);
create index if not exists chat_tasks_chat_idx on public.chat_tasks (chat_id);
alter table public.chat_tasks enable row level security;

drop policy if exists chat_tasks_read on public.chat_tasks;
create policy chat_tasks_read on public.chat_tasks for select to authenticated
  using (public.is_member(chat_id));

create or replace function public.add_chat_task(p_chat uuid, p_title text, p_note text, p_due_date date, p_due_time text) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  new_id bigint;
begin
  if not public.is_staff(p_chat) then raise exception 'forbidden' using errcode = 'P0001'; end if;
  if public.is_restricted('chat') then raise exception 'muted' using errcode = 'P0001'; end if;
  if btrim(coalesce(p_title, '')) = '' or (p_due_time is not null and p_due_time !~ '^\d{2}:\d{2}$') then
    raise exception 'bad_request' using errcode = 'P0001';
  end if;
  if (select count(*) from chat_tasks where created_by = auth.uid() and created_at > now() - interval '1 day') >= 30 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  insert into chat_tasks (chat_id, title, note, due_date, due_time, created_by, author)
    values (p_chat, left(btrim(p_title), 200), left(btrim(coalesce(p_note, '')), 500), p_due_date,
      case when p_due_date is null then null else p_due_time end, auth.uid(), public.my_name())
    returning id into new_id;
  return new_id;
end $$;

create or replace function public.delete_chat_task(p_task bigint) returns void
language plpgsql security definer set search_path = public as $$
declare
  c uuid;
begin
  select chat_id into c from chat_tasks where id = p_task;
  if c is null or not public.is_staff(c) then raise exception 'forbidden' using errcode = 'P0001'; end if;
  delete from chat_tasks where id = p_task;
end $$;

drop function if exists public.my_chats();
create or replace function public.my_chats()
returns table (
  id uuid, kind text, title text, group_id text, invite_code text, role text, accepted boolean,
  peer_name text, last_id bigint, last_body text, last_author text, last_at timestamptz, unread int,
  pinned_body text, pinned_author text, pinned_at timestamptz, rep boolean
)
language sql stable security definer set search_path = public as $$
  select c.id, c.kind, c.title, c.group_id,
    case when me.role = 'owner' or c.kind = 'group' then c.invite_code end,
    me.role, me.accepted,
    (select p.name from chat_members p where p.chat_id = c.id and p.user_id <> auth.uid() limit 1),
    l.id, l.body, l.author, l.created_at,
    (select count(*)::int from messages u where u.chat_id = c.id and u.id > me.last_read and u.user_id <> auth.uid()),
    c.pinned_body, c.pinned_author, c.pinned_at,
    c.kind = 'group' and exists (select 1 from class_reps r where r.user_id = auth.uid() and r.group_id = c.group_id)
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
revoke all on function public.set_member_role(uuid, uuid, text) from public, anon;
revoke all on function public.pin_message(uuid, bigint) from public, anon;
revoke all on function public.create_poll(uuid, text, text[], boolean) from public, anon;
revoke all on function public.vote_poll(bigint, int[]) from public, anon;
revoke all on function public.close_poll(bigint, boolean) from public, anon;
revoke all on function public.chat_polls(uuid) from public, anon;
revoke all on function public.add_chat_task(uuid, text, text, date, text) from public, anon;
revoke all on function public.delete_chat_task(bigint) from public, anon;
grant execute on function public.set_member_role(uuid, uuid, text) to authenticated;
grant execute on function public.pin_message(uuid, bigint) to authenticated;
grant execute on function public.create_poll(uuid, text, text[], boolean) to authenticated;
grant execute on function public.vote_poll(bigint, int[]) to authenticated;
grant execute on function public.close_poll(bigint, boolean) to authenticated;
grant execute on function public.chat_polls(uuid) to authenticated;
grant execute on function public.add_chat_task(uuid, text, text, date, text) to authenticated;
grant execute on function public.delete_chat_task(bigint) to authenticated;
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

-- Polls live 30 days; group tasks two weeks after their due date (60 days without one).
select cron.schedule('chat-extras-retention', '30 3 * * *', $$
  delete from public.polls where created_at < now() - interval '30 days';
  delete from public.chat_tasks
  where (due_date is not null and due_date < current_date - 14)
     or (due_date is null and created_at < now() - interval '60 days');
$$);

-- Web push: after every new message Supabase calls our server, which sends the notifications.
-- Needs the pg_net extension (Database → Extensions → pg_net) and one row with the server address and
-- the same secret as PUSH_WEBHOOK_SECRET on Vercel:
--   insert into public.push_config (url, secret)
--   values ('https://studraspbgtu-web.vercel.app/api/push/chat', '<PUSH_WEBHOOK_SECRET>');
create extension if not exists pg_net;

create table if not exists public.push_config (
  id     int primary key default 1 check (id = 1),
  url    text not null,
  secret text not null
);
alter table public.push_config enable row level security;

create or replace function public.messages_after_insert() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  cfg push_config;
begin
  select * into cfg from push_config where id = 1;
  if cfg.url is not null then
    perform net.http_post(
      url := cfg.url,
      body := jsonb_build_object('chat_id', new.chat_id, 'user_id', new.user_id, 'author', new.author, 'body', left(new.body, 300)),
      headers := jsonb_build_object('Content-Type', 'application/json', 'X-Push-Secret', cfg.secret)
    );
  end if;
  return new;
end $$;

drop trigger if exists messages_after_insert on public.messages;
create trigger messages_after_insert after insert on public.messages
  for each row execute function public.messages_after_insert();
