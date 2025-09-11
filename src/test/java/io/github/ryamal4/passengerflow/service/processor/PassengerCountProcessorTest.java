package io.github.ryamal4.passengerflow.service.processor;

import io.github.ryamal4.passengerflow.AbstractTestContainerTest;
import io.github.ryamal4.passengerflow.model.Bus;
import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IPassengerCountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PassengerCountCalculationService.class)
class PassengerCountProcessorTest extends AbstractTestContainerTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IPassengerCountRepository passengerRepository;

    @Autowired
    private PassengerCountCalculationService calculationService;

    private Stop testStop;
    private Bus testBus1;
    private Bus testBus2;

    @BeforeEach
    void setUp() {
        Route testRoute = new Route();
        testRoute.setName("7A");
        entityManager.persistAndFlush(testRoute);

        testStop = new Stop();
        testStop.setName("Kampi");
        testStop.setLat(60.1699);
        testStop.setLon(24.9342);
        testStop.setRoute(testRoute);
        entityManager.persistAndFlush(testStop);

        testBus1 = new Bus();
        testBus1.setModel("Bus Model 1");
        testBus1.setRoute(testRoute);
        entityManager.persistAndFlush(testBus1);

        testBus2 = new Bus();
        testBus2.setModel("Bus Model 2");
        testBus2.setRoute(testRoute);
        entityManager.persistAndFlush(testBus2);
    }

    @Test
    void testCalculateCurrentPassengersForDate_SingleBusCorrectCalculation() {
        LocalDate testDate = LocalDate.of(2025, 1, 15);

        createPassengerCount(testBus1, testDate.atTime(8, 0), 10, 0);
        createPassengerCount(testBus1, testDate.atTime(9, 0), 5, 3);
        createPassengerCount(testBus1, testDate.atTime(10, 0), 2, 8);

        calculationService.calculateCurrentPassengersForDate(testDate);
        entityManager.flush();
        entityManager.clear();

        List<PassengerCount> allRecordsForDate = passengerRepository
                .findByTimestampBetween(
                        testDate.atStartOfDay(),
                        testDate.plusDays(1).atStartOfDay()
                );

        assertThat(allRecordsForDate).hasSize(3).allMatch(pc -> pc.getCurrentPassengers() != null);

        List<PassengerCount> allRecords = passengerRepository.findAll();
        List<PassengerCount> testDateRecords = allRecords.stream()
                .filter(pc -> pc.getTimestamp().toLocalDate().equals(testDate))
                .sorted(Comparator.comparing(PassengerCount::getTimestamp))
                .toList();

        assertThat(testDateRecords).hasSize(3);
        assertThat(testDateRecords.get(0).getCurrentPassengers()).isEqualTo(10);
        assertThat(testDateRecords.get(1).getCurrentPassengers()).isEqualTo(12);
        assertThat(testDateRecords.get(2).getCurrentPassengers()).isEqualTo(6);
    }

    @Test
    void testCalculateCurrentPassengersForDate_MultipleBusesSeparateCalculation() {
        LocalDate testDate = LocalDate.of(2025, 1, 15);

        createPassengerCount(testBus1, testDate.atTime(8, 0), 10, 2);
        createPassengerCount(testBus1, testDate.atTime(9, 0), 5, 1);

        createPassengerCount(testBus2, testDate.atTime(8, 30), 15, 3);
        createPassengerCount(testBus2, testDate.atTime(9, 30), 0, 7);

        calculationService.calculateCurrentPassengersForDate(testDate);
        entityManager.flush();
        entityManager.clear();

        List<PassengerCount> allRecords = passengerRepository.findAll();

        List<PassengerCount> bus1Records = allRecords.stream()
                .filter(pc -> pc.getBus().getId().equals(testBus1.getId()))
                .filter(pc -> pc.getTimestamp().toLocalDate().equals(testDate))
                .sorted(Comparator.comparing(PassengerCount::getTimestamp))
                .toList();

        List<PassengerCount> bus2Records = allRecords.stream()
                .filter(pc -> pc.getBus().getId().equals(testBus2.getId()))
                .filter(pc -> pc.getTimestamp().toLocalDate().equals(testDate))
                .sorted(Comparator.comparing(PassengerCount::getTimestamp))
                .toList();

        assertThat(bus1Records).hasSize(2);
        assertThat(bus1Records.get(0).getCurrentPassengers()).isEqualTo(8);
        assertThat(bus1Records.get(1).getCurrentPassengers()).isEqualTo(12);

        assertThat(bus2Records).hasSize(2);
        assertThat(bus2Records.get(0).getCurrentPassengers()).isEqualTo(12);
        assertThat(bus2Records.get(1).getCurrentPassengers()).isEqualTo(5);
    }

    @Test
    void testCalculateCurrentPassengersForDate_EmptyData() {
        LocalDate testDate = LocalDate.of(2025, 1, 15);

        calculationService.calculateCurrentPassengersForDate(testDate);

        List<PassengerCount> allRecords = passengerRepository.findAll();
        assertThat(allRecords).isEmpty();
    }

    @Test
    void testCalculateCurrentPassengersForDate_ReprocessesAllRecords() {
        LocalDate testDate = LocalDate.of(2025, 1, 15);

        PassengerCount record1 = createPassengerCount(testBus1, testDate.atTime(8, 0), 10, 0);
        PassengerCount record2 = createPassengerCount(testBus1, testDate.atTime(9, 0), 5, 3);

        record2.setCurrentPassengers(999);
        entityManager.persistAndFlush(record2);

        calculationService.calculateCurrentPassengersForDate(testDate);
        entityManager.flush();
        entityManager.clear();

        PassengerCount refreshedRecord1 = entityManager.find(PassengerCount.class, record1.getId());
        PassengerCount refreshedRecord2 = entityManager.find(PassengerCount.class, record2.getId());

        assertThat(refreshedRecord1.getCurrentPassengers()).isEqualTo(10);
        assertThat(refreshedRecord2.getCurrentPassengers()).isEqualTo(12);
    }

    @Test
    void testCalculateCurrentPassengersForDate_HandlesNegativePassengerCount() {
        LocalDate testDate = LocalDate.of(2025, 1, 15);

        createPassengerCount(testBus1, testDate.atTime(8, 0), 10, 0);
        createPassengerCount(testBus1, testDate.atTime(9, 0), 2, 15);

        calculationService.calculateCurrentPassengersForDate(testDate);
        entityManager.flush();
        entityManager.clear();

        List<PassengerCount> allRecords = passengerRepository.findAll();
        List<PassengerCount> testDateRecords = allRecords.stream()
                .filter(pc -> pc.getTimestamp().toLocalDate().equals(testDate))
                .sorted(Comparator.comparing(PassengerCount::getTimestamp))
                .toList();

        assertThat(testDateRecords).hasSize(2);
        assertThat(testDateRecords.get(0).getCurrentPassengers()).isEqualTo(10);
        assertThat(testDateRecords.get(1).getCurrentPassengers()).isEqualTo(-3);
    }

    private PassengerCount createPassengerCount(Bus bus, LocalDateTime timestamp, int entered, int exited) {
        PassengerCount passengerCount = new PassengerCount();

        passengerCount.setBus(bus);
        passengerCount.setStop(testStop);
        passengerCount.setEntered(entered);
        passengerCount.setExited(exited);
        passengerCount.setTimestamp(timestamp);
        passengerCount.setCurrentPassengers(null);

        return entityManager.persistAndFlush(passengerCount);
    }
}