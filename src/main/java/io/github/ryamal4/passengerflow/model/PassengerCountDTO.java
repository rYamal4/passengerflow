package io.github.ryamal4.passengerflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PassengerCountDTO {
    private Long id;
    private BusDTO bus;
    private StopDTO stop;
    private Integer entered;
    private Integer exited;
    private LocalDateTime timestamp;
}
