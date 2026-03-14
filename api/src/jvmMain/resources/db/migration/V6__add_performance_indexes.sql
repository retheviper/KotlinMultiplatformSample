CREATE INDEX idx_mention_notifications_message
    ON mention_notifications(message_id);

CREATE INDEX idx_mention_notifications_thread_root
    ON mention_notifications(thread_root_message_id);

CREATE INDEX idx_mention_notifications_member_id
    ON mention_notifications(member_id, id);
