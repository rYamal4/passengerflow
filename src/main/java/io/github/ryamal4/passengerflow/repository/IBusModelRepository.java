package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.BusModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IBusModelRepository extends JpaRepository<BusModel, Long> {
}
