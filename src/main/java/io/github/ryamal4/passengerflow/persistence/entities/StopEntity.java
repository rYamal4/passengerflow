package io.github.ryamal4.passengerflow.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "stops", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "route_id"})
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class StopEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double lat;

    @Column(nullable = false)
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double lon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity route;

    @OneToMany(mappedBy = "stop")
    @Setter(AccessLevel.PRIVATE)
    private List<PassengerCountEntity> passengerCounts = new ArrayList<>();

    public List<PassengerCountEntity> getPassengerCounts() {
        return Collections.unmodifiableList(passengerCounts);
    }
}
