package io.github.ryamal4.passengerflow.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "passenger_counts")
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
    private Integer entered;

    @Column(nullable = false)
    private Integer exited;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;
}
