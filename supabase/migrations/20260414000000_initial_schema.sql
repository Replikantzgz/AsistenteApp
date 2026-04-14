-- ============================================================
-- Alfred App — Schema inicial
-- Ejecutar en Supabase Dashboard → SQL Editor
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. PROFILES
-- Se crea automáticamente al registrarse un usuario
-- ────────────────────────────────────────────────────────────
create table if not exists public.profiles (
    id              uuid primary key references auth.users(id) on delete cascade,
    email           text unique not null,
    full_name       text,
    avatar_url      text,
    plan            text not null default 'free',          -- 'free' | 'eco' | 'pro'
    subscription_status text not null default 'inactive',  -- 'active' | 'inactive' | 'trialing'
    stripe_customer_id  text,
    referral_code   text unique default substr(md5(random()::text), 1, 8),
    referral_balance numeric(10,2) not null default 0,
    referred_by     uuid references public.profiles(id),
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

-- Trigger: crear perfil automáticamente al hacer signup
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer as $$
begin
    insert into public.profiles (id, email, full_name, avatar_url)
    values (
        new.id,
        new.email,
        coalesce(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1)),
        new.raw_user_meta_data->>'avatar_url'
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ────────────────────────────────────────────────────────────
-- 2. NOTES
-- ────────────────────────────────────────────────────────────
create table if not exists public.notes (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references auth.users(id) on delete cascade,
    title       text,
    content     text,
    audio_url   text,
    tags        text[] not null default '{}',
    is_pinned   boolean not null default false,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create index if not exists notes_user_id_idx on public.notes(user_id);
create index if not exists notes_created_at_idx on public.notes(user_id, created_at desc);

-- ────────────────────────────────────────────────────────────
-- 3. REFERRALS
-- ────────────────────────────────────────────────────────────
create table if not exists public.referrals (
    id          uuid primary key default gen_random_uuid(),
    referrer_id uuid not null references public.profiles(id) on delete cascade,
    referred_id uuid not null references public.profiles(id) on delete cascade,
    reward_paid boolean not null default false,
    created_at  timestamptz not null default now(),
    unique(referrer_id, referred_id)
);

-- ────────────────────────────────────────────────────────────
-- 4. RPC: increment_referral_balance
-- ────────────────────────────────────────────────────────────
create or replace function public.increment_referral_balance(user_id uuid, amount numeric)
returns void language plpgsql security definer as $$
begin
    update public.profiles
    set referral_balance = referral_balance + amount,
        updated_at = now()
    where id = user_id;
end;
$$;

-- ────────────────────────────────────────────────────────────
-- 5. RLS (Row Level Security)
-- ────────────────────────────────────────────────────────────

-- profiles
alter table public.profiles enable row level security;
create policy "Users can view own profile"
    on public.profiles for select using (auth.uid() = id);
create policy "Users can update own profile"
    on public.profiles for update using (auth.uid() = id);

-- notes
alter table public.notes enable row level security;
create policy "Users can manage own notes"
    on public.notes for all using (auth.uid() = user_id);

-- referrals
alter table public.referrals enable row level security;
create policy "Users can view own referrals"
    on public.referrals for select
    using (auth.uid() = referrer_id or auth.uid() = referred_id);
create policy "Users can create referrals"
    on public.referrals for insert with check (true);

-- ────────────────────────────────────────────────────────────
-- 6. STORAGE: note-attachments
-- ────────────────────────────────────────────────────────────
insert into storage.buckets (id, name, public)
values ('note-attachments', 'note-attachments', true)
on conflict (id) do nothing;

create policy "Users can upload audio notes"
    on storage.objects for insert
    with check (bucket_id = 'note-attachments' and auth.uid() is not null);

create policy "Public read for audio notes"
    on storage.objects for select
    using (bucket_id = 'note-attachments');
