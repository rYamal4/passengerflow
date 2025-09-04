package io.github.ryamal4.passengerflow.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class PermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToMany(mappedBy = "permissions")
    @Setter(AccessLevel.PRIVATE)
    Set<RoleEntity> roles = new HashSet<>();

    public Set<RoleEntity> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
}
