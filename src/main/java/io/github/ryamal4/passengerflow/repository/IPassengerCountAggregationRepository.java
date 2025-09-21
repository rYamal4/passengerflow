package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.PassengerCountAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IPassengerCountAggregationRepository extends JpaRepository<PassengerCountAggregation, Long> {

    @Modifying
    @Query("DELETE FROM PassengerCountAggregation p WHERE p.dayOfWeek = :dayOfWeek")
    void deleteByDayOfWeek(@Param("dayOfWeek") Integer dayOfWeek);

    @Modifying
    @Query(value = """
            WITH passenger_loads AS (
                SELECT
                    pc.stop_id,
                    pc.timestamp,
                    SUM(pc.entered - pc.exited) OVER (
                        PARTITION BY pc.bus_id, DATE_TRUNC('day', pc.timestamp - INTERVAL '4 hours')
                        ORDER BY pc.timestamp
                        ROWS UNBOUNDED PRECEDING
                    ) as current_load
                FROM passenger_counts pc
                WHERE pc.timestamp >= (CURRENT_DATE - INTERVAL '1 day' + INTERVAL '4 hours')
                AND pc.timestamp < (CURRENT_DATE + INTERVAL '4 hours')
                AND EXTRACT(DOW FROM pc.timestamp) = :dayOfWeek
            ),
            aggregated_data AS (
                SELECT
                    pl.stop_id,
                    :dayOfWeek as day_of_week,
                    date_bin('5 minutes', pl.timestamp, '2000-01-01 04:00:00') as time_slot,
                    ROUND(AVG(pl.current_load)) as average_load
                FROM passenger_loads pl
                GROUP BY pl.stop_id, time_slot
            )
            INSERT INTO passenger_counts_aggregation (stop_id, day_of_week, hour, minute, average_load)
            SELECT
                stop_id,
                day_of_week,
                EXTRACT(HOUR FROM time_slot)::integer as hour,
                EXTRACT(MINUTE FROM time_slot)::integer as minute,
                average_load
            FROM aggregated_data
            """, nativeQuery = true)
    int insertAggregatedData(@Param("dayOfWeek") Integer dayOfWeek);

    @Modifying
    @Query(value = """
            WITH passenger_loads AS (
                SELECT
                    pc.stop_id,
                    pc.timestamp,
                    SUM(pc.entered - pc.exited) OVER (
                        PARTITION BY pc.bus_id, DATE_TRUNC('day', pc.timestamp - INTERVAL '4 hours')
                        ORDER BY pc.timestamp
                        ROWS UNBOUNDED PRECEDING
                    ) as current_load
                FROM passenger_counts pc
                WHERE pc.timestamp >= :startTime AND pc.timestamp < :endTime
                AND EXTRACT(DOW FROM pc.timestamp) = :dayOfWeek
            ),
            aggregated_data AS (
                SELECT
                    pl.stop_id,
                    :dayOfWeek as day_of_week,
                    date_bin('5 minutes', pl.timestamp, '2000-01-01 04:00:00') as time_slot,
                    ROUND(AVG(pl.current_load)) as average_load
                FROM passenger_loads pl
                GROUP BY pl.stop_id, time_slot
            )
            INSERT INTO passenger_counts_aggregation (stop_id, day_of_week, hour, minute, average_load)
            SELECT
                stop_id,
                day_of_week,
                EXTRACT(HOUR FROM time_slot)::integer as hour,
                EXTRACT(MINUTE FROM time_slot)::integer as minute,
                average_load
            FROM aggregated_data
            """, nativeQuery = true)
    int insertAggregatedDataForPeriod(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("dayOfWeek") Integer dayOfWeek);
}