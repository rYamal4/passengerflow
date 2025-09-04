package io.github.ryamal4.passengerflow.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToMany()
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Setter(AccessLevel.PRIVATE)
    private Set<PermissionEntity> permissions = new HashSet<>();

    @ManyToMany(mappedBy = "roles")
    @Setter(AccessLevel.PRIVATE)
    private Set<UserEntity> users = new HashSet<>();

    public void addPermission(PermissionEntity permission) {
        permissions.add(permission);
        permission.getRoles().add(this);
    }

    public void removePermission(PermissionEntity permission) {
        permissions.remove(permission);
        permission.getRoles().remove(this);
    }
}
