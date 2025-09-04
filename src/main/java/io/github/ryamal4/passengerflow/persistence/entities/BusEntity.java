package io.github.ryamal4.passengerflow.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "buses")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class BusEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity route;

    @OneToMany(mappedBy = "bus")
    @Setter(AccessLevel.PRIVATE)
    private List<PassengerCountEntity> passengerCounts = new ArrayList<>();

    public List<PassengerCountEntity> getPassengerCounts() {
        return Collections.unmodifiableList(passengerCounts);
    }
}

// TODO make collection fields package-private, add helper methods