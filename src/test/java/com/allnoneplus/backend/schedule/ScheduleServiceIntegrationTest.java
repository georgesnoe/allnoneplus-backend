package com.allnoneplus.backend.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.allnoneplus.backend.config.PlanningProperties;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Integration test of the pipeline: fetch → parsing → persistence → comparison.
 * The remote API is simulated with MockRestServiceServer (sample data).
 */
@DataJpaTest
@EnableConfigurationProperties(PlanningProperties.class)
@ActiveProfiles("local")
class ScheduleServiceIntegrationTest {

  @Autowired
  private ScheduleRepository scheduleRepository;

  @Autowired
  private PlanningProperties planningProperties;

  private MockRestServiceServer server;
  private ScheduleService scheduleService;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder()
        .baseUrl(planningProperties.baseUrl())
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("X-Requested-With", "XMLHttpRequest");
    server = MockRestServiceServer.bindTo(builder).build();
    scheduleService = new ScheduleService(scheduleRepository, builder.build());
    scheduleRepository.deleteAll();
  }

  @Test
  void syncFetchesParsesAndPersists() {
    server.expect(requestTo(planningProperties.baseUrl() + "?start=2026-05-18&end=2026-05-24"))
        .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

    SyncResult result = scheduleService.syncWeek(LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24));

    assertThat(result.fetched()).isEqualTo(1);
    assertThat(result.created()).isEqualTo(1);
    assertThat(result.updated()).isZero();
    assertThat(result.deleted()).isZero();

    List<Schedule> stored = scheduleRepository.findByWeekStart(LocalDate.of(2026, 5, 18));
    assertThat(stored).hasSize(1);
    Schedule s = stored.get(0);
    assertThat(s.getScheduleId()).isEqualTo(3611);
    assertThat(s.getExternalId()).isEqualTo("schedule_3611_2026-05-18");
    assertThat(s.getModuleCode()).isEqualTo("MAN2302");
    assertThat(s.getModule()).isEqualTo("RESPONSABILITE SOCIETALE DES ENTREPRISES  ET DEVELOPPEMENT DURABLE");
    assertThat(s.getTeacher()).isEqualTo("Precious AHIABA");
    assertThat(s.getRoom()).isEqualTo("AKWABA");
    assertThat(s.getLevelsCodes()).isEqualTo("M2 M - AF, M2 M - LOG, M2 M - MK");
    assertThat(s.getDayOfWeek()).isEqualTo(1);
    assertThat(s.getTotalStudents()).isEqualTo(31);
    assertThat(s.getRoomCapacity()).isEqualTo(46);
    assertThat(s.getCapacityExceeded()).isFalse();
    assertThat(s.getStartTime()).isEqualTo(LocalTime.of(18, 30));
    assertThat(s.getRooms()).containsExactly("AKWABA");
    assertThat(s.getRoomIds()).containsExactly(7L);
    assertThat(s.getScheduleIds()).containsExactly(3611L);
    assertThat(s.getLevelIds()).containsExactly(44L, 46L, 45L);
    assertThat(s.getWeekStart()).isEqualTo(LocalDate.of(2026, 5, 18));
  }

  @Test
  void syncIsIdempotentAndDetectsUnchanged() {
    server.expect(requestTo(planningProperties.baseUrl() + "?start=2026-05-18&end=2026-05-24"))
        .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));
    server.expect(requestTo(planningProperties.baseUrl() + "?start=2026-05-18&end=2026-05-24"))
        .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

    SyncResult first = scheduleService.syncWeek(LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24));
    SyncResult second = scheduleService.syncWeek(LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24));

    assertThat(first.created()).isEqualTo(1);
    assertThat(second.created()).isZero();
    assertThat(second.unchanged()).isEqualTo(1);
    assertThat(second.updated()).isZero();
    assertThat(scheduleRepository.count()).isEqualTo(1);
  }

  @Test
  void syncDeletesSchedulesNoLongerPresent() {
    // Set up BOTH expectations up front: first call returns data, second returns an
    // empty list.
    server.expect(requestTo(planningProperties.baseUrl() + "?start=2026-05-18&end=2026-05-24"))
        .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));
    server.expect(requestTo(planningProperties.baseUrl() + "?start=2026-05-18&end=2026-05-24"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    scheduleService.syncWeek(LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24));
    SyncResult third = scheduleService.syncWeek(LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24));

    assertThat(third.deleted()).isEqualTo(1);
    assertThat(scheduleRepository.count()).isZero();
  }

  private static final String SAMPLE_JSON = """
      [{
        "id": "schedule_3611_2026-05-18",
        "title": "RESPONSABILITE SOCIETALE DES ENTREPRISES  ET DEVELOPPEMENT DURABLE",
        "start": "2026-05-18T18:30:00.000000Z",
        "end": "2026-05-18T21:30:00.000000Z",
        "backgroundColor": "#808000",
        "borderColor": "#808000",
        "textColor": "#000000",
        "extendedProps": {
          "type": "course",
          "sessionType": "course",
          "sessionLabel": null,
          "isExamSession": false,
          "isActivitySession": false,
          "examId": null,
          "planningActivityId": null,
          "scheduleId": 3611,
          "scheduleIds": [3611],
          "module": "RESPONSABILITE SOCIETALE DES ENTREPRISES  ET DEVELOPPEMENT DURABLE",
          "moduleCode": "MAN2302",
          "moduleId": 429,
          "teacher": "Precious AHIABA",
          "teacherId": 317,
          "room": "AKWABA",
          "rooms": ["AKWABA"],
          "roomIds": [7],
          "roomId": 7,
          "levels": "Master 2 M Audit Finance Soir, Master 2 M Logistique Soir, Master 2 M Marketing Soir",
          "levelsCodes": "M2 M - AF, M2 M - LOG, M2 M - MK",
          "levelIds": [44, 46, 45],
          "dayOfWeek": 1,
          "startTime": "18:30:00",
          "endTime": "21:30:00",
          "totalStudents": 31,
          "roomCapacity": 46,
          "capacityExceeded": false
        }
      }]
      """;
}
