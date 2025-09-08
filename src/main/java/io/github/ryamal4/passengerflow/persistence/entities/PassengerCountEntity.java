package io.github.ryamal4.passengerflow.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

@Entity
@Table(name = "passenger_counts", indexes = {
    @Index(name = "idx_bus_date_time", columnList = "bus_id, timestamp"),
    @Index(name = "idx_stop_timestamp", columnList = "stop_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class PassengerCountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private BusEntity bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private StopEntity stop;

    @Column(nullable = false)
    @Min(0)
    private Integer entered;

    @Column(nullable = false)
    @Min(0)
    private Integer exited;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "current_passengers")
    private Integer currentPassengers;
}
