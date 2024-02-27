package com.jordansimsmith.immersion.tracker;

import static com.jordansimsmith.immersion.tracker.jooq.Tables.EPISODE;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.DatePart;
import org.jooq.Record2;
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
            @JsonProperty("episodes_per_show_watched")
                    Map<String, Integer> episodesPerShowWatched) {}

    public record SyncRequest(
            @JsonProperty("folder_name") String folderName,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("timestamp") LocalDateTime timestamp) {}

    public record SyncResponse(@JsonProperty("episodes_added") int episodesAdded) {}

    private static final int MINUTES_PER_EPISODE = 20;

    private final DSLContext create;

    @Autowired
    public ImmersionTrackerController(DSLContext create) {
        this.create = create;
    }

    @GetMapping("/progress")
    public ProgressResponse progress() {
        var showsWatched =
                create.select(EPISODE.FOLDER_NAME, DSL.count().as("episodes_watched"))
                        .from(EPISODE)
                        .groupBy(EPISODE.FOLDER_NAME)
                        .fetch();

        int totalEpisodesWatched =
                showsWatched.stream().map(Record2::value2).reduce(0, Integer::sum);
        int totalHoursWatched = totalEpisodesWatched * MINUTES_PER_EPISODE / 60;
        var episodesPerShowWatched = new LinkedHashMap<String, Integer>();
        showsWatched.stream()
                .sorted((a, b) -> b.value2() - a.value2())
                .forEach(s -> episodesPerShowWatched.put(s.value1(), s.value2()));

        return new ProgressResponse(
                totalEpisodesWatched, totalHoursWatched, episodesPerShowWatched);
    }

    @GetMapping(value = "/chart", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] chart(HttpServletResponse res) throws IOException {
        var year = DSL.extract(EPISODE.TIMESTAMP, DatePart.YEAR).as("year");
        var month = DSL.extract(EPISODE.TIMESTAMP, DatePart.MONTH).as("month");
        var day = DSL.extract(EPISODE.TIMESTAMP, DatePart.DAY).as("day");
        var episodesPerMonth =
                create.select(year, month, day, DSL.count().as("episodes_watched"))
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
        var episodes =
                create.selectFrom(EPISODE)
                        .orderBy(EPISODE.TIMESTAMP.asc(), EPISODE.ID.asc())
                        .fetch();
        var builder = new StringBuilder();
        builder.append("id,file_name,folder_name,timestamp");
        builder.append("\n");

        for (var episode : episodes) {
            var row =
                    episode.getId()
                            + ","
                            + episode.getFileName()
                            + ","
                            + episode.getFolderName()
                            + ","
                            + episode.getTimestamp();
            builder.append(row);
            builder.append("\n");
        }

        return builder.toString();
    }

    @PostMapping("/sync")
    public SyncResponse sync(@RequestBody List<SyncRequest> syncRequests) {
        var episodesAdded = new AtomicInteger();
        create.transaction(
                (Configuration txn) -> {
                    for (var syncMessage : syncRequests) {
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
                            episodesAdded.getAndIncrement();
                        }
                    }
                });

        return new SyncResponse(episodesAdded.get());
    }
}
