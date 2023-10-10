package com.jordansimsmith.immersion.tracker;

import com.jordansimsmith.immersion.tracker.jooq.tables.records.EpisodeRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.jordansimsmith.immersion.tracker.jooq.Tables.EPISODE;

@RestController
public class ImmersionTrackerController {

    private final DSLContext dslContext;

    @Autowired
    public ImmersionTrackerController(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @GetMapping("/")
    public List<EpisodeRecord> index() {
        return dslContext.selectFrom(EPISODE).fetch();
    }
}
