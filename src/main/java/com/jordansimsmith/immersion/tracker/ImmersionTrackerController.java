package com.jordansimsmith.immersion.tracker;

import static com.jordansimsmith.immersion.tracker.jooq.Tables.EPISODE;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
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
        var episodesPerShowWatched = new LinkedHashMap<String, Integer>();
        showsWatched.stream()
                .sorted((a, b) -> b.value2() - a.value2())
                .forEach(s -> episodesPerShowWatched.put(s.value1(), s.value2()));

        return new ProgressMessage(totalEpisodesWatched, totalHoursWatched, episodesPerShowWatched);
    }

    @GetMapping(value = "/chart", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] chart() throws IOException {
        var year = DSL.extract(EPISODE.TIMESTAMP, DatePart.YEAR).as("year");
        var month = DSL.extract(EPISODE.TIMESTAMP, DatePart.MONTH).as("month");
        var episodesPerMonth =
                create.select(year, month, DSL.count().as("episodes_watched"))
                        .from(EPISODE)
                        .groupBy(year, month)
                        .orderBy(year.asc(), month.asc())
                        .fetch();

        var dataset = new DefaultCategoryDataset();
        // immersion start date
        dataset.addValue(0, "episodes", LocalDate.of(2023, 5, 1));
        var sum = 0;
        for (var episodes : episodesPerMonth) {
            sum += episodes.value3();
            var date = LocalDate.of(episodes.value1(), episodes.value2(), 1);
            dataset.addValue(sum, "episodes", date);
        }

        var chart =
                ChartFactory.createLineChart(
                        "accumulation of episodes watched",
                        "month",
                        "episodes watched",
                        dataset,
                        PlotOrientation.VERTICAL,
                        false,
                        false,
                        false);
        var plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setOutlineVisible(false);

        try (var outputStream = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(outputStream, chart, 800, 600);
            return outputStream.toByteArray();
        }
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
