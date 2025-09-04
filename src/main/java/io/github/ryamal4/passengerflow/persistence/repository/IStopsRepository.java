package io.github.ryamal4.passengerflow.persistence.repository;

import io.github.ryamal4.passengerflow.persistence.entities.RouteEntity;
import io.github.ryamal4.passengerflow.persistence.entities.StopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IStopsRepository extends JpaRepository<RouteEntity, Long> {
    @Query(value = "SELECT s.*, " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(s.lat)) * cos(radians(s.lon)" +
            " - radians(:lon)) + sin(radians(:lat)) * sin(radians(s.lat)))) AS distance " +
            "FROM stop s " +
            "ORDER BY distance " +
            "LIMIT :count",
            nativeQuery = true)
    public List<StopEntity> getNearbyStops(Double lat, Double lon, int count);
}
