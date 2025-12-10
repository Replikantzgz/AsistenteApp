-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- Create profiles table
create table public.profiles (
  id uuid references auth.users on delete cascade not null primary key,
  email text unique not null,
  full_name text,
  avatar_url text,
  plan text default 'eco' check (plan in ('eco', 'pro')),
  stripe_customer_id text,
  subscription_status text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Create api_keys table (securely store user keys)
create table public.api_keys (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid references public.profiles(id) on delete cascade not null,
  provider text not null check (provider in ('gemini', 'gemini_pro', 'openai')),
  key_value text not null, -- In production, this should be encrypted
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Create usage_logs table
create table public.usage_logs (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid references public.profiles(id) on delete cascade not null,
  date date default current_date not null,
  request_count int default 0,
  model_used text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  unique(user_id, date)
);

-- RLS Policies (Row Level Security)
alter table public.profiles enable row level security;
alter table public.api_keys enable row level security;
alter table public.usage_logs enable row level security;

create policy "Users can view own profile" on public.profiles
  for select using (auth.uid() = id);

create policy "Users can update own profile" on public.profiles
  for update using (auth.uid() = id);

create policy "Users can view own keys" on public.api_keys
  for select using (auth.uid() = user_id);

create policy "Users can insert own keys" on public.api_keys
  for insert with check (auth.uid() = user_id);

create policy "Users can update own keys" on public.api_keys
  for update using (auth.uid() = user_id);

-- Function to handle new user signup (if using Supabase Auth directly)
-- Since we use NextAuth, we will upsert manually, but this is good to have.
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email, full_name, avatar_url)
  values (new.id, new.email, new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'avatar_url');
  return new;
end;
$$ language plpgsql security definer;

-- Trigger for Supabase Auth (Optional if we sync via NextAuth)
-- create trigger on_auth_user_created
--   after insert on auth.users
--   for each row execute procedure public.handle_new_user();
