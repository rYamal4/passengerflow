package io.github.ryamal4.passengerflow.service.weather;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherResponseDto {
    private HourlyDataDto hourly;
}
