CREATE TABLE mention_notifications (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    thread_root_message_id UUID NULL REFERENCES messages(id) ON DELETE CASCADE,
    author_display_name VARCHAR(120) NOT NULL,
    message_preview TEXT NOT NULL,
    created_at_epoch_millis BIGINT NOT NULL,
    read_at_epoch_millis BIGINT NULL
);

CREATE INDEX idx_mention_notifications_member_created
    ON mention_notifications(member_id, created_at_epoch_millis DESC);

CREATE INDEX idx_mention_notifications_member_read_created
    ON mention_notifications(member_id, read_at_epoch_millis, created_at_epoch_millis DESC);
