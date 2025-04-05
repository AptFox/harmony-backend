alter table users add column created_at timestamp with time zone default now();
alter table users add column updated_at timestamp with time zone default now();
