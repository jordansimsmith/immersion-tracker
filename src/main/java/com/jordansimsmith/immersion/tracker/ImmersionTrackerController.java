package com.jordansimsmith.immersion.tracker;

import static com.jordansimsmith.immersion.tracker.jooq.Tables.EPISODE;
import static com.jordansimsmith.immersion.tracker.jooq.Tables.SHOW;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
public class ImmersionTrackerController {
    public record ListShowsResponse(
            @JsonProperty("total_episodes_watched") int totalEpisodesWatched,
            @JsonProperty("total_hours_watched") int totalHoursWatched,
            @JsonProperty("shows") List<Show> shows) {}

    public record Show(
            @JsonProperty("id") int id,
            @JsonProperty("episodes_watched") int episodesWatched,
            @JsonProperty("folder_name") String folderName,
            @Nullable @JsonProperty("tvdb_id") Integer tvdbId,
            @Nullable @JsonProperty("tvdb_name") String tvdbName,
            @Nullable @JsonProperty("tvdb_image") String tvdbImage) {}

    public record UpdateShowRequest(@JsonProperty("tvdb_id") int tvdbId) {}

    public record SyncEpisodesRequest(
            @JsonProperty("folder_name") String folderName,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("timestamp") LocalDateTime timestamp) {}

    public record SyncEpisodesResponse(@JsonProperty("episodes_added") int episodesAdded) {}

    private static final int MINUTES_PER_EPISODE = 20;

    private final String tvdbApiKey;
    private final DSLContext ctx;

    @Autowired
    public ImmersionTrackerController(@Value("${tvdb.api.key}") String tvdbApiKey, DSLContext ctx) {
        this.tvdbApiKey = Objects.requireNonNull(tvdbApiKey);
        this.ctx = ctx;
    }

    @GetMapping("/shows")
    public ListShowsResponse listShows() {
        var records =
                ctx.select(
                                SHOW.ID,
                                DSL.count(),
                                SHOW.FOLDER_NAME,
                                SHOW.TVDB_ID,
                                SHOW.TVDB_NAME,
                                SHOW.TVDB_IMAGE)
                        .from(SHOW)
                        .join(EPISODE)
                        .on(SHOW.ID.eq(EPISODE.SHOW_ID))
                        .groupBy(
                                SHOW.ID,
                                SHOW.FOLDER_NAME,
                                SHOW.TVDB_ID,
                                SHOW.TVDB_NAME,
                                SHOW.TVDB_IMAGE)
                        .orderBy(DSL.count().desc())
                        .fetch();

        var totalEpisodesWatched = 0;
        for (var record : records) {
            totalEpisodesWatched += record.value2();
        }
        int totalHoursWatched = totalEpisodesWatched * MINUTES_PER_EPISODE / 60;

        var showsWatched = new ArrayList<Show>();
        for (var record : records) {
            var show =
                    new Show(
                            record.value1(),
                            record.value2(),
                            record.value3(),
                            record.value4(),
                            record.value5(),
                            record.value6());
            showsWatched.add(show);
        }

        return new ListShowsResponse(totalEpisodesWatched, totalHoursWatched, showsWatched);
    }

    @PutMapping("/shows/{id}")
    public void updateShow(@PathVariable(value = "id") int id, @RequestBody UpdateShowRequest req) {
        var client =
                WebClient.builder()
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .baseUrl("https://api4.thetvdb.com/v4")
                        .build();

        // authenticate and get a bearer token
        var login =
                client.post()
                        .uri("/login")
                        .body(BodyInserters.fromValue(Map.of("apikey", tvdbApiKey)))
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
        Objects.requireNonNull(login);
        if (!"success".equals(login.path("status").asText())) {
            throw new RuntimeException("failed to login to tvdb api");
        }
        var token = login.path("data").path("token").asText();
        Objects.requireNonNull(token);

        // request series information
        var series =
                client.get()
                        .uri("/series/" + req.tvdbId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
        Objects.requireNonNull(series);
        if (!"success".equals(series.path("status").asText())) {
            throw new RuntimeException("failed to retrieve series from tvdb api");
        }
        var name = series.path("data").path("name").asText(null);
        var image = series.path("data").path("image").asText(null);

        // update the show record
        ctx.update(SHOW)
                .set(SHOW.TVDB_ID, req.tvdbId())
                .set(SHOW.TVDB_NAME, name)
                .set(SHOW.TVDB_IMAGE, image)
                .where(SHOW.ID.eq(id))
                .limit(1)
                .execute();
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
    public SyncEpisodesResponse syncEpisodes(@RequestBody List<SyncEpisodesRequest> req) {
        var episodesAdded = new AtomicInteger();
        ctx.transaction(
                (Configuration txn) -> {
                    for (var episodeMessage : req) {
                        // check if the show already exists
                        var show =
                                txn.dsl()
                                        .selectFrom(SHOW)
                                        .where(SHOW.FOLDER_NAME.eq(episodeMessage.folderName()))
                                        .fetchAny();

                        // create the show if it doesn't exist
                        if (show == null) {
                            show = txn.dsl().newRecord(SHOW);
                            show.setFolderName(episodeMessage.folderName());
                            show.insert();
                        }

                        // check if the episode exists already
                        var episode =
                                txn.dsl()
                                        .selectFrom(EPISODE)
                                        .where(EPISODE.SHOW_ID.eq(show.getId()))
                                        .and(EPISODE.FILE_NAME.eq(episodeMessage.fileName()))
                                        .fetchAny();

                        // create the episode if it doesn't exist
                        if (episode == null) {
                            episode = txn.dsl().newRecord(EPISODE);
                            episode.setShowId(show.getId());
                            episode.setFileName(episodeMessage.fileName());
                            episode.setTimestamp(episodeMessage.timestamp());
                            episode.insert();
                            episodesAdded.getAndIncrement();
                        }
                    }
                });

        return new SyncEpisodesResponse(episodesAdded.get());
    }
}
