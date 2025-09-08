package io.github.ryamal4.passengerflow.service;

import io.github.ryamal4.passengerflow.AbstractTestContainerTest;
import io.github.ryamal4.passengerflow.persistence.entities.BusEntity;
import io.github.ryamal4.passengerflow.persistence.entities.PassengerCountEntity;
import io.github.ryamal4.passengerflow.persistence.entities.RouteEntity;
import io.github.ryamal4.passengerflow.persistence.entities.StopEntity;
import io.github.ryamal4.passengerflow.persistence.repository.IPassengerRepository;
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
    private IPassengerRepository passengerRepository;

    @Autowired
    private PassengerCountCalculationService calculationService;

    private StopEntity testStop;
    private BusEntity testBus1;
    private BusEntity testBus2;

    @BeforeEach
    void setUp() {
        RouteEntity testRoute = new RouteEntity();
        testRoute.setName("7A");
        entityManager.persistAndFlush(testRoute);

        testStop = new StopEntity();
        testStop.setName("Kampi");
        testStop.setLat(60.1699);
        testStop.setLon(24.9342);
        testStop.setRoute(testRoute);
        entityManager.persistAndFlush(testStop);

        testBus1 = new BusEntity();
        testBus1.setModel("Bus Model 1");
        testBus1.setRoute(testRoute);
        entityManager.persistAndFlush(testBus1);

        testBus2 = new BusEntity();
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

        List<PassengerCountEntity> processedRecords = passengerRepository
                .findByTimestampBetweenAndCurrentPassengersIsNull(
                        testDate.atStartOfDay(),
                        testDate.plusDays(1).atStartOfDay()
                );

        assertThat(processedRecords).isEmpty();

        List<PassengerCountEntity> allRecords = passengerRepository.findAll();
        List<PassengerCountEntity> testDateRecords = allRecords.stream()
                .filter(pc -> pc.getTimestamp().toLocalDate().equals(testDate))
                .sorted(Comparator.comparing(PassengerCountEntity::getTimestamp))
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

        List<PassengerCountEntity> allRecords = passengerRepository.findAll();

        List<PassengerCountEntity> bus1Records = allRecords.stream()
                .filter(pc -> pc.getBus().getId().equals(testBus1.getId()))
                .filter(pc -> pc.getTimestamp().toLocalDate().equals(testDate))
                .sorted(Comparator.comparing(PassengerCountEntity::getTimestamp))
                .toList();

        List<PassengerCountEntity> bus2Records = allRecords.stream()
                .filter(pc -> pc.getBus().getId().equals(testBus2.getId()))
                .filter(pc -> pc.getTimestamp().toLocalDate().equals(testDate))
                .sorted(Comparator.comparing(PassengerCountEntity::getTimestamp))
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

        List<PassengerCountEntity> allRecords = passengerRepository.findAll();
        assertThat(allRecords).isEmpty();
    }

    @Test
    void testCalculateCurrentPassengersForDate_OnlyProcessesNullRecords() {
        LocalDate testDate = LocalDate.of(2025, 1, 15);

        PassengerCountEntity unprocessed = createPassengerCount(testBus1,
                testDate.atTime(8, 0), 10, 0);
        PassengerCountEntity alreadyProcessed = createPassengerCount(testBus1,
                testDate.atTime(9, 0), 5, 3);

        alreadyProcessed.setCurrentPassengers(15);
        entityManager.persistAndFlush(alreadyProcessed);

        calculationService.calculateCurrentPassengersForDate(testDate);
        entityManager.flush();
        entityManager.clear();

        PassengerCountEntity refreshedUnprocessed = entityManager.find(PassengerCountEntity.class,
                unprocessed.getId());
        PassengerCountEntity refreshedProcessed = entityManager.find(PassengerCountEntity.class,
                alreadyProcessed.getId());

        assertThat(refreshedUnprocessed.getCurrentPassengers()).isEqualTo(10);
        assertThat(refreshedProcessed.getCurrentPassengers()).isEqualTo(15);
    }

    @Test
    void testCalculateCurrentPassengersForDate_HandlesNegativePassengerCount() {
        LocalDate testDate = LocalDate.of(2025, 1, 15);

        createPassengerCount(testBus1, testDate.atTime(8, 0), 10, 0);
        createPassengerCount(testBus1, testDate.atTime(9, 0), 2, 15);

        calculationService.calculateCurrentPassengersForDate(testDate);
        entityManager.flush();
        entityManager.clear();

        List<PassengerCountEntity> allRecords = passengerRepository.findAll();
        List<PassengerCountEntity> testDateRecords = allRecords.stream()
                .filter(pc -> pc.getTimestamp().toLocalDate().equals(testDate))
                .sorted(Comparator.comparing(PassengerCountEntity::getTimestamp))
                .toList();

        assertThat(testDateRecords).hasSize(2);
        assertThat(testDateRecords.get(0).getCurrentPassengers()).isEqualTo(10);
        assertThat(testDateRecords.get(1).getCurrentPassengers()).isEqualTo(-3);
    }

    private PassengerCountEntity createPassengerCount(BusEntity bus, LocalDateTime timestamp, int entered, int exited) {
        PassengerCountEntity passengerCount = new PassengerCountEntity();

        passengerCount.setBus(bus);
        passengerCount.setStop(testStop);
        passengerCount.setEntered(entered);
        passengerCount.setExited(exited);
        passengerCount.setTimestamp(timestamp);
        passengerCount.setCurrentPassengers(null);

        return entityManager.persistAndFlush(passengerCount);
    }
}