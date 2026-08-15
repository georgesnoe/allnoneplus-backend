package com.allnoneplus.backend.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

  Optional<Schedule> findByScheduleIdAndWeekStart(Long scheduleId, LocalDate weekStart);

  List<Schedule> findByWeekStart(LocalDate weekStart);
}
