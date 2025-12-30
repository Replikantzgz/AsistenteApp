-- Create notes table
create table if not exists notes (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users(id) on delete cascade not null,
  title text,
  content text,
  audio_url text,
  tags text[],
  is_pinned boolean default false,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Enable RLS
alter table notes enable row level security;

-- Create policy
create policy "Users can manage their own notes"
  on notes for all
  using (auth.uid() = user_id);

-- Create storage bucket for audio
insert into storage.buckets (id, name, public) 
values ('note-attachments', 'note-attachments', true)
on conflict (id) do nothing;

-- Storage policy
create policy "Users can upload note attachments"
  on storage.objects for insert
  with check ( bucket_id = 'note-attachments' and auth.uid()::text = (storage.foldername(name))[1] );

create policy "Users can view their own note attachments"
  on storage.objects for select
  using ( bucket_id = 'note-attachments' and auth.uid()::text = (storage.foldername(name))[1] );
