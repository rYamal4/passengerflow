package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.AbstractTestContainerTest;
import io.github.ryamal4.passengerflow.model.Bus;
import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PassengerCountRepositoryTest extends AbstractTestContainerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IPassengerCountRepository passengerCountRepository;

    private Bus testBus;
    private Stop testStop;
    private Route testRoute;

    @BeforeEach
    void setUp() {
        testRoute = new Route();
        testRoute.setName("TestRoute");
        entityManager.persistAndFlush(testRoute);

        testStop = new Stop();
        testStop.setName("TestStop");
        testStop.setLat(60.1699);
        testStop.setLon(24.9342);
        testStop.setRoute(testRoute);
        entityManager.persistAndFlush(testStop);

        testBus = new Bus();
        testBus.setModel("TestBus");
        testBus.setRoute(testRoute);
        entityManager.persistAndFlush(testBus);
    }

    @Test
    void testFindByTimestampBetween_ReturnsOrderedResults() {
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 15, 10, 0);

        PassengerCount count1 = createPassengerCount(testBus, testStop, baseTime, 10);
        PassengerCount count2 = createPassengerCount(testBus, testStop, baseTime.plusMinutes(30), 15);
        PassengerCount count3 = createPassengerCount(testBus, testStop, baseTime.plusMinutes(60), 20);
        PassengerCount count4 = createPassengerCount(testBus, testStop, baseTime.plusMinutes(90), 25);

        entityManager.persistAndFlush(count1);
        entityManager.persistAndFlush(count2);
        entityManager.persistAndFlush(count3);
        entityManager.persistAndFlush(count4);

        List<PassengerCount> result = passengerCountRepository.findByTimestampBetween(
                baseTime.plusMinutes(15),
                baseTime.plusMinutes(75)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCurrentPassengers()).isEqualTo(15);
        assertThat(result.get(1).getCurrentPassengers()).isEqualTo(20);
    }

    @Test
    void testFindByTimestampBetween_EmptyResult() {
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 15, 10, 0);

        PassengerCount count1 = createPassengerCount(testBus, testStop, baseTime, 10);
        entityManager.persistAndFlush(count1);

        List<PassengerCount> result = passengerCountRepository.findByTimestampBetween(
                baseTime.plusHours(1),
                baseTime.plusHours(2)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void testFindByTimestampBetween_InclusiveBounds() {
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 15, 10, 0);

        PassengerCount count1 = createPassengerCount(testBus, testStop, baseTime, 10);
        PassengerCount count2 = createPassengerCount(testBus, testStop, baseTime.plusHours(1), 15);

        entityManager.persistAndFlush(count1);
        entityManager.persistAndFlush(count2);

        List<PassengerCount> result = passengerCountRepository.findByTimestampBetween(
                baseTime,
                baseTime.plusHours(1)
        );

        assertThat(result).hasSize(2);
    }

    @Test
    void testFindAvgLoadForSameWeekDays_CalculatesCorrectAverage() {
        LocalDateTime mondayTime = LocalDateTime.of(2024, 1, 15, 14, 30);
        LocalDateTime anotherMondayTime = LocalDateTime.of(2024, 1, 22, 14, 25);
        LocalDateTime tuesdayTime = LocalDateTime.of(2024, 1, 16, 14, 30);

        PassengerCount mondayCount1 = createPassengerCount(testBus, testStop, mondayTime, 20);
        PassengerCount mondayCount2 = createPassengerCount(testBus, testStop, anotherMondayTime, 30);
        PassengerCount tuesdayCount = createPassengerCount(testBus, testStop, tuesdayTime, 40);

        entityManager.persistAndFlush(mondayCount1);
        entityManager.persistAndFlush(mondayCount2);
        entityManager.persistAndFlush(tuesdayCount);

        Double result = passengerCountRepository.findAvgLoadForSameWeekDays(
                testRoute.getId(),
                testStop.getName(),
                mondayTime
        );

        assertThat(result).isEqualTo(25.0);
    }

    @Test
    void testFindAvgLoadForSameWeekDays_NoMatchingData() {
        LocalDateTime mondayTime = LocalDateTime.of(2024, 1, 15, 14, 30);

        Double result = passengerCountRepository.findAvgLoadForSameWeekDays(
                testRoute.getId(),
                testStop.getName(),
                mondayTime
        );

        assertThat(result).isNull();
    }

    @Test
    void testFindAvgLoadForSameWeekDays_TimeRangeFilter() {
        LocalDateTime targetTime = LocalDateTime.of(2024, 1, 15, 14, 30);
        LocalDateTime withinRange = LocalDateTime.of(2024, 1, 22, 14, 25);
        LocalDateTime outsideRange = LocalDateTime.of(2024, 1, 29, 15, 0);

        PassengerCount withinRangeCount = createPassengerCount(testBus, testStop, withinRange, 20);
        PassengerCount outsideRangeCount = createPassengerCount(testBus, testStop, outsideRange, 40);

        entityManager.persistAndFlush(withinRangeCount);
        entityManager.persistAndFlush(outsideRangeCount);

        Double result = passengerCountRepository.findAvgLoadForSameWeekDays(
                testRoute.getId(),
                testStop.getName(),
                targetTime
        );

        assertThat(result).isEqualTo(20.0);
    }

    @Test
    void testFindAvgLoadForSameWeekDays_DifferentStopsFiltered() {
        Stop otherStop = new Stop();
        otherStop.setName("OtherStop");
        otherStop.setLat(61.0);
        otherStop.setLon(25.0);
        otherStop.setRoute(testRoute);
        entityManager.persistAndFlush(otherStop);

        LocalDateTime mondayTime = LocalDateTime.of(2024, 1, 15, 14, 30);

        PassengerCount targetStopCount = createPassengerCount(testBus, testStop, mondayTime, 20);
        PassengerCount otherStopCount = createPassengerCount(testBus, otherStop, mondayTime, 40);

        entityManager.persistAndFlush(targetStopCount);
        entityManager.persistAndFlush(otherStopCount);

        Double result = passengerCountRepository.findAvgLoadForSameWeekDays(
                testRoute.getId(),
                testStop.getName(),
                mondayTime
        );

        assertThat(result).isEqualTo(20.0);
    }

    private PassengerCount createPassengerCount(Bus bus, Stop stop, LocalDateTime timestamp, Integer currentPassengers) {
        PassengerCount passengerCount = new PassengerCount();
        passengerCount.setBus(bus);
        passengerCount.setStop(stop);
        passengerCount.setTimestamp(timestamp);
        passengerCount.setEntered(5);
        passengerCount.setExited(3);
        passengerCount.setCurrentPassengers(currentPassengers);
        return passengerCount;
    }
}