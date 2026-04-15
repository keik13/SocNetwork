CREATE TABLE friendship (
  user_id uuid NOT NULL REFERENCES soc_user(id) ON DELETE CASCADE,
  friend_id uuid NOT NULL REFERENCES soc_user(id) ON DELETE CASCADE,
  CONSTRAINT friendship_pk PRIMARY KEY (user_id, friend_id),
  CONSTRAINT no_self_friend CHECK (user_id != friend_id)
);

-- для поиска фоловеров
CREATE INDEX idx_friendship_friend_id ON friendship(friend_id);
