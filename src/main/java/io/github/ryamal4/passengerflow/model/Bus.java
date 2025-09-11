package io.github.ryamal4.passengerflow.model;

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
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @OneToMany(mappedBy = "bus")
    @Setter(AccessLevel.PRIVATE)
    private List<PassengerCount> passengerCounts = new ArrayList<>();

    public List<PassengerCount> getPassengerCounts() {
        return Collections.unmodifiableList(passengerCounts);
    }
}