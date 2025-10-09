package io.github.ryamal4.passengerflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "passenger_counts", indexes = {
        @Index(name = "idx_bus_date_time", columnList = "bus_id, timestamp"),
        @Index(name = "idx_stop_timestamp", columnList = "stop_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class PassengerCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    @NotNull
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    @NotNull
    private Stop stop;

    @Column(nullable = false)
    @Min(0)
    @NotNull
    private Integer entered;

    @Column(nullable = false)
    @Min(0)
    @NotNull
    private Integer exited;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime timestamp;
}
