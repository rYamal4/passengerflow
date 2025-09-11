package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IPassengerRepository extends JpaRepository<PassengerCount, Long> {
    String COUNTS_BETWEEN_DATETIME = """
            SELECT pc FROM PassengerCount pc
            WHERE pc.timestamp BETWEEN :startDateTime AND :endDateTime
            ORDER BY pc.bus.id, pc.timestamp
            """;

    @Query(COUNTS_BETWEEN_DATETIME)
    List<PassengerCount> findByTimestampBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}