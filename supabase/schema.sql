-- AppleTree: схема для Supabase.
-- Вставь целиком в Supabase -> SQL Editor -> Run.

create extension if not exists pgcrypto;

create table if not exists public.countries (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  color text not null default '#7FB069',
  emblem text not null default '🍎',
  leader text not null,
  members int not null default 1,
  score bigint not null default 0,
  regions int not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.players (
  id uuid primary key default gen_random_uuid(),
  nickname text not null unique,
  salt text not null,
  pass_hash text not null,
  token text not null,
  score bigint not null default 0,
  captured int not null default 0,
  country_id uuid references public.countries(id) on delete set null,
  created_at timestamptz not null default now()
);

create table if not exists public.applications (
  id uuid primary key default gen_random_uuid(),
  country_id uuid not null references public.countries(id) on delete cascade,
  nickname text not null,
  created_at timestamptz not null default now(),
  unique (country_id, nickname)
);

create table if not exists public.regions (
  id text primary key,
  name text not null,
  x double precision not null,
  y double precision not null,
  r double precision not null,
  owner_id uuid references public.countries(id) on delete set null,
  owner_name text,
  owner_color text,
  progress int not null default 0,
  progress_country_id uuid
);

insert into public.regions (id, name, x, y, r) values
  ('north',   'Северный сад',    30, 18, 13),
  ('pine',    'Сосновый край',   62, 15, 11),
  ('lake',    'Озёрный берег',   82, 30, 12),
  ('valley',  'Тихая долина',    18, 40, 14),
  ('orchard', 'Большой сад',     45, 38, 15),
  ('hills',   'Яблочные холмы',  72, 48, 13),
  ('river',   'Речные луга',     25, 62, 14),
  ('field',   'Пшеничные поля',  52, 63, 13),
  ('coast',   'Тёплый берег',    80, 68, 12),
  ('south',   'Южная роща',      33, 82, 12),
  ('meadow',  'Медовый луг',     60, 86, 13),
  ('cape',    'Мыс Ветров',      86, 87, 10)
on conflict (id) do nothing;

-- Доступ для приложения (publishable/anon ключ).
alter table public.countries    enable row level security;
alter table public.players      enable row level security;
alter table public.applications enable row level security;
alter table public.regions      enable row level security;

do $$
declare t text;
begin
  foreach t in array array['countries','players','applications','regions'] loop
    execute format('drop policy if exists anon_select on public.%I', t);
    execute format('drop policy if exists anon_insert on public.%I', t);
    execute format('drop policy if exists anon_update on public.%I', t);
    execute format('drop policy if exists anon_delete on public.%I', t);
    execute format('create policy anon_select on public.%I for select using (true)', t);
    execute format('create policy anon_insert on public.%I for insert with check (true)', t);
    execute format('create policy anon_update on public.%I for update using (true) with check (true)', t);
    execute format('create policy anon_delete on public.%I for delete using (true)', t);
    execute format('grant all on public.%I to anon, authenticated', t);
  end loop;
end $$;
