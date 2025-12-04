package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.AbstractTestContainerTest;
import io.github.ryamal4.passengerflow.model.*;
import io.github.ryamal4.passengerflow.specification.PassengerCountSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PassengerCountRepositoryTest extends AbstractTestContainerTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 9, 21, 10, 0, 0);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IPassengerCountRepository passengerCountRepository;

    private Bus bus1;
    private Bus bus2;
    private Stop stop1;
    private Stop stop2;

    @BeforeEach
    void setUp() {
        var route1 = new Route(null, "Route 1", new ArrayList<>(), new ArrayList<>());
        entityManager.persistAndFlush(route1);

        var route2 = new Route(null, "Route 2", new ArrayList<>(), new ArrayList<>());
        entityManager.persistAndFlush(route2);

        var busModel1 = BusModel.builder()
                .name("Volvo 7900")
                .capacity(50)
                .buses(new ArrayList<>())
                .build();
        entityManager.persistAndFlush(busModel1);

        var busModel2 = BusModel.builder()
                .name("Mercedes Citaro")
                .capacity(60)
                .buses(new ArrayList<>())
                .build();
        entityManager.persistAndFlush(busModel2);

        bus1 = new Bus(null, busModel1, route1, new ArrayList<>());
        entityManager.persistAndFlush(bus1);

        bus2 = new Bus(null, busModel2, route2, new ArrayList<>());
        entityManager.persistAndFlush(bus2);

        stop1 = new Stop(null, "Stop 1", 60.1699, 24.9342, route1, new ArrayList<>());
        entityManager.persistAndFlush(stop1);

        stop2 = new Stop(null, "Stop 2", 60.1700, 24.9350, route2, new ArrayList<>());
        entityManager.persistAndFlush(stop2);

        createPassengerCount(bus1, stop1, BASE_TIME, 10, 5);
        createPassengerCount(bus1, stop1, BASE_TIME.plusMinutes(10), 15, 8);
        createPassengerCount(bus1, stop2, BASE_TIME.plusMinutes(20), 20, 10);
        createPassengerCount(bus2, stop1, BASE_TIME.plusMinutes(30), 12, 6);
        createPassengerCount(bus2, stop2, BASE_TIME.plusMinutes(40), 18, 9);
        createPassengerCount(bus2, stop2, BASE_TIME.plusMinutes(50), 25, 15);
    }

    @Test
    void testFindAllWithBusIdFilterReturnsOnlyMatchingBus() {
        Specification<PassengerCount> spec = PassengerCountSpecification.hasBusId(bus1.getId());

        List<PassengerCount> result = passengerCountRepository.findAll(spec);

        assertThat(result)
                .hasSize(3)
                .allMatch(pc -> pc.getBus().getId().equals(bus1.getId()));
    }

    @Test
    void testFindAllWithStopIdFilterReturnsOnlyMatchingStop() {
        Specification<PassengerCount> spec = PassengerCountSpecification.hasStopId(stop1.getId());

        List<PassengerCount> result = passengerCountRepository.findAll(spec);

        assertThat(result)
                .hasSize(3)
                .allMatch(pc -> pc.getStop().getId().equals(stop1.getId()));
    }

    @Test
    void testFindAllWithStartTimeFilterReturnsRecordsAfterTime() {
        LocalDateTime startTime = BASE_TIME.plusMinutes(25);
        Specification<PassengerCount> spec = PassengerCountSpecification.hasTimestampAfter(startTime);

        List<PassengerCount> result = passengerCountRepository.findAll(spec);

        assertThat(result)
                .hasSize(3)
                .allMatch(pc -> pc.getTimestamp().isAfter(startTime)
                        || pc.getTimestamp().isEqual(startTime));
    }

    @Test
    void testFindAllWithEndTimeFilterReturnsRecordsBeforeTime() {
        LocalDateTime endTime = BASE_TIME.plusMinutes(25);
        Specification<PassengerCount> spec = PassengerCountSpecification.hasTimestampBefore(endTime);

        List<PassengerCount> result = passengerCountRepository.findAll(spec);

        assertThat(result)
                .hasSize(3)
                .allMatch(pc -> pc.getTimestamp().isBefore(endTime)
                        || pc.getTimestamp().isEqual(endTime));
    }

    @Test
    void testFindAllWithAllFiltersReturnsMatchingRecords() {
        LocalDateTime startTime = BASE_TIME.plusMinutes(5);
        LocalDateTime endTime = BASE_TIME.plusMinutes(45);
        Specification<PassengerCount> spec = PassengerCountSpecification.withFilters(
                bus2.getId(), stop2.getId(), startTime, endTime
        );

        List<PassengerCount> result = passengerCountRepository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBus().getId()).isEqualTo(bus2.getId());
        assertThat(result.get(0).getStop().getId()).isEqualTo(stop2.getId());
        assertThat(result.get(0).getTimestamp()).isAfterOrEqualTo(startTime);
        assertThat(result.get(0).getTimestamp()).isBeforeOrEqualTo(endTime);
    }

    @Test
    void testFindAllWithNullFiltersReturnsAllRecords() {
        Specification<PassengerCount> spec = PassengerCountSpecification.withFilters(null, null,
                null, null);

        List<PassengerCount> result = passengerCountRepository.findAll(spec);

        assertThat(result).hasSize(6);
    }

    @Test
    void testFindAllWithNoMatchesReturnsEmptyList() {
        LocalDateTime startTime = BASE_TIME.plusHours(10);
        Specification<PassengerCount> spec = PassengerCountSpecification.hasTimestampAfter(startTime);

        List<PassengerCount> result = passengerCountRepository.findAll(spec);

        assertThat(result).isEmpty();
    }

    @Test
    void testFindAllWithPartialFiltersBusAndTimeRangeReturnsCorrectRecords() {
        LocalDateTime startTime = BASE_TIME.plusMinutes(5);
        LocalDateTime endTime = BASE_TIME.plusMinutes(25);
        Specification<PassengerCount> spec = PassengerCountSpecification.withFilters(
                bus1.getId(), null, startTime, endTime
        );

        List<PassengerCount> result = passengerCountRepository.findAll(spec);

        assertThat(result)
                .hasSize(2)
                .allMatch(pc -> pc.getBus().getId().equals(bus1.getId()))
                .allMatch(pc ->
                        (pc.getTimestamp().isAfter(startTime) || pc.getTimestamp().isEqual(startTime)) &&
                                (pc.getTimestamp().isBefore(endTime) || pc.getTimestamp().isEqual(endTime))
                );
    }

    private void createPassengerCount(Bus bus, Stop stop, LocalDateTime timestamp, int entered, int exited) {
        var count = new PassengerCount(null, bus, stop, entered, exited, timestamp);
        entityManager.persistAndFlush(count);
    }
}
