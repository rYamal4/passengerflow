package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface IPassengerCountRepository extends JpaRepository<PassengerCount, Long> {

    @Query("SELECT pc FROM PassengerCount pc " +
           "WHERE (:busId IS NULL OR pc.bus.id = :busId) " +
           "AND (:stopId IS NULL OR pc.stop.id = :stopId) " +
           "AND (:startTime IS NULL OR pc.timestamp >= :startTime) " +
           "AND (:endTime IS NULL OR pc.timestamp <= :endTime) " +
           "ORDER BY pc.timestamp DESC")
    Page<PassengerCount> findByFilters(@Param("busId") Long busId,
                                      @Param("stopId") Long stopId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      Pageable pageable);
}