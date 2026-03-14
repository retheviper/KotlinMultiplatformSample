ALTER TABLE messages
    ADD COLUMN preview_url TEXT NULL,
    ADD COLUMN preview_title TEXT NULL,
    ADD COLUMN preview_description TEXT NULL,
    ADD COLUMN preview_image_url TEXT NULL,
    ADD COLUMN preview_site_name VARCHAR(255) NULL;
