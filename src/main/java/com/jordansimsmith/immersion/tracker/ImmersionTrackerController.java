package com.jordansimsmith.immersion.tracker;

import static com.jordansimsmith.immersion.tracker.jooq.Tables.EPISODE;
import static com.jordansimsmith.immersion.tracker.jooq.Tables.SHOW;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.DatePart;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImmersionTrackerController {
    public record ProgressResponse(
            @JsonProperty("total_episodes_watched") int totalEpisodesWatched,
            @JsonProperty("total_hours_watched") int totalHoursWatched,
            @JsonProperty("shows_watched") List<ShowProgress> showsWatched) {}

    public record ShowProgress(
            @JsonProperty("name") String name,
            @JsonProperty("episodes_watched") int episodesWatched) {}

    public record SyncRequest(
            @JsonProperty("folder_name") String folderName,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("timestamp") LocalDateTime timestamp) {}

    public record SyncResponse(@JsonProperty("episodes_added") int episodesAdded) {}

    private static final int MINUTES_PER_EPISODE = 20;

    private final DSLContext ctx;

    @Autowired
    public ImmersionTrackerController(DSLContext ctx) {
        this.ctx = ctx;
    }

    @GetMapping("/progress")
    public ProgressResponse progress() {
        var name = DSL.coalesce(SHOW.TVDB_NAME, SHOW.FOLDER_NAME);
        var count = DSL.count();
        var records =
                ctx.select(name, count)
                        .from(SHOW)
                        .join(EPISODE)
                        .on(SHOW.ID.eq(EPISODE.SHOW_ID))
                        .groupBy(SHOW.TVDB_ID, SHOW.TVDB_NAME, SHOW.FOLDER_NAME)
                        .orderBy(count.desc())
                        .fetch();

        var totalEpisodesWatched = 0;
        for (var record : records) {
            totalEpisodesWatched += record.value2();
        }
        int totalHoursWatched = totalEpisodesWatched * MINUTES_PER_EPISODE / 60;

        var showsWatched = new ArrayList<ShowProgress>();
        for (var record : records) {
            var show = new ShowProgress(record.value1(), record.value2());
            showsWatched.add(show);
        }

        return new ProgressResponse(totalEpisodesWatched, totalHoursWatched, showsWatched);
    }

    @GetMapping(value = "/chart", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] chart(HttpServletResponse res) throws IOException {
        var year = DSL.extract(EPISODE.TIMESTAMP, DatePart.YEAR).as("year");
        var month = DSL.extract(EPISODE.TIMESTAMP, DatePart.MONTH).as("month");
        var day = DSL.extract(EPISODE.TIMESTAMP, DatePart.DAY).as("day");
        var episodesPerMonth =
                ctx.select(year, month, day, DSL.count().as("episodes_watched"))
                        .from(EPISODE)
                        .groupBy(year, month, day)
                        .orderBy(year.asc(), month.asc(), day.asc())
                        .fetch();

        var series = new TimeSeries("episodes");
        series.add(new Day(7, 5, 2023), 0); // immersion start date
        var sum = 0;
        for (var episodes : episodesPerMonth) {
            sum += episodes.value4();
            series.add(new Day(episodes.value3(), episodes.value2(), episodes.value1()), sum);
        }
        var dataset = new TimeSeriesCollection(series);

        var chart =
                ChartFactory.createTimeSeriesChart(
                        "episodes watched over time",
                        "time",
                        "episodes watched",
                        dataset,
                        false,
                        false,
                        false);
        var plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setOutlineVisible(false);

        res.setHeader("Cache-Control", "no-cache");

        try (var outputStream = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(outputStream, chart, 800, 600);
            return outputStream.toByteArray();
        }
    }

    @GetMapping(value = "/csv", produces = "text/csv")
    public String csv() {
        return ctx.select(
                        EPISODE.ID,
                        EPISODE.FILE_NAME,
                        EPISODE.TIMESTAMP,
                        SHOW.ID.as("show_id"),
                        SHOW.FOLDER_NAME,
                        SHOW.TVDB_ID,
                        SHOW.TVDB_NAME,
                        SHOW.TVDB_IMAGE)
                .from(EPISODE)
                .leftJoin(SHOW)
                .on(EPISODE.SHOW_ID.eq(SHOW.ID))
                .orderBy(EPISODE.TIMESTAMP.asc(), EPISODE.ID.asc())
                .fetch()
                .formatCSV();
    }

    @PostMapping("/sync")
    public SyncResponse sync(@RequestBody List<SyncRequest> syncRequests) {
        var episodesAdded = new AtomicInteger();
        ctx.transaction(
                (Configuration txn) -> {
                    for (var syncMessage : syncRequests) {
                        // check if the show already exists
                        var show =
                                txn.dsl()
                                        .selectFrom(SHOW)
                                        .where(SHOW.FOLDER_NAME.eq(syncMessage.folderName()))
                                        .fetchAny();

                        // create the show if it doesn't exist
                        if (show == null) {
                            show = txn.dsl().newRecord(SHOW);
                            show.setFolderName(syncMessage.folderName());
                            show.insert();
                        }

                        // check if the episode exists already
                        var episode =
                                txn.dsl()
                                        .selectFrom(EPISODE)
                                        .where(EPISODE.SHOW_ID.eq(show.getId()))
                                        .and(EPISODE.FILE_NAME.eq(syncMessage.fileName()))
                                        .fetchAny();

                        // create the episode if it doesn't exist
                        if (episode == null) {
                            episode = txn.dsl().newRecord(EPISODE);
                            episode.setShowId(show.getId());
                            episode.setFileName(syncMessage.fileName());
                            episode.setTimestamp(syncMessage.timestamp());
                            episode.insert();
                            episodesAdded.getAndIncrement();
                        }
                    }
                });

        return new SyncResponse(episodesAdded.get());
    }
}
