package io.github.ryamal4.passengerflow.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean enabled;

    @ManyToMany()
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Setter(AccessLevel.PRIVATE)
    private Set<RoleEntity> roles = new HashSet<>();

    public void addRole(RoleEntity role) {
        roles.add(role);
        role.users.add(this);
    }

    public void removeRole(RoleEntity role) {
        roles.remove(role);
        role.users.remove(this);
    }
}
