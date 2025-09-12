package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IStopsRepository extends JpaRepository<Stop, Long> {
    
    @NativeQuery("""
            SELECT s.*,
                   (6371 * acos(
                       cos(radians(:lat)) * cos(radians(s.lat)) *
                       cos(radians(s.lon) - radians(:lon)) +
                       sin(radians(:lat)) * sin(radians(s.lat))
                   )) AS distance
            FROM stops s
            ORDER BY distance
            LIMIT :count
            """)
    List<Stop> getNearbyStops(Double lat, Double lon, int count);
}
