package com.jordansimsmith.immersion.tracker;

import static com.jordansimsmith.immersion.tracker.jooq.Tables.EPISODE;

import com.jordansimsmith.immersion.tracker.jooq.tables.records.EpisodeRecord;
import java.util.List;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImmersionTrackerController {

    private final DSLContext create;

    @Autowired
    public ImmersionTrackerController(DSLContext create) {
        this.create = create;
    }

    @GetMapping("/progress")
    public List<EpisodeRecord> progress() {
        return create.selectFrom(EPISODE).fetch();
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
