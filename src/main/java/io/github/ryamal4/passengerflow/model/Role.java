package io.github.ryamal4.passengerflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role {
    private Long id;
    private String title;
    private Set<Permission> permissions;
}
