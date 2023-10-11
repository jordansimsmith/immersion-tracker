package com.jordansimsmith.immersion.tracker;

import static com.jordansimsmith.immersion.tracker.jooq.Tables.EPISODE;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImmersionTrackerController {
    public record ProgressMessage(
            @JsonProperty("total_episodes_watched") int totalEpisodesWatched,
            @JsonProperty("total_hours_watched") int totalHoursWatched,
            @JsonProperty("episodes_per_show_watched")
                    Map<String, Integer> episodesPerShowWatched) {}

    public record SyncMessage(
            @JsonProperty("folder_name") String folderName,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("timestamp") LocalDateTime timestamp) {}

    private static final int MINUTES_PER_EPISODE = 20;

    private final DSLContext create;

    @Autowired
    public ImmersionTrackerController(DSLContext create) {
        this.create = create;
    }

    @GetMapping("/progress")
    public ProgressMessage progress() {
        var showsWatched =
                create.select(EPISODE.FOLDER_NAME, DSL.count().as("episodes_watched"))
                        .from(EPISODE)
                        .groupBy(EPISODE.FOLDER_NAME)
                        .fetch();

        int totalEpisodesWatched =
                showsWatched.stream().map(Record2::value2).reduce(0, Integer::sum);
        int totalHoursWatched = totalEpisodesWatched * MINUTES_PER_EPISODE / 60;
        var episodesPerShowWatched =
                showsWatched.stream().collect(Collectors.toMap(Record2::value1, Record2::value2));

        return new ProgressMessage(totalEpisodesWatched, totalHoursWatched, episodesPerShowWatched);
    }

    @PostMapping("/sync")
    public void sync(@RequestBody List<SyncMessage> syncMessages) {
        create.transaction(
                (Configuration txn) -> {
                    for (var syncMessage : syncMessages) {
                        var episode =
                                txn.dsl()
                                        .selectFrom(EPISODE)
                                        .where(EPISODE.FOLDER_NAME.eq(syncMessage.folderName()))
                                        .and(EPISODE.FILE_NAME.eq(syncMessage.fileName()))
                                        .fetchAny();
                        if (episode == null) {
                            episode = txn.dsl().newRecord(EPISODE);
                            episode.setFolderName(syncMessage.folderName());
                            episode.setFileName(syncMessage.fileName());
                            episode.setTimestamp(syncMessage.timestamp());
                            episode.insert();
                        }
                    }
                });
    }
}
