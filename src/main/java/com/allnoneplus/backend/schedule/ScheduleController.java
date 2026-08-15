package com.allnoneplus.backend.schedule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

/**
 * Exposes the schedule synchronization and lookup operations.
 */
@Tag(name = "Schedules", description = "Synchronization and lookup of student schedules")
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

  private final ScheduleService scheduleService;

  public ScheduleController(ScheduleService scheduleService) {
    this.scheduleService = scheduleService;
  }

  /**
   * Triggers the synchronization of schedules for a week.
   * Without parameters, the current week (monday → sunday) is used.
   */
  @Operation(summary = "Trigger a schedule synchronization", description = "Fetches schedules from the remote planning API for the given week and persists "
      + "them. Without parameters, the current week (monday → sunday) is used.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Synchronization completed"),
      @ApiResponse(responseCode = "400", description = "Invalid parameters (e.g. start after end)", content = @Content(schema = @Schema(implementation = String.class))),
      @ApiResponse(responseCode = "502", description = "Remote planning API unreachable or not returning JSON")
  })
  @PostMapping("/sync")
  public SyncResult sync(
      @Parameter(description = "First day of the week (ISO date, e.g. 2026-05-18). "
          + "Defaults to the current week's monday.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @Parameter(description = "Last day of the week (ISO date, e.g. 2026-05-24). "
          + "Defaults to the current week's sunday.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    LocalDate weekStart = start;
    LocalDate weekEnd = end;
    if (weekStart == null && weekEnd == null) {
      LocalDate[] week = scheduleService.currentWeek();
      weekStart = week[0];
      weekEnd = week[1];
    } else if (weekStart == null) {
      weekStart = weekEnd.minusDays(6);
    } else if (weekEnd == null) {
      weekEnd = weekStart.plusDays(6);
    }
    if (weekStart.isAfter(weekEnd)) {
      throw new IllegalArgumentException("The 'start' parameter must be before or equal to 'end'.");
    }
    return scheduleService.syncWeek(weekStart, weekEnd);
  }

  /** Lists the persisted schedules, optionally filtered by week. */
  @Operation(summary = "List persisted schedules", description = "Returns all persisted schedules, optionally filtered by week start.")
  @GetMapping
  public List<ScheduleView> getSchedules(
      @Parameter(description = "Monday of the week to filter on (ISO date, e.g. 2026-05-18). "
          + "When omitted, all schedules are returned.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
    return weekStart == null ? scheduleService.findAll() : scheduleService.findByWeekStart(weekStart);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleIllegalArgument(IllegalArgumentException ex) {
    return ex.getMessage();
  }

  @ExceptionHandler(RestClientException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public String handleRemoteError(RestClientException ex) {
    return "Unable to reach the planning API: " + ex.getMessage();
  }

  @ExceptionHandler(org.springframework.web.client.UnknownContentTypeException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public String handleUnknownContentType(org.springframework.web.client.UnknownContentTypeException ex) {
    return "The planning API did not return JSON (content: " + ex.getContentType()
        + "). Check the authentication or the configured URL.";
  }
}
