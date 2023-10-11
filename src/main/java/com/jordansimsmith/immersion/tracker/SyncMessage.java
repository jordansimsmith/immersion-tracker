package com.jordansimsmith.immersion.tracker;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record SyncMessage(
        @JsonProperty("folder_name") String folderName,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("timestamp") LocalDateTime timestamp) {}
