package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IBusRepository extends JpaRepository<Bus, Long> {
}