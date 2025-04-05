drop table if exists refresh_tokens;

create table refresh_tokens (
    jti uuid not null primary key,
    user_id uuid not null references users (user_id),
    revoked boolean not null default false,
    issued_at bigint not null,
    expires_at bigint not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index idx_refresh_token_jti on refresh_tokens (jti);
