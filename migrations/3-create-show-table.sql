CREATE TABLE show
(
    id          SERIAL PRIMARY KEY,
    folder_name TEXT    NOT NULL,
    tvdb_id     INTEGER NULL,
    tvdb_name   TEXT    NULL,
    tvdb_image  TEXT    NULL
);

ALTER TABLE episode
    ADD COLUMN show_id INTEGER REFERENCES show (id);