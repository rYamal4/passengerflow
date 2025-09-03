package io.github.ryamal4.passengerflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Route {
    private Long id;
    private List<Stop> stops;
    private List<Bus> buses;
}