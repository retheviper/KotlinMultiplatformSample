CREATE TABLE message_reactions (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    member_id UUID NOT NULL REFERENCES workspace_members (id) ON DELETE CASCADE,
    emoji VARCHAR(32) NOT NULL,
    created_at_epoch_millis BIGINT NOT NULL
);

CREATE UNIQUE INDEX message_reactions_message_member_emoji_idx
    ON message_reactions (message_id, member_id, emoji);

CREATE INDEX message_reactions_message_idx
    ON message_reactions (message_id, created_at_epoch_millis);
