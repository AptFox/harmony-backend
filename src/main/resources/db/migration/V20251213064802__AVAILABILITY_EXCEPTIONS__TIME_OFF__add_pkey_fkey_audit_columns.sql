ALTER TABLE availability_exceptions
    ADD PRIMARY KEY (id),
    ADD CONSTRAINT fk_user_time_off FOREIGN KEY (user_id) REFERENCES users(user_id),
    ADD created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    ADD updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL;

ALTER TABLE availability_exceptions
    RENAME TO time_off;