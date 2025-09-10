CREATE INDEX idx_refresh_tokens_user_id_created_at
    ON refresh_tokens(user_id, created_at ASC);
