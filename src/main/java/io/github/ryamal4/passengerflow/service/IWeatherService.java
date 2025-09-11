package io.github.ryamal4.passengerflow.service;

import java.time.LocalDateTime;
import java.util.TimeZone;

public interface IWeatherService {
    boolean isRaining(LocalDateTime dateTime, Double latitude, Double longitude, TimeZone timeZone);
}
