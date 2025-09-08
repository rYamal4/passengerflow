package io.github.ryamal4.passengerflow.persistence.repository;

import io.github.ryamal4.passengerflow.persistence.entities.PassengerCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IPassengerRepository extends JpaRepository<PassengerCountEntity, Long> {

    @Query("""
            SELECT pc FROM PassengerCountEntity pc
            WHERE pc.timestamp BETWEEN :startDateTime AND :endDateTime
              AND pc.currentPassengers IS NULL
            ORDER BY pc.bus.id, pc.timestamp
            """)
    List<PassengerCountEntity> findByTimestampBetweenAndCurrentPassengersIsNull(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}