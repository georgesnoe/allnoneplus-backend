package com.allnoneplus.backend.schedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Fetches slots from the remote planning API and persists them in the
 * database. Comparing existing data with fresh data makes it possible to
 * detect creations, updates and deletions.
 */
@Service
public class ScheduleService {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private final ScheduleRepository scheduleRepository;
  private final RestClient restClient;

  public ScheduleService(ScheduleRepository scheduleRepository, RestClient planningRestClient) {
    this.scheduleRepository = scheduleRepository;
    this.restClient = planningRestClient;
  }

  /** Returns [monday, sunday] of the current week. */
  public LocalDate[] currentWeek() {
    LocalDate today = LocalDate.now();
    LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return new LocalDate[] { monday, monday.plusDays(6) };
  }

  /**
   * Synchronizes the week [weekStart, weekEnd]: fetches the remote data,
   * compares it with the database and applies the differences.
   */
  @Transactional
  public SyncResult syncWeek(LocalDate weekStart, LocalDate weekEnd) {
    List<ScheduleDto> fetched = fetch(weekStart, weekEnd);

    List<Schedule> existing = scheduleRepository.findByWeekStart(weekStart);
    Map<Long, Schedule> existingById = new HashMap<>();
    for (Schedule s : existing) {
      existingById.put(s.getScheduleId(), s);
    }

    Set<Long> fetchedIds = new HashSet<>();
    int created = 0;
    int updated = 0;
    int unchanged = 0;

    for (ScheduleDto dto : fetched) {
      if (dto.extendedProps() == null || dto.extendedProps().scheduleId() == null) {
        continue; // unidentifiable slot, skip it
      }
      Long scheduleId = dto.extendedProps().scheduleId();
      fetchedIds.add(scheduleId);

      Schedule mapped = toEntity(dto, weekStart);
      Schedule current = existingById.get(scheduleId);
      if (current == null) {
        scheduleRepository.save(mapped);
        created++;
      } else {
        mapped.setId(current.getId());
        if (sameContent(current, mapped)) {
          // No content change: only refresh fetchedAt
          mapped.setFetchedAt(current.getFetchedAt());
          scheduleRepository.save(mapped);
          unchanged++;
        } else {
          scheduleRepository.save(mapped);
          updated++;
        }
      }
    }

    // Delete slots that no longer exist on the source side for this week
    int deleted = 0;
    for (Schedule s : existing) {
      if (!fetchedIds.contains(s.getScheduleId())) {
        scheduleRepository.delete(s);
        deleted++;
      }
    }

    return new SyncResult(weekStart, weekEnd, fetched.size(), created, updated, unchanged, deleted);
  }

  @Transactional(readOnly = true)
  public List<ScheduleView> findAll() {
    return scheduleRepository.findAll().stream().map(ScheduleView::from).toList();
  }

  @Transactional(readOnly = true)
  public List<ScheduleView> findByWeekStart(LocalDate weekStart) {
    return scheduleRepository.findByWeekStart(weekStart).stream().map(ScheduleView::from).toList();
  }

