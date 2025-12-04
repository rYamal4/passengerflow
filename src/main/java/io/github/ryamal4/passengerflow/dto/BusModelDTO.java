package io.github.ryamal4.passengerflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusModelDTO {
    private Long id;
    private String name;
    private Integer capacity;
    private String fileName;
}
