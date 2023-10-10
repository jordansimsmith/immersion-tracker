CREATE TABLE episode
(
    id          SERIAL PRIMARY KEY,
    file_name   TEXT      NOT NULL,
    folder_name TEXT      NOT NULL,
    timestamp   TIMESTAMP NOT NULL
)