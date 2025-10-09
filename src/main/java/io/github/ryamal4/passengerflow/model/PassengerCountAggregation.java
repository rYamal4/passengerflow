package io.github.ryamal4.passengerflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name = "passenger_counts_aggregation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class PassengerCountAggregation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    @Column(name = "day_of_week", nullable = false)
    @Min(1)
    @Max(7)
    private Integer dayOfWeek;

    @Column(nullable = false)
    @Min(0)
    @Max(23)
    private Integer hour;

    @Column(nullable = false)
    @Min(0)
    @Max(55)
    private Integer minute;

    @Column(name = "average_load", nullable = false)
    private Integer averageLoad;
}
