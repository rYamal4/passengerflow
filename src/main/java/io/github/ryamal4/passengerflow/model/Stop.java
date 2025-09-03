package io.github.ryamal4.passengerflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Stop {
    private Long id;
    private String name;
    private Double lat;
    private Double lon;
}