package io.github.ryamal4.passengerflow.model;

import java.time.LocalDateTime;

public class PassengerCount {
    private Long id;
    private Bus bus;
    private Stop stop;
    private Integer entered;
    private Integer exited;
    private LocalDateTime timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }

    public Integer getEntered() {
        return entered;
    }

    public void setEntered(Integer entered) {
        this.entered = entered;
    }

    public Integer getExited() {
        return exited;
    }

    public void setExited(Integer exited) {
        this.exited = exited;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
