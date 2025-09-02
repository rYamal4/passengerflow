package io.github.ryamal4.passengerflow.model;

import java.time.LocalDateTime;

public class RoutePredictionDTO {
    private Route route;
    private LocalDateTime departureTime;
    private Integer predictedLoad;

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public Integer getPredictedLoad() {
        return predictedLoad;
    }

    public void setPredictedLoad(Integer predictedLoad) {
        this.predictedLoad = predictedLoad;
    }
}