  private List<ScheduleDto> fetch(LocalDate start, LocalDate end) {
    ScheduleDto[] schedules = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .queryParam("start", start.format(ISO_DATE))
            .queryParam("end", end.format(ISO_DATE))
            .build())
        .retrieve()
        .body(ScheduleDto[].class);
    return schedules == null ? List.of() : List.of(schedules);
  }

  private Schedule toEntity(ScheduleDto dto, LocalDate weekStart) {
    ScheduleDto.ExtendedProps ep = dto.extendedProps();
    return Schedule.builder()
        .externalId(dto.id())
        .title(dto.title())
        .start(dto.start())
        .end(dto.end())
        .backgroundColor(dto.backgroundColor())
        .borderColor(dto.borderColor())
        .textColor(dto.textColor())
        .type(ep.type())
        .sessionType(ep.sessionType())
        .sessionLabel(ep.sessionLabel())
        .isExamSession(ep.isExamSession())
        .isActivitySession(ep.isActivitySession())
        .examId(ep.examId())
        .planningActivityId(ep.planningActivityId())
        .scheduleId(ep.scheduleId())
        .module(ep.module())
        .moduleCode(ep.moduleCode())
        .moduleId(ep.moduleId())
        .teacher(ep.teacher())
        .teacherId(ep.teacherId())
        .room(ep.room())
        .roomId(ep.roomId())
        .levels(ep.levels())
        .levelsCodes(ep.levelsCodes())
        .dayOfWeek(ep.dayOfWeek())
        .startTime(parseLocalTime(ep.startTime()))
        .endTime(parseLocalTime(ep.endTime()))
        .totalStudents(ep.totalStudents())
        .roomCapacity(ep.roomCapacity())
        .capacityExceeded(ep.capacityExceeded())
        .weekStart(weekStart)
        .fetchedAt(Instant.now())
        .scheduleIds(new ArrayList<>(ep.scheduleIds() == null ? List.of() : ep.scheduleIds()))
        .rooms(new ArrayList<>(ep.rooms() == null ? List.of() : ep.rooms()))
        .roomIds(new ArrayList<>(ep.roomIds() == null ? List.of() : ep.roomIds()))
        .levelIds(new ArrayList<>(ep.levelIds() == null ? List.of() : ep.levelIds()))
        .build();
  }

  private LocalTime parseLocalTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return LocalTime.parse(value);
  }

  /**
   * Compares the business content of two schedules (ignoring id, weekStart,
   * fetchedAt).
   * List comparisons are content-based because Hibernate wraps stored collections
   * in
   * PersistentBag instances whose equals() only matches other Hibernate
   * collections.
   */
  private boolean sameContent(Schedule a, Schedule b) {
    return Objects.equals(a.getExternalId(), b.getExternalId())
        && Objects.equals(a.getTitle(), b.getTitle())
        && Objects.equals(a.getStart(), b.getStart())
        && Objects.equals(a.getEnd(), b.getEnd())
        && Objects.equals(a.getBackgroundColor(), b.getBackgroundColor())
        && Objects.equals(a.getBorderColor(), b.getBorderColor())
        && Objects.equals(a.getTextColor(), b.getTextColor())
        && Objects.equals(a.getType(), b.getType())
        && Objects.equals(a.getSessionType(), b.getSessionType())
        && Objects.equals(a.getSessionLabel(), b.getSessionLabel())
        && Objects.equals(a.getIsExamSession(), b.getIsExamSession())
        && Objects.equals(a.getIsActivitySession(), b.getIsActivitySession())
        && Objects.equals(a.getExamId(), b.getExamId())
        && Objects.equals(a.getPlanningActivityId(), b.getPlanningActivityId())
        && Objects.equals(a.getModule(), b.getModule())
        && Objects.equals(a.getModuleCode(), b.getModuleCode())
        && Objects.equals(a.getModuleId(), b.getModuleId())
        && Objects.equals(a.getTeacher(), b.getTeacher())
        && Objects.equals(a.getTeacherId(), b.getTeacherId())
        && Objects.equals(a.getRoom(), b.getRoom())
        && Objects.equals(a.getRoomId(), b.getRoomId())
        && Objects.equals(a.getLevels(), b.getLevels())
        && Objects.equals(a.getLevelsCodes(), b.getLevelsCodes())
        && Objects.equals(a.getDayOfWeek(), b.getDayOfWeek())
        && Objects.equals(a.getStartTime(), b.getStartTime())
        && Objects.equals(a.getEndTime(), b.getEndTime())
        && Objects.equals(a.getTotalStudents(), b.getTotalStudents())
        && Objects.equals(a.getRoomCapacity(), b.getRoomCapacity())
        && Objects.equals(a.getCapacityExceeded(), b.getCapacityExceeded())
        && sameElements(a.getScheduleIds(), b.getScheduleIds())
        && sameElements(a.getRooms(), b.getRooms())
        && sameElements(a.getRoomIds(), b.getRoomIds())
        && sameElements(a.getLevelIds(), b.getLevelIds());
  }

  /**
   * Content-based list equality (order-sensitive), safe across collection
   * implementations (ArrayList vs Hibernate PersistentBag).
   */
  private static boolean sameElements(List<?> a, List<?> b) {
    if (a == null || b == null) {
      return a == b;
    }
    return new ArrayList<>(a).equals(new ArrayList<>(b));
  }
}
