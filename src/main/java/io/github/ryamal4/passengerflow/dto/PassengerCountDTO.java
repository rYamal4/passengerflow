package io.github.ryamal4.passengerflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerCountDTO {
    private Long id;

    @NotNull
    private Long busId;

    @NotNull
    private Long stopId;

    @NotNull
    @Min(0)
    private Integer entered;

    @NotNull
    @Min(0)
    private Integer exited;

    @NotNull
    private LocalDateTime timestamp;

    private String busModel;

    private String stopName;

    private String routeName;
}