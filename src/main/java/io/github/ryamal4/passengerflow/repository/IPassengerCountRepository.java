package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IPassengerCountRepository extends JpaRepository<PassengerCount, Long> {
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

    @Query(value = """
            SELECT AVG(pc.current_passengers)
            FROM passenger_counts pc
            JOIN stops s ON s.id = pc.stop_id
            JOIN routes r ON r.id = s.route_id
            WHERE r.id = :routeId
                AND s.name = :stopName
                AND EXTRACT(DOW FROM pc.timestamp) = EXTRACT(DOW FROM CAST(:dateTime AS timestamp))
                AND ABS(EXTRACT(EPOCH FROM (pc.timestamp::time - CAST(:dateTime AS timestamp)::time)) / 60) <= 15
            """, nativeQuery = true)
    Double findAvgLoadForSameWeekDays(
            @Param("routeId") Long routeId,
            @Param("stopName") String stopName,
            @Param("dateTime") LocalDateTime dateTime
    );
}