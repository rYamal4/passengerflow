package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
