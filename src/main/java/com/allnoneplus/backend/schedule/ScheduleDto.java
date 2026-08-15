package com.allnoneplus.backend.schedule;

import java.time.Instant;
import java.util.List;

/**
 * DTO for deserializing the JSON response from the remote API.
 */
public record ScheduleDto(
    String id,
    String title,
    Instant start,
    Instant end,
    String backgroundColor,
    String borderColor,
    String textColor,
    ExtendedProps extendedProps) {

  public record ExtendedProps(
      String type,
      String sessionType,
      String sessionLabel,
      boolean isExamSession,
      boolean isActivitySession,
      Long examId,
      Long planningActivityId,
      Long scheduleId,
      List<Long> scheduleIds,
      String module,
      String moduleCode,
      Long moduleId,
      String teacher,
      Long teacherId,
      String room,
      List<String> rooms,
      List<Long> roomIds,
      Long roomId,
      String levels,
      String levelsCodes,
      List<Long> levelIds,
      int dayOfWeek,
      String startTime,
      String endTime,
      int totalStudents,
      int roomCapacity,
      boolean capacityExceeded) {
  }
}
