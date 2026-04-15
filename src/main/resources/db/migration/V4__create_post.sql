CREATE TABLE post (
    id uuid primary key,
    user_id uuid NOT NULL REFERENCES soc_user(id) ON DELETE CASCADE,
    message text not null,
    created_at bigint not null,
    updated_at bigint not null
);

CREATE INDEX idx_post_user_id ON post(user_id);
CREATE INDEX idx_post_updated_at ON post(updated_at);
