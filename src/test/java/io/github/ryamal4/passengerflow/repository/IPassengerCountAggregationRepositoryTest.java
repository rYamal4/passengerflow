package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.AbstractTestContainerTest;
import io.github.ryamal4.passengerflow.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IPassengerCountAggregationRepositoryTest extends AbstractTestContainerTest {

    // Test constants: 21 September 2025 = Sunday (DOW = 7)
    private static final LocalDateTime SUNDAY_SEPT_21 = LocalDateTime.of(2025, 9, 21, 10, 5, 0);
    private static final LocalDateTime SATURDAY_SEPT_20 = SUNDAY_SEPT_21.minusDays(1); // DOW = 6

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IPassengerCountAggregationRepository aggregationRepository;

    private Stop testStop;
    private Bus testBus;

    @BeforeEach
    void setUp() {
        Route testRoute = new Route();
        testRoute.setName("TestRoute");
        entityManager.persistAndFlush(testRoute);

        testStop = new Stop();
        testStop.setName("TestStop");
        testStop.setLat(60.1699);
        testStop.setLon(24.9342);
        testStop.setRoute(testRoute);
        entityManager.persistAndFlush(testStop);

        testBus = new Bus();
        testBus.setModel("Volvo 7900");
        testBus.setRoute(testRoute);
        entityManager.persistAndFlush(testBus);
    }

    @Test
    void deleteByDayOfWeek_RemovesCorrectRecords() {
        PassengerCountAggregation monday = createAggregation(1, 10, 15, 50);
        PassengerCountAggregation anotherMonday = createAggregation(1, 12, 25, 70);
        PassengerCountAggregation tuesday = createAggregation(2, 11, 20, 60);

        entityManager.persistAndFlush(monday);
        entityManager.persistAndFlush(anotherMonday);
        entityManager.persistAndFlush(tuesday);

        aggregationRepository.deleteByDayOfWeek(1);
        entityManager.flush();
        List<PassengerCountAggregation> remaining = aggregationRepository.findAll();

        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getDayOfWeek()).isEqualTo(2);
    }

    @Test
    void insertAggregatedData_WithBasicPassengerData() {
        // Use Saturday data (DOW = 6) for testing with insertAggregatedDataForPeriod
        LocalDateTime startTime = SATURDAY_SEPT_20.withHour(4);
        LocalDateTime endTime = SATURDAY_SEPT_20.plusDays(1).withHour(4);
        int targetDayOfWeek = 6; // Saturday

        insertPassengerCount(SATURDAY_SEPT_20, 10, 5);
        insertPassengerCount(SATURDAY_SEPT_20.plusMinutes(2), 8, 3);
        insertPassengerCount(SATURDAY_SEPT_20.plusMinutes(3), 12, 6);

        int insertedCount = aggregationRepository.insertAggregatedDataForPeriod(startTime, endTime, targetDayOfWeek);
        List<PassengerCountAggregation> aggregations = aggregationRepository.findAll();

        assertThat(insertedCount).isGreaterThan(0);
        assertThat(aggregations).isNotEmpty();
        assertThat(aggregations.get(0).getDayOfWeek()).isEqualTo(targetDayOfWeek);
    }

    @Test
    void insertAggregatedData_CalculatesCorrectLoadProgression() {
        // Use Saturday data for load progression test
        LocalDateTime startTime = SATURDAY_SEPT_20.withHour(4);
        LocalDateTime endTime = SATURDAY_SEPT_20.plusDays(1).withHour(4);
        int targetDayOfWeek = 6; // Saturday

        // Bus starts empty, passengers enter and exit
        insertPassengerCount(SATURDAY_SEPT_20, 15, 0);  // +15 = 15 total
        insertPassengerCount(SATURDAY_SEPT_20.plusMinutes(2), 5, 8);   // +5-8 = 12 total
        insertPassengerCount(SATURDAY_SEPT_20.plusMinutes(3), 10, 2);  // +10-2 = 20 total

        int insertedCount = aggregationRepository.insertAggregatedDataForPeriod(startTime, endTime, targetDayOfWeek);
        List<PassengerCountAggregation> aggregations = aggregationRepository.findAll();
        boolean foundCorrectSlot = aggregations.stream()
                .anyMatch(agg -> agg.getHour() == SATURDAY_SEPT_20.getHour() && agg.getMinute() == 5);

        assertThat(insertedCount).isGreaterThan(0);
        assertThat(aggregations).hasSizeGreaterThan(0);
        assertThat(foundCorrectSlot).isTrue();
    }

    @Test
    void insertAggregatedData_GroupsByTimeSlots() {
        // Use Saturday data with different time slots
        LocalDateTime startTime = SATURDAY_SEPT_20.withHour(4);
        LocalDateTime endTime = SATURDAY_SEPT_20.plusDays(1).withHour(4);
        int targetDayOfWeek = 6; // Saturday

        // Data in 10:05 slot (date_bin rounds to 10:05)
        insertPassengerCount(SATURDAY_SEPT_20, 10, 0);
        insertPassengerCount(SATURDAY_SEPT_20.plusMinutes(1), 5, 2);
        insertPassengerCount(SATURDAY_SEPT_20.plusMinutes(2), 3, 1);

        // Data in 10:10 slot
        insertPassengerCount(SATURDAY_SEPT_20.plusMinutes(5), 8, 4);
        insertPassengerCount(SATURDAY_SEPT_20.plusMinutes(7), 6, 3);

        aggregationRepository.insertAggregatedDataForPeriod(startTime, endTime, targetDayOfWeek);
        List<PassengerCountAggregation> aggregations = aggregationRepository.findAll();
        boolean has1005Slot = aggregations.stream()
                .anyMatch(agg -> agg.getHour() == SATURDAY_SEPT_20.getHour() && agg.getMinute() == 5);
        boolean has1010Slot = aggregations.stream()
                .anyMatch(agg -> agg.getHour() == SATURDAY_SEPT_20.getHour() && agg.getMinute() == 10);

        assertThat(has1005Slot).isTrue();
        assertThat(has1010Slot).isTrue();
    }

    @Test
    void insertAggregatedData_HandlesEmptyPassengerData() {
        int insertedCount = aggregationRepository.insertAggregatedData(1);
        List<PassengerCountAggregation> aggregations = aggregationRepository.findAll();

        assertThat(insertedCount).isZero();
        assertThat(aggregations).isEmpty();
    }

    @Test
    void insertAggregatedData_OnlyProcessesSpecificDayOfWeek() {
        // Create data for Saturday and Sunday (different days of week)
        LocalDateTime startTime = SATURDAY_SEPT_20.withHour(4);
        LocalDateTime endTime = SATURDAY_SEPT_20.plusDays(1).withHour(4);
        int saturdayDayOfWeek = 6;

        insertPassengerCount(SATURDAY_SEPT_20, 10, 0);     // Saturday data
        insertPassengerCount(SUNDAY_SEPT_21, 20, 5);       // Sunday data (should be ignored)

        // Process only Saturday's day of week
        int insertedCount = aggregationRepository.insertAggregatedDataForPeriod(startTime, endTime, saturdayDayOfWeek);
        List<PassengerCountAggregation> aggregations = aggregationRepository.findAll();
        boolean hasOnlySaturdayData = aggregations.stream()
                .allMatch(agg -> agg.getDayOfWeek() == saturdayDayOfWeek);

        assertThat(insertedCount).isGreaterThan(0);
        assertThat(hasOnlySaturdayData).isTrue();
    }

    private PassengerCountAggregation createAggregation(int dayOfWeek, int hour, int minute, int avgLoad) {
        PassengerCountAggregation aggregation = new PassengerCountAggregation();

        aggregation.setStop(testStop);
        aggregation.setDayOfWeek(dayOfWeek);
        aggregation.setHour(hour);
        aggregation.setMinute(minute);
        aggregation.setAverageLoad(avgLoad);

        return aggregation;
    }

    private void insertPassengerCount(LocalDateTime timestamp, int entered, int exited) {
        PassengerCount passengerCount = new PassengerCount();

        passengerCount.setBus(testBus);
        passengerCount.setStop(testStop);
        passengerCount.setTimestamp(timestamp);
        passengerCount.setEntered(entered);
        passengerCount.setExited(exited);

        entityManager.persistAndFlush(passengerCount);
    }
}