/*
    TABLE: weekly_availability_slots
    CHANGES: Adding primary key and audit fields to weekly_availability_slots
 */

ALTER TABLE weekly_availability_slots
    ADD PRIMARY KEY (id),
    ADD CONSTRAINT fk_weekly_availability_slots_users FOREIGN KEY (user_id) REFERENCES users(user_id),
    ADD created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    ADD updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL;

