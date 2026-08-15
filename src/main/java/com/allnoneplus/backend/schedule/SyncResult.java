package com.allnoneplus.backend.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Summary of a synchronization operation: used to compare the database
 * with the freshly fetched data.
 */
@Schema(description = "Result of a schedule synchronization operation")
public record SyncResult(
    @Schema(description = "First day of the synchronized week") LocalDate weekStart,
    @Schema(description = "Last day of the synchronized week") LocalDate weekEnd,
    @Schema(description = "Number of schedules fetched from the remote API") int fetched,
    @Schema(description = "Number of schedules created") int created,
    @Schema(description = "Number of schedules updated") int updated,
    @Schema(description = "Number of schedules that were already up to date") int unchanged,
    @Schema(description = "Number of schedules deleted") int deleted) {
}
