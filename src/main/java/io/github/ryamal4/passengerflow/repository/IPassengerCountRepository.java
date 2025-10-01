package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IPassengerCountRepository extends JpaRepository<PassengerCount, Long>, JpaSpecificationExecutor<PassengerCount> {
}