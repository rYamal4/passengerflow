package io.github.ryamal4.passengerflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "bus_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class BusModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private Integer capacity;

    @OneToMany(mappedBy = "busModel")
    @Setter(AccessLevel.PRIVATE)
    private List<Bus> buses = new ArrayList<>();

    public List<Bus> getBuses() {
        return Collections.unmodifiableList(buses);
    }
}
