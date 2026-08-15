package com.allnoneplus.backend.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO exposing the schedules persisted in the database.
 */
@Schema(description = "A persisted schedule entry")
public record ScheduleView(
    @Schema(description = "Internal primary key") Long id,
    @Schema(description = "Business identifier as provided by the remote API") String externalId,
    @Schema(description = "Session title") String title,
    @Schema(description = "Start date/time of the session") Instant start,
    @Schema(description = "End date/time of the session") Instant end,
    @Schema(description = "Background color used by the planner (e.g. #808000)") String backgroundColor,
    @Schema(description = "Border color used by the planner (e.g. #808000)") String borderColor,
    @Schema(description = "Text color used by the planner (e.g. #000000)") String textColor,
    @Schema(description = "Session type (course, exam, activity, ...)") String type,
    @Schema(description = "Detailed session type") String sessionType,
    @Schema(description = "Session label") String sessionLabel,
    @Schema(description = "Whether this session is an exam") boolean isExamSession,
    @Schema(description = "Whether this session is an activity") boolean isActivitySession,
    @Schema(description = "Exam identifier, when applicable") Long examId,
    @Schema(description = "Planning activity identifier, when applicable") Long planningActivityId,
    @Schema(description = "Schedule identifier on the source side") Long scheduleId,
    @Schema(description = "Module name") String module,
    @Schema(description = "Module code") String moduleCode,
    @Schema(description = "Module identifier") Long moduleId,
    @Schema(description = "Teacher name") String teacher,
    @Schema(description = "Teacher identifier") Long teacherId,
    @Schema(description = "Room name") String room,
    @Schema(description = "Room identifier") Long roomId,
    @Schema(description = "Level names (raw string)") String levels,
    @Schema(description = "Level codes (raw string)") String levelsCodes,
    @Schema(description = "Day of the week (1 = Monday ... 7 = Sunday)") int dayOfWeek,
    @Schema(description = "Start time of the session") LocalTime startTime,
    @Schema(description = "End time of the session") LocalTime endTime,
    @Schema(description = "Total number of students") int totalStudents,
    @Schema(description = "Room capacity") int roomCapacity,
    @Schema(description = "Whether the room capacity is exceeded") boolean capacityExceeded,
    @Schema(description = "Monday of the week this entry belongs to") LocalDate weekStart,
    @Schema(description = "Date/time of the last synchronization") Instant fetchedAt,
    @Schema(description = "Schedule identifiers (from extendedProps)") List<Long> scheduleIds,
    @Schema(description = "Room names (from extendedProps)") List<String> rooms,
    @Schema(description = "Room identifiers (from extendedProps)") List<Long> roomIds,
    @Schema(description = "Level identifiers (from extendedProps)") List<Long> levelIds) {

  public static ScheduleView from(Schedule s) {
    return new ScheduleView(
        s.getId(),
        s.getExternalId(),
        s.getTitle(),
        s.getStart(),
        s.getEnd(),
        s.getBackgroundColor(),
        s.getBorderColor(),
        s.getTextColor(),
        s.getType(),
        s.getSessionType(),
        s.getSessionLabel(),
        Boolean.TRUE.equals(s.getIsExamSession()),
        Boolean.TRUE.equals(s.getIsActivitySession()),
        s.getExamId(),
        s.getPlanningActivityId(),
        s.getScheduleId(),
        s.getModule(),
        s.getModuleCode(),
        s.getModuleId(),
        s.getTeacher(),
        s.getTeacherId(),
        s.getRoom(),
        s.getRoomId(),
        s.getLevels(),
        s.getLevelsCodes(),
        s.getDayOfWeek() == null ? 0 : s.getDayOfWeek(),
        s.getStartTime(),
        s.getEndTime(),
        s.getTotalStudents() == null ? 0 : s.getTotalStudents(),
        s.getRoomCapacity() == null ? 0 : s.getRoomCapacity(),
        Boolean.TRUE.equals(s.getCapacityExceeded()),
        s.getWeekStart(),
        s.getFetchedAt(),
        copy(s.getScheduleIds()),
        copy(s.getRooms()),
        copy(s.getRoomIds()),
        copy(s.getLevelIds()));
  }

  /** Returns a defensive copy of the given list, or an empty list when null. */
  private static <T> List<T> copy(List<T> list) {
    return list == null ? List.of() : new ArrayList<>(list);
  }
}
