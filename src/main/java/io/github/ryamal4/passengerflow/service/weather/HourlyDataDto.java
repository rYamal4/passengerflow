package io.github.ryamal4.passengerflow.service.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HourlyDataDto {
    @JsonProperty("weather_code")
    private List<Integer> weatherCode;
}
