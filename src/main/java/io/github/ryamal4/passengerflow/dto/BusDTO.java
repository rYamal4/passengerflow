package io.github.ryamal4.passengerflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusDTO {
    private Long id;
    private Long busModelId;
    private String busModelName;
    private Integer busModelCapacity;
    private Long routeId;
    private String routeName;
}