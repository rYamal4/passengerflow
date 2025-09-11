package io.github.ryamal4.passengerflow.model;

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
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @ManyToMany(mappedBy = "permissions")
    @Setter(AccessLevel.PRIVATE)
    Set<Role> roles = new HashSet<>();

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
}
