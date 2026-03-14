CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    owner_member_id UUID NOT NULL,
    created_at_epoch_millis BIGINT NOT NULL
);

CREATE TABLE workspace_members (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id VARCHAR(120) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at_epoch_millis BIGINT NOT NULL,
    CONSTRAINT uq_workspace_member UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_members_workspace ON workspace_members(workspace_id);

CREATE TABLE channels (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(120) NOT NULL,
    topic TEXT NULL,
    visibility VARCHAR(20) NOT NULL,
    created_by_member_id UUID NOT NULL REFERENCES workspace_members(id),
    created_at_epoch_millis BIGINT NOT NULL,
    CONSTRAINT uq_channel_slug UNIQUE (workspace_id, slug)
);

CREATE INDEX idx_channels_workspace ON channels(workspace_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    author_member_id UUID NOT NULL REFERENCES workspace_members(id),
    body TEXT NOT NULL,
    thread_root_message_id UUID NULL REFERENCES messages(id) ON DELETE CASCADE,
    created_at_epoch_millis BIGINT NOT NULL
);

CREATE INDEX idx_messages_channel_root_created
    ON messages(channel_id, thread_root_message_id, created_at_epoch_millis DESC);

CREATE INDEX idx_messages_thread_root_created
    ON messages(thread_root_message_id, created_at_epoch_millis ASC);
