package com.allnoneplus.backend.schedule;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a planning slot (schedule entry) persisted in the database.
 * A given slot is identified by (scheduleId, weekStart), which makes it
 * possible to compare data between two synchronizations of the same week.
 */
@Entity
@Table(name = "schedules", uniqueConstraints = @UniqueConstraint(name = "uq_schedule_week", columnNames = {
    "schedule_id", "week_start" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "external_id")
  private String externalId;

  @Column(name = "title")
  private String title;

  @Column(name = "start_time")
  private Instant start;

  @Column(name = "end_time")
  private Instant end;

  @Column(name = "background_color")
  private String backgroundColor;

  @Column(name = "border_color")
  private String borderColor;

  @Column(name = "text_color")
  private String textColor;

  // --- extendedProps ---

  @Column(name = "type")
  private String type;

  @Column(name = "session_type")
  private String sessionType;

  @Column(name = "session_label")
  private String sessionLabel;

  @Column(name = "is_exam_session")
  private Boolean isExamSession;

  @Column(name = "is_activity_session")
  private Boolean isActivitySession;

  @Column(name = "exam_id")
  private Long examId;

  @Column(name = "planning_activity_id")
  private Long planningActivityId;

  /** Business identifier of the slot on the source side. */
  @Column(name = "schedule_id")
  private Long scheduleId;

  @Column(name = "module")
  private String module;

  @Column(name = "module_code")
  private String moduleCode;

  @Column(name = "module_id")
  private Long moduleId;

  @Column(name = "teacher")
  private String teacher;

  @Column(name = "teacher_id")
  private Long teacherId;

  @Column(name = "room")
  private String room;

  @Column(name = "room_id")
  private Long roomId;

  @Column(name = "levels")
  private String levels;

  @Column(name = "levels_codes")
  private String levelsCodes;

  @Column(name = "day_of_week")
  private Integer dayOfWeek;

  @Column(name = "start_time_of_day")
  private LocalTime startTime;

  @Column(name = "end_time_of_day")
  private LocalTime endTime;

  @Column(name = "total_students")
  private Integer totalStudents;

  @Column(name = "room_capacity")
  private Integer roomCapacity;

  @Column(name = "capacity_exceeded")
  private Boolean capacityExceeded;

  // --- synchronization context ---

  /** Monday of the week this slot belongs to. */
  @Column(name = "week_start")
  private LocalDate weekStart;

  /** Date/time of the last synchronization. */
  @Column(name = "fetched_at")
  private Instant fetchedAt;

  // --- extendedProps collections ---

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "schedule_schedule_ids", joinColumns = @JoinColumn(name = "schedule_ref_id"))
  @Column(name = "schedule_id_value")
  @Builder.Default
  private List<Long> scheduleIds = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "schedule_rooms", joinColumns = @JoinColumn(name = "schedule_ref_id"))
  @Column(name = "room")
  @Builder.Default
  private List<String> rooms = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "schedule_room_ids", joinColumns = @JoinColumn(name = "schedule_ref_id"))
  @Column(name = "room_id")
  @Builder.Default
  private List<Long> roomIds = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "schedule_level_ids", joinColumns = @JoinColumn(name = "schedule_ref_id"))
  @Column(name = "level_id")
  @Builder.Default
  private List<Long> levelIds = new ArrayList<>();
}
