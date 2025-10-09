package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.PassengerCountAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
                    b.bus_model_id,
                    SUM(pc.entered - pc.exited) OVER (
                        PARTITION BY pc.bus_id, DATE_TRUNC('day', pc.timestamp - INTERVAL '4 hours')
                        ORDER BY pc.timestamp
                        ROWS UNBOUNDED PRECEDING
                    ) as current_load
                FROM passenger_counts pc
                JOIN buses b ON pc.bus_id = b.id
                WHERE EXTRACT(DOW FROM pc.timestamp) = :dayOfWeek
            ),
            occupancy_percentages AS (
                SELECT
                    pl.stop_id,
                    pl.timestamp,
                    (pl.current_load::float / bm.capacity * 100.0) as occupancy_percentage
                FROM passenger_loads pl
                JOIN bus_models bm ON pl.bus_model_id = bm.id
            ),
            aggregated_data AS (
                SELECT
                    op.stop_id,
                    :dayOfWeek as day_of_week,
                    date_bin('5 minutes', op.timestamp, '2000-01-01 04:00:00') as time_slot,
                    AVG(op.occupancy_percentage) as average_occupancy_percentage
                FROM occupancy_percentages op
                GROUP BY op.stop_id, time_slot
            )
            INSERT INTO passenger_counts_aggregation (stop_id, day_of_week, hour, minute, average_occupancy_percentage)
            SELECT
                stop_id,
                day_of_week,
                EXTRACT(HOUR FROM time_slot)::integer as hour,
                EXTRACT(MINUTE FROM time_slot)::integer as minute,
                average_occupancy_percentage
            FROM aggregated_data
            """, nativeQuery = true)
    int insertAggregatedData(@Param("dayOfWeek") Integer dayOfWeek);
}