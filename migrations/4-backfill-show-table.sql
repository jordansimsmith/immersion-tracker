INSERT INTO show (folder_name)
SELECT folder_name
FROM episode
GROUP BY folder_name;

UPDATE episode
SET show_id = (SELECT id
               FROM show
               WHERE show.folder_name = episode.folder_name
               LIMIT 1);
