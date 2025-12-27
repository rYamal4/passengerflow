package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.Bus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBusRepository extends JpaRepository<Bus, Long> {

    @Override
    @EntityGraph(attributePaths = {"busModel", "route"})
    List<Bus> findAll();
